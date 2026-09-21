package com.funasr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AudioChunkerTest {
  @Test
  void calculateChunkBytesMatchesLegacyLogic() {
    int bytes = AudioChunker.calculateChunkBytes("5,10,5", 10, 16000, 2);
    assertEquals(1920, bytes);
  }
}
