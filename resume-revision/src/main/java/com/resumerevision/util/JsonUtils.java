package com.resumerevision.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert object to JSON", e);
        }
    }

    public static Object parseJsonOrRaw(String maybeJson) {
        if (maybeJson == null || maybeJson.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(maybeJson, Object.class);
        } catch (JsonProcessingException e) {
            return Map.of("raw_text", maybeJson);
        }
    }
}
