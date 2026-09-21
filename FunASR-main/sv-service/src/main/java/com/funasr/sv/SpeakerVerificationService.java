package com.funasr.sv;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SpeakerVerificationService {
  private static final String DEFAULT_SPEAKER_ID = "default";
  private static final String EMBEDDING_SUFFIX = ".emb";

  public record EnrollResult(String speakerId, int embeddingDim, int enrolledCount) {}

  public record VerifyResult(
      double score,
      double threshold,
      boolean blocked,
      String matchedSpeakerId,
      int enrolledCount) {}

  private static final Logger logger = LoggerFactory.getLogger(SpeakerVerificationService.class);

  private final SvProperties properties;
  private final SpeakerEmbeddingExtractor extractor;
  private final Map<String, float[]> enrolledEmbeddings = new LinkedHashMap<>();
  private final Object lock = new Object();

  public SpeakerVerificationService(SvProperties properties) {
    this.properties = properties;
    SpeakerEmbeddingExtractor created;
    try {
      created = new SpeakerEmbeddingExtractor(properties);
    } catch (RuntimeException ex) {
      // Speaker verification is optional. The ASR gateway (/api/voice/recognize)
      // must still start when the ONNX speaker model has not been deployed, so a
      // missing model degrades this feature instead of failing the whole context.
      logger.warn("Speaker verification disabled: {}", ex.getMessage());
      created = null;
    }
    this.extractor = created;
  }

  private SpeakerEmbeddingExtractor requireExtractor() {
    if (extractor == null) {
      throw new IllegalStateException("Speaker verification model is not available.");
    }
    return extractor;
  }

  @PostConstruct
  public void loadEmbedding() {
    synchronized (lock) {
      Path embeddingPath = properties.getEmbeddingPath();
      if (embeddingPath != null && embeddingPath.toFile().exists()) {
        try {
          float[] embedding = EmbeddingStore.load(embeddingPath);
          enrolledEmbeddings.put(DEFAULT_SPEAKER_ID, embedding);
          logger.info("Loaded blacklist embedding from {}", embeddingPath);
        } catch (Exception ex) {
          logger.warn("Failed to load embedding from {}", embeddingPath, ex);
        }
      }

      Path embeddingsDir = properties.getEmbeddingsDir();
      if (embeddingsDir != null && embeddingsDir.toFile().exists()) {
        try (var stream = Files.list(embeddingsDir)) {
          stream
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().endsWith(EMBEDDING_SUFFIX))
              .forEach(
                  path -> {
                    String name = path.getFileName().toString();
                    String speakerId = name.substring(0, name.length() - EMBEDDING_SUFFIX.length());
                    try {
                      float[] embedding = EmbeddingStore.load(path);
                      enrolledEmbeddings.put(speakerId, embedding);
                    } catch (Exception ex) {
                      logger.warn("Failed to load embedding {}", path, ex);
                    }
                  });
          logger.info("Loaded {} blacklist embedding(s) from {}", enrolledEmbeddings.size(), embeddingsDir);
        } catch (Exception ex) {
          logger.warn("Failed to load embeddings from {}", embeddingsDir, ex);
        }
      }
    }
  }

  public List<String> listSpeakerIds() {
    synchronized (lock) {
      return new ArrayList<>(enrolledEmbeddings.keySet());
    }
  }

  public EnrollResult enroll(byte[] wavBytes, String speakerId) throws Exception {
    SpeakerEmbeddingExtractor active = requireExtractor();
    WavUtils.WavData wav = WavUtils.readWav(wavBytes);
    float[] pcm = WavUtils.resampleIfNeeded(wav.samples(), wav.sampleRate(), properties.getSampleRate());
    float[] embedding = active.extractEmbedding(pcm, properties.getSampleRate());
    String normalizedSpeakerId = normalizeSpeakerId(speakerId);
    int dim = embedding.length;
    int enrolledCount;
    synchronized (lock) {
      float[] toStore = Arrays.copyOf(embedding, embedding.length);
      enrolledEmbeddings.put(normalizedSpeakerId, toStore);
      saveEmbedding(normalizedSpeakerId, toStore);
      enrolledCount = enrolledEmbeddings.size();
    }
    return new EnrollResult(normalizedSpeakerId, dim, enrolledCount);
  }

  public VerifyResult verify(byte[] wavBytes) throws Exception {
    Map<String, float[]> enrolledSnapshot;
    synchronized (lock) {
      enrolledSnapshot = new LinkedHashMap<>(enrolledEmbeddings);
    }
    if (enrolledSnapshot.isEmpty()) {
      throw new IllegalStateException("No blacklist embedding enrolled yet.");
    }
    SpeakerEmbeddingExtractor active = requireExtractor();
    WavUtils.WavData wav = WavUtils.readWav(wavBytes);
    float[] pcm = WavUtils.resampleIfNeeded(wav.samples(), wav.sampleRate(), properties.getSampleRate());
    float[] embedding = active.extractEmbedding(pcm, properties.getSampleRate());

    double bestScore = Double.NEGATIVE_INFINITY;
    String bestSpeakerId = null;
    for (Map.Entry<String, float[]> entry : enrolledSnapshot.entrySet()) {
      double score = cosine(entry.getValue(), embedding);
      if (score > bestScore) {
        bestScore = score;
        bestSpeakerId = entry.getKey();
      }
    }

    boolean blocked = bestScore >= properties.getThreshold();
    return new VerifyResult(
        bestScore, properties.getThreshold(), blocked, bestSpeakerId, enrolledSnapshot.size());
  }

  private void saveEmbedding(String speakerId, float[] embedding) throws Exception {
    Path embeddingsDir = properties.getEmbeddingsDir();
    if (embeddingsDir != null) {
      Path path = embeddingsDir.resolve(speakerId + EMBEDDING_SUFFIX);
      EmbeddingStore.save(path, embedding);
    }
    Path legacyPath = properties.getEmbeddingPath();
    if (legacyPath != null && DEFAULT_SPEAKER_ID.equals(speakerId)) {
      EmbeddingStore.save(legacyPath, embedding);
    }
  }

  private static String normalizeSpeakerId(String speakerId) {
    if (speakerId == null || speakerId.isBlank()) {
      return DEFAULT_SPEAKER_ID;
    }
    String sanitized = speakerId.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    return sanitized.isEmpty() ? DEFAULT_SPEAKER_ID : sanitized;
  }

  private static double cosine(float[] a, float[] b) {
    if (a.length != b.length) {
      throw new IllegalArgumentException("Embedding dimension mismatch.");
    }
    double dot = 0.0;
    double na = 0.0;
    double nb = 0.0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      na += a[i] * a[i];
      nb += b[i] * b[i];
    }
    if (na == 0 || nb == 0) {
      return 0.0;
    }
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
  }
}
