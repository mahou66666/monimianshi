package interview_elf.resume_cut;

import interview_elf.resume_cut.dto.ResumeResultDTO;
import interview_elf.resume_cut.service.impl.ResumeParserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResumeParserThinkingTest {
    @Test
    void parserExplicitlyDisablesThinkingInOutgoingRequest() throws Exception {
        var parser = new ResumeParserServiceImpl();
        ReflectionTestUtils.setField(parser, "llmApiKey", "test-only-key");
        ReflectionTestUtils.setField(parser, "llmApiUrl", "https://parser-test.invalid/chat/completions");
        ReflectionTestUtils.setField(parser, "llmModel", "Qwen/Qwen3.8-27B");
        var client = (RestTemplate) ReflectionTestUtils.getField(parser, "restTemplate");
        var server = MockRestServiceServer.bindTo(client).build();
        server.expect(requestTo("https://parser-test.invalid/chat/completions"))
                .andExpect(jsonPath("$.enable_thinking").value(false))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"BASIC_INFO\\\":{\\\"name\\\":\\\"Test\\\"}}\"}}]}", MediaType.APPLICATION_JSON));
        try (var input = getClass().getResourceAsStream("/sample_resume_zh_01.pdf")) {
            assertNotNull(input);
            ResumeResultDTO result = parser.parse(input, "sample_resume_zh_01.pdf");
            assertNotNull(result.getBasicInfo());
        }
        server.verify();
    }
}
