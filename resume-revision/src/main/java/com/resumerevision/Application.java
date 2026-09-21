package com.resumerevision;

import com.resumerevision.config.SiliconFlowConfig;
import com.resumerevision.gateway.ResumeLlmGateway;
import com.resumerevision.gateway.SiliconFlowResumeLlmGateway;
import com.resumerevision.model.ResumeRevisionRequest;
import com.resumerevision.model.ResumeRevisionResult;
import com.resumerevision.service.PromptComposer;
import com.resumerevision.service.ResumeRevisionService;
import com.resumerevision.service.ResumeSectionSplitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: mvn exec:java -Dexec.args=\"<targetRole> <resumeFilePath> [constraints]\"");
            return;
        }

        String targetRole = args[0];
        Path resumePath = Path.of(args[1]);
        String constraints = args.length >= 3 ? args[2] : "one page; emphasize project impact and engineering outcomes";

        String originalResume = Files.readString(resumePath);

        SiliconFlowConfig config = SiliconFlowConfig.fromEnv();
        ResumeLlmGateway llmGateway = SiliconFlowResumeLlmGateway.fromConfig(config);

        ResumeRevisionService service = new ResumeRevisionService(
                llmGateway,
                new ResumeSectionSplitter(),
                new PromptComposer()
        );

        ResumeRevisionRequest request = new ResumeRevisionRequest(targetRole, originalResume, constraints);
        ResumeRevisionResult result = service.revise(request);

        System.out.println("\n===== Composed Prompt JSON =====\n");
        System.out.println(result.composedPromptJson());

        System.out.println("\n===== Revised Resume =====\n");
        System.out.println(result.revisedResume());

        System.out.println("\n===== Optimization Summary =====\n");
        System.out.println(result.optimizationSummary());

        System.out.println("\n===== Risk Warnings =====\n");
        System.out.println(result.riskWarnings());
    }
}

