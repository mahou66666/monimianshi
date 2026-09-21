package com.funasr.sv;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SvController {
  private final SpeakerVerificationService service;

  public SvController(SpeakerVerificationService service) {
    this.service = service;
  }

  @PostMapping(path = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> enroll(
      @RequestParam("audio") MultipartFile audio,
      @RequestParam(value = "speaker_id", required = false) String speakerId)
      throws Exception {
    SpeakerVerificationService.EnrollResult result = service.enroll(audio.getBytes(), speakerId);
    return Map.of(
        "enrolled", true,
        "speaker_id", result.speakerId(),
        "embedding_dim", result.embeddingDim(),
        "enrolled_count", result.enrolledCount());
  }

  @PostMapping(path = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> verify(@RequestParam("audio") MultipartFile audio) throws Exception {
    SpeakerVerificationService.VerifyResult result = service.verify(audio.getBytes());
    return Map.of(
        "score", result.score(),
        "threshold", result.threshold(),
        "blocked", result.blocked(),
        "matched_speaker_id", result.matchedSpeakerId(),
        "enrolled_count", result.enrolledCount());
  }

  @GetMapping(path = "/enrolled")
  public Map<String, Object> enrolled() {
    var speakerIds = service.listSpeakerIds();
    return Map.of("count", speakerIds.size(), "speaker_ids", speakerIds);
  }
}
