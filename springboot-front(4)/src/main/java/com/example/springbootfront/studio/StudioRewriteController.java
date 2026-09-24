package com.example.springbootfront.studio;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

@RestController @RequestMapping("/api/studio/resumes")
@ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioRewriteController {
    private final StudioRewrite rewrite;
    public StudioRewriteController(StudioRewrite rewrite){this.rewrite=rewrite;}
    @PostMapping("/{id}/rewrite") public Map<String,Object> generate(@PathVariable String id,@RequestBody StudioRewrite.Input input,HttpServletRequest request){
        return rewrite.generate((Long)request.getAttribute("studioUserId"),id,input);
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"请求失败":e.getReason()));}
}
