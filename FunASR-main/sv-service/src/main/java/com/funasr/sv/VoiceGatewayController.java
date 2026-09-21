package com.funasr.sv;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/voice")
public class VoiceGatewayController {
  private static final Logger logger = LoggerFactory.getLogger(VoiceGatewayController.class);

  private final SpeakerVerificationService svService;
  private final AsrWebSocketService asrService;

  public VoiceGatewayController(
      SpeakerVerificationService svService,
      AsrWebSocketService asrService) {
    this.svService = svService;
    this.asrService = asrService;
  }

  @PostMapping(path = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> enroll(
      @RequestParam("audio") MultipartFile audio,
      @RequestParam(value = "speaker_id", required = false) String speakerId)
      throws Exception {
    SpeakerVerificationService.EnrollResult result = svService.enroll(audio.getBytes(), speakerId);
    return Map.of(
        "enrolled", true,
        "speaker_id", result.speakerId(),
        "embedding_dim", result.embeddingDim(),
        "enrolled_count", result.enrolledCount());
  }

  @PostMapping(path = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> verify(@RequestParam("audio") MultipartFile audio) throws Exception {
    SpeakerVerificationService.VerifyResult result = svService.verify(audio.getBytes());
    return Map.of(
        "score", result.score(),
        "threshold", result.threshold(),
        "blocked", result.blocked(),
        "matched_speaker_id", result.matchedSpeakerId(),
        "enrolled_count", result.enrolledCount());
  }

  @PostMapping(path = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> recognize(
      @RequestParam("audio") MultipartFile audio,
      @RequestParam(value = "wav_name", required = false) String wavName)
      throws Exception {
    try {
      String text = asrService.recognize(audio.getBytes(), wavName);
      return ResponseEntity.ok(Map.of("text", text, "blocked", false));
    } catch (IllegalArgumentException ex) {
      logger.warn("recognize rejected: {}", ex.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage(), "blocked", false));
    } catch (Exception ex) {
      logger.error("recognize failed: {}", ex.getMessage(), ex);
      return ResponseEntity.ok(
          Map.of(
              "text", "",
              "blocked", false,
              "error", "ASR backend temporarily unavailable. Please retry."));
    }
  }

  @PostMapping(path = "/recognize_filter", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> recognizeWithFilter(
      @RequestParam("audio") MultipartFile audio,
      @RequestParam(value = "wav_name", required = false) String wavName)
      throws Exception {
    byte[] bytes = audio.getBytes();
    SpeakerVerificationService.VerifyResult svResult = tryVerify(bytes);
    if (svResult != null && svResult.blocked()) {
      return ResponseEntity.ok(
          Map.of(
              "blocked", true,
              "text", "",
              "score", svResult.score(),
              "threshold", svResult.threshold(),
              "matched_speaker_id", svResult.matchedSpeakerId(),
              "enrolled_count", svResult.enrolledCount()));
    }

    try {
      String text = asrService.recognize(bytes, wavName);
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("blocked", false);
      out.put("text", text);
      if (svResult != null) {
        out.put("score", svResult.score());
        out.put("threshold", svResult.threshold());
        out.put("matched_speaker_id", svResult.matchedSpeakerId());
        out.put("enrolled_count", svResult.enrolledCount());
      } else {
        out.put("score", null);
        out.put("threshold", null);
        out.put("matched_speaker_id", null);
        out.put("enrolled_count", 0);
      }
      return ResponseEntity.ok(out);
    } catch (IllegalArgumentException ex) {
      logger.warn("recognize_filter rejected: {}", ex.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage(), "blocked", false));
    } catch (Exception ex) {
      logger.error("recognize_filter failed: {}", ex.getMessage(), ex);
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("blocked", false);
      out.put("text", "");
      out.put("error", "ASR backend temporarily unavailable. Please retry.");
      if (svResult != null) {
        out.put("score", svResult.score());
        out.put("threshold", svResult.threshold());
        out.put("matched_speaker_id", svResult.matchedSpeakerId());
        out.put("enrolled_count", svResult.enrolledCount());
      } else {
        out.put("score", null);
        out.put("threshold", null);
        out.put("matched_speaker_id", null);
        out.put("enrolled_count", 0);
      }
      return ResponseEntity.ok(out);
    }
  }

  private SpeakerVerificationService.VerifyResult tryVerify(byte[] bytes) throws Exception {
    try {
      return svService.verify(bytes);
    } catch (IllegalStateException ex) {
      // No enrolled speaker yet. Keep ASR available for controller-level unification.
      logger.warn("verify skipped: {}", ex.getMessage());
      return null;
    }
  }
}
