package com.example.springbootfront.studio;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/studio/interviews")
@ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioInterviewController {
    private final StudioInterviews interviews;private final StudioAsr asr;
    public StudioInterviewController(StudioInterviews interviews,StudioAsr asr){this.interviews=interviews;this.asr=asr;}
    private long user(HttpServletRequest req){return (Long)req.getAttribute("studioUserId");}
    public record Start(String requestId,String resumeId,String industry,String jd,int maxQuestions,String roleId,String sourceSessionId,Integer sourceRound){}
    public record Answer(int expectedQuestion,String answer,Long durationMs,Map<String,Object> speechMetrics){}
    @PostMapping public Map<String,Object> start(@RequestBody Start input,HttpServletRequest req){return interviews.start(user(req),input.requestId,input.resumeId,input.industry,input.jd,input.maxQuestions,input.roleId,input.sourceSessionId,input.sourceRound);}
    @GetMapping public List<Map<String,Object>> list(HttpServletRequest req){return interviews.list(user(req));}
    @GetMapping("/growth") public Map<String,Object> growth(HttpServletRequest req){return interviews.growth(user(req));}
    @GetMapping("/{id}/report") public Map<String,Object> report(@PathVariable String id,HttpServletRequest req){return interviews.report(user(req),id);}
    @GetMapping("/{id}") public Map<String,Object> state(@PathVariable String id,HttpServletRequest req){return interviews.state(user(req),id);}
    @PostMapping("/{id}/answers") public Map<String,Object> answer(@PathVariable String id,@RequestBody Answer input,HttpServletRequest req){return interviews.answer(user(req),id,input.expectedQuestion,input.answer,input.durationMs,input.speechMetrics);}
    @PostMapping(value="/{id}/transcribe",consumes="multipart/form-data") public Map<String,Object> transcribe(@PathVariable String id,@RequestParam int expectedQuestion,@RequestParam("audio") MultipartFile audio,HttpServletRequest req){return asr.transcribe(user(req),id,expectedQuestion,audio);}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,Object>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"请求失败")));}
    @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> unexpected(Exception e){return ResponseEntity.internalServerError().body(Map.of("message","面试服务暂不可用，请刷新核对已保存进度"));}
}
