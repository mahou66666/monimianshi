package com.funasr.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HotwordParserTest {
  @Test
  void parseHotwordsWithWeights() {
    Map<String, Integer> result = HotwordParser.parse("hello 30 nihao 40");
    assertEquals(2, result.size());
    assertEquals(30, result.get("hello"));
    assertEquals(40, result.get("nihao"));
  }

  @Test
  void parseMultiTokenPhrase() {
    Map<String, Integer> result = HotwordParser.parse("hello world 50");
    assertEquals(1, result.size());
    assertEquals(50, result.get("hello world"));
  }
}
