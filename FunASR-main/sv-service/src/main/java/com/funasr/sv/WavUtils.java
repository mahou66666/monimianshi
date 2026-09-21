package com.funasr.sv;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class WavUtils {
  private WavUtils() {}

  public record WavData(float[] samples, int sampleRate) {}

  public static WavData readWav(byte[] wavBytes) {
    if (wavBytes.length < 44) {
      throw new IllegalArgumentException("Invalid WAV data");
    }
    String riff = new String(wavBytes, 0, 4, StandardCharsets.US_ASCII);
    String wave = new String(wavBytes, 8, 4, StandardCharsets.US_ASCII);
    if (!"RIFF".equals(riff) || !"WAVE".equals(wave)) {
      throw new IllegalArgumentException("Not a WAV file");
    }

    int offset = 12;
    int channels = 1;
    int sampleRate = 16000;
    int bitsPerSample = 16;
    int dataOffset = -1;
    int dataSize = -1;

    while (offset + 8 <= wavBytes.length) {
      String chunkId = new String(wavBytes, offset, 4, StandardCharsets.US_ASCII);
      int chunkSize = readIntLE(wavBytes, offset + 4);
      offset += 8;
      if ("fmt ".equals(chunkId)) {
        int audioFormat = readShortLE(wavBytes, offset);
        channels = readShortLE(wavBytes, offset + 2);
        sampleRate = readIntLE(wavBytes, offset + 4);
        bitsPerSample = readShortLE(wavBytes, offset + 14);
        if (audioFormat != 1) {
          throw new IllegalArgumentException("Only PCM WAV supported");
        }
      } else if ("data".equals(chunkId)) {
        dataOffset = offset;
        dataSize = chunkSize;
        break;
      }
      offset += chunkSize;
      if (chunkSize % 2 == 1) {
        offset += 1;
      }
    }

    if (dataOffset < 0 || dataSize <= 0) {
      throw new IllegalArgumentException("No data chunk found");
    }
    if (bitsPerSample != 16) {
      throw new IllegalArgumentException("Only 16-bit WAV supported");
    }

    int bytesPerSample = bitsPerSample / 8;
    int totalSamples = dataSize / bytesPerSample / channels;
    float[] pcm = new float[totalSamples];

    ByteBuffer buffer = ByteBuffer.wrap(wavBytes, dataOffset, dataSize).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < totalSamples; i++) {
      double sum = 0.0;
      for (int ch = 0; ch < channels; ch++) {
        short sample = buffer.getShort();
        sum += sample / 32768.0;
      }
      pcm[i] = (float) (sum / channels);
    }

    return new WavData(pcm, sampleRate);
  }

  public static float[] resampleIfNeeded(float[] pcm, int srcRate, int dstRate) {
    if (srcRate == dstRate) {
      return pcm;
    }
    int outLength = (int) Math.round(pcm.length * (dstRate / (double) srcRate));
    float[] out = new float[outLength];
    for (int i = 0; i < outLength; i++) {
      double srcIndex = i * (srcRate / (double) dstRate);
      int index = (int) srcIndex;
      double frac = srcIndex - index;
      float s1 = pcm[Math.min(index, pcm.length - 1)];
      float s2 = pcm[Math.min(index + 1, pcm.length - 1)];
      out[i] = (float) (s1 + frac * (s2 - s1));
    }
    return out;
  }

  private static int readIntLE(byte[] data, int offset) {
    return (data[offset] & 0xff)
        | ((data[offset + 1] & 0xff) << 8)
        | ((data[offset + 2] & 0xff) << 16)
        | ((data[offset + 3] & 0xff) << 24);
  }

  private static short readShortLE(byte[] data, int offset) {
    return (short) ((data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8));
  }
}
