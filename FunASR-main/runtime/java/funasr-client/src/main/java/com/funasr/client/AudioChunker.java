package com.funasr.client;

final class AudioChunker {
  private AudioChunker() {}

  static int calculateChunkBytes(String chunkSize, int chunkInterval, int sampleRate, int bytesPerSample) {
    String[] parts = chunkSize.split(",");
    if (parts.length < 2) {
      throw new IllegalArgumentException("chunkSize must have at least 2 numbers, e.g. 5,10,5");
    }
    int middle = Integer.parseInt(parts[1].trim());
    int intChunkSize = 60 * middle / chunkInterval;
    int framesPerChunk = sampleRate / 1000 * intChunkSize;
    return framesPerChunk * bytesPerSample;
  }

  static long calculateSleepMillis(int chunkBytes, int sampleRate, int bytesPerSample) {
    int bytesPerSecond = sampleRate * bytesPerSample;
    return Math.max(1, (long) chunkBytes * 1000 / bytesPerSecond);
  }
}
