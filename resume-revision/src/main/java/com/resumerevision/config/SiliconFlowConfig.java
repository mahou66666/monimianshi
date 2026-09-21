package com.resumerevision.config;

public record SiliconFlowConfig(
        String baseUrl,
        String apiKey,
        String modelName,
        double temperature,
        int maxTokens
) {

    public static SiliconFlowConfig fromEnv() {
        String baseUrl = readOrDefault("SILICONFLOW_BASE_URL", "https://api.siliconflow.cn/v1");
        String apiKey = readRequired("SILICONFLOW_API_KEY");
        String modelName = readOrDefault("SILICONFLOW_MODEL", "Qwen/Qwen3.8-27B");
        double temperature = Double.parseDouble(readOrDefault("RESUME_LLM_TEMPERATURE", "0.3"));
        int maxTokens = Integer.parseInt(readOrDefault("RESUME_LLM_MAX_TOKENS", "2048"));

        return new SiliconFlowConfig(baseUrl, apiKey, modelName, temperature, maxTokens);
    }

    private static String readRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + key);
        }
        return value;
    }

    private static String readOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
