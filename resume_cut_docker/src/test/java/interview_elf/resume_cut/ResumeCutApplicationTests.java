package interview_elf.resume_cut;

import interview_elf.resume_cut.dto.ResumeResultDTO;
import interview_elf.resume_cut.service.ResumeParserService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ResumeCutApplicationTests {

    @Autowired
    private ResumeParserService resumeParserService;

    @Test
    void testResumeParsing() throws Exception {
        File file = findAnyPdfInTestResources();
        Assumptions.assumeTrue(file != null && file.exists(), "No test PDF under src/test/resources, skip this test.");

        try (InputStream inputStream = new FileInputStream(file)) {
            ResumeResultDTO result = resumeParserService.parse(inputStream, file.getName());

            assertNotNull(result, "Parse result should not be null");

            printObjectSection("BASIC_INFO", result.getBasicInfo());
            printObjectSection("SKILLS", result.getSkills());
            printObjectSection("AWARDS", result.getAwards());
            printObjectSection("SELF_EVALUATION", result.getSelfEvaluation());

            printListSection("EDUCATION", result.getEducation());
            printListSection("WORK_EXPERIENCE", result.getWorkExperience());
            printListSection("PROJECT_EXPERIENCE", result.getProjectExperience());
            printListSection("INTERNSHIP_EXPERIENCE", result.getInternshipExperience());

            if (result.getErrorMessage() != null && !result.getErrorMessage().isBlank()) {
                System.err.println("ERROR: " + result.getErrorMessage());
            }

            boolean hasContent = result.getBasicInfo() != null
                    || hasObjectContent(result.getSkills())
                    || (result.getWorkExperience() != null && !result.getWorkExperience().isEmpty())
                    || (result.getProjectExperience() != null && !result.getProjectExperience().isEmpty())
                    || (result.getInternshipExperience() != null && !result.getInternshipExperience().isEmpty())
                    || (result.getEducation() != null && !result.getEducation().isEmpty())
                    || hasObjectContent(result.getAwards())
                    || hasObjectContent(result.getSelfEvaluation())
                    || (result.getRawContent() != null && !result.getRawContent().isBlank());

            assertTrue(hasContent, "Parsed result contains no meaningful content");
        }
    }

    private File findAnyPdfInTestResources() throws IOException {
        Path root = Paths.get("src", "test", "resources");
        if (!Files.exists(root)) {
            return null;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);
        }
    }

    private boolean hasObjectContent(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return !s.isBlank();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return true;
    }

    private void printObjectSection(String title, Object value) {
        System.out.println("[" + title + "]");
        if (value == null) {
            System.out.println("(empty)");
            System.out.println();
            return;
        }

        String text;
        if (value instanceof List<?> list) {
            text = list.stream().filter(Objects::nonNull).map(String::valueOf).reduce((a, b) -> a + " | " + b).orElse("");
        } else {
            text = String.valueOf(value);
        }

        text = text.replace("\n", " ").trim();
        if (text.length() > 120) {
            text = text.substring(0, 120) + "...";
        }
        System.out.println(text.isEmpty() ? "(empty)" : text);
        System.out.println();
    }

    private void printListSection(String title, List<String> list) {
        System.out.println("[" + title + "]");
        if (list == null || list.isEmpty()) {
            System.out.println("(empty)");
            System.out.println();
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            String item = String.valueOf(list.get(i)).replace("\n", " ").trim();
            if (item.length() > 100) {
                item = item.substring(0, 100) + "...";
            }
            System.out.println((i + 1) + ". " + item);
        }
        System.out.println();
    }
}
