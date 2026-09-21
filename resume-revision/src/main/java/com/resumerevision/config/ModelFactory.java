package com.resumerevision.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public final class ModelFactory {

    private ModelFactory() {
    }

    public static ChatLanguageModel siliconFlowChatModel(SiliconFlowConfig config) {
        return OpenAiChatModel.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .modelName(config.modelName())
                .temperature(config.temperature())
                .maxTokens(config.maxTokens())
                .build();
    }
}
