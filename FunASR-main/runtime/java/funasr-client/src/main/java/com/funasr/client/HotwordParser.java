package com.funasr.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

final class HotwordParser {
  private static final Pattern NUMBER = Pattern.compile("\\d+");

  private HotwordParser() {}

  static Map<String, Integer> parse(String input) {
    Map<String, Integer> result = new LinkedHashMap<>();
    if (input == null) {
      return result;
    }
    String trimmed = input.trim();
    if (trimmed.isEmpty()) {
      return result;
    }
    String[] tokens = trimmed.split("\\s+");
    StringBuilder current = new StringBuilder();
    for (String token : tokens) {
      if (NUMBER.matcher(token).matches()) {
        String phrase = current.toString().trim();
        if (!phrase.isEmpty()) {
          result.put(phrase, Integer.parseInt(token));
        }
        current.setLength(0);
      } else {
        if (current.length() > 0) {
          current.append(' ');
        }
        current.append(token);
      }
    }
    return result;
  }
}
