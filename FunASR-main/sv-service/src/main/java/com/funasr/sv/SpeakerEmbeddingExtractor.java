package com.funasr.sv;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.TensorInfo;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeakerEmbeddingExtractor {
  private static final Logger logger = LoggerFactory.getLogger(SpeakerEmbeddingExtractor.class);

  private final OrtEnvironment env;
  private final OrtSession session;
  private final FbankExtractor fbankExtractor;
  private final String inputName;
  private final long[] inputShape;

  public SpeakerEmbeddingExtractor(SvProperties properties) {
    try {
      Path modelPath = properties.getModelPath();
      if (modelPath == null || !modelPath.toFile().exists()) {
        throw new IllegalArgumentException("Model not found: " + modelPath);
      }
      env = OrtEnvironment.getEnvironment();
      SessionOptions options = new SessionOptions();
      session = env.createSession(modelPath.toString(), options);

      Map<String, NodeInfo> inputInfo = session.getInputInfo();
      if (inputInfo.isEmpty()) {
        throw new IllegalStateException("Model has no inputs.");
      }
      inputName = inputInfo.keySet().iterator().next();
      TensorInfo info = (TensorInfo) inputInfo.get(inputName).getInfo();
      inputShape = info.getShape();

      fbankExtractor = new FbankExtractor(properties);
      logger.info("Loaded SV model: {}", modelPath);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to initialize speaker model", ex);
    }
  }

  public float[] extractEmbedding(float[] pcm, int sampleRate) throws Exception {
    float[][] features = fbankExtractor.extract(pcm, sampleRate);
    if (features.length == 0) {
      throw new IllegalArgumentException("Audio too short for feature extraction");
    }

    TensorBundle input = prepareInput(features);
    try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input.data), input.shape);
         Result result = session.run(Collections.singletonMap(inputName, tensor))) {
      Object value = result.get(0).getValue();
      return normalize(flatten(value));
    }
  }

  private TensorBundle prepareInput(float[][] features) {
    int frames = features.length;
    int dims = features[0].length;
    int rank = inputShape.length;

    if (rank == 2) {
      float[] data = flatten2d(features, false);
      return new TensorBundle(data, new long[] {frames, dims});
    }

    if (rank == 3) {
      if (inputShape[2] == dims || inputShape[2] == -1) {
        float[] data = flatten2d(features, false);
        return new TensorBundle(data, new long[] {1, frames, dims});
      }
      if (inputShape[1] == dims || inputShape[1] == -1) {
        float[] data = flatten2d(features, true);
        return new TensorBundle(data, new long[] {1, dims, frames});
      }
      float[] data = flatten2d(features, false);
      return new TensorBundle(data, new long[] {1, frames, dims});
    }

    throw new IllegalArgumentException("Unsupported input rank: " + rank);
  }

  private static float[] flatten2d(float[][] input, boolean transpose) {
    int frames = input.length;
    int dims = input[0].length;
    float[] out = new float[frames * dims];
    if (!transpose) {
      int idx = 0;
      for (float[] frame : input) {
        for (float v : frame) {
          out[idx++] = v;
        }
      }
    } else {
      int idx = 0;
      for (int d = 0; d < dims; d++) {
        for (int f = 0; f < frames; f++) {
          out[idx++] = input[f][d];
        }
      }
    }
    return out;
  }

  private static float[] flatten(Object value) {
    if (value instanceof float[]) {
      return (float[]) value;
    }
    if (value instanceof float[][] array2d) {
      int total = array2d.length * array2d[0].length;
      float[] out = new float[total];
      int idx = 0;
      for (float[] row : array2d) {
        for (float v : row) {
          out[idx++] = v;
        }
      }
      return out;
    }
    if (value instanceof float[][][] array3d) {
      int total = array3d[0].length * array3d[0][0].length;
      float[] out = new float[total];
      int idx = 0;
      for (float[] row : array3d[0]) {
        for (float v : row) {
          out[idx++] = v;
        }
      }
      return out;
    }
    throw new IllegalArgumentException("Unsupported output type: " + value.getClass());
  }

  private static float[] normalize(float[] embedding) {
    double norm = 0.0;
    for (float v : embedding) {
      norm += v * v;
    }
    norm = Math.sqrt(norm);
    if (norm == 0) {
      return embedding;
    }
    float[] out = new float[embedding.length];
    for (int i = 0; i < embedding.length; i++) {
      out[i] = (float) (embedding[i] / norm);
    }
    return out;
  }

  private record TensorBundle(float[] data, long[] shape) {}
}
