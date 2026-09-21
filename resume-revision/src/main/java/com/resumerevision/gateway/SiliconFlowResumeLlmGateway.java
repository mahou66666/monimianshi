package com.resumerevision.gateway;

import com.resumerevision.config.ModelFactory;
import com.resumerevision.config.SiliconFlowConfig;
import com.resumerevision.service.ResumeEditorAgent;
import com.resumerevision.service.ResumeSuggestionAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;

import static java.util.Objects.requireNonNull;

public class SiliconFlowResumeLlmGateway implements ResumeLlmGateway {

    private final ResumeSuggestionAgent suggestionAgent;
    private final ResumeEditorAgent editorAgent;

    public SiliconFlowResumeLlmGateway(ChatLanguageModel chatModel) {
        ChatLanguageModel safeModel = requireNonNull(chatModel, "chatModel must not be null");
        this.suggestionAgent = AiServices.builder(ResumeSuggestionAgent.class)
                .chatLanguageModel(safeModel)
                .build();
        this.editorAgent = AiServices.builder(ResumeEditorAgent.class)
                .chatLanguageModel(safeModel)
                .build();
    }

    public static SiliconFlowResumeLlmGateway fromConfig(SiliconFlowConfig config) {
        return new SiliconFlowResumeLlmGateway(ModelFactory.siliconFlowChatModel(config));
    }

    @Override
    public String generateSuggestions(String targetRole, String constraints, String resumeSectionsJson) {
        return suggestionAgent.generateSuggestions(targetRole, constraints, resumeSectionsJson);
    }

    @Override
    public String reviseResume(String composedPromptJson) {
        return editorAgent.reviseResume(composedPromptJson);
    }
}

