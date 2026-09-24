package com.example.springbootfront.studio;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController @RequestMapping("/api/studio")
@ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioController {
    private final StudioAuth auth;private final StudioResumes resumes;private final JdbcTemplate db;
    public StudioController(StudioAuth auth,StudioResumes resumes,JdbcTemplate db){this.auth=auth;this.resumes=resumes;this.db=db;}
    private long user(HttpServletRequest req){return (Long)req.getAttribute("studioUserId");}
    @GetMapping("/capabilities") public Map<String,Object> capabilities(){return Map.of("passwordLogin",true,"smsLogin",auth.smsAvailable(),"smsDemo",auth.smsAvailable(),"resumeUpload",true,"personalizedInterview",false,"wbInterview",true);}
    public record Sms(String phone,String code,String challengeId){}
    @PostMapping("/auth/sms/code") public Map<String,Object> smsCode(@RequestBody Sms p,HttpServletRequest req){return auth.sendSms(p.phone,req.getRemoteAddr());}
    @PostMapping("/auth/sms/login") public Map<String,Object> smsLogin(@RequestBody Sms p,HttpServletRequest req){return auth.loginSms(p.phone,p.code,p.challengeId,req.getRemoteAddr());}
    public record Login(String phone,String password){}
    @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody Login input,HttpServletRequest req){return auth.login(input.phone,input.password,req.getRemoteAddr());}
    @PostMapping("/auth/logout") public Map<String,Object> logout(HttpServletRequest req){auth.logout(req.getHeader("Authorization"));return Map.of("success",true);}
    private Map<String,Object> profile(long id){
        var rows=db.queryForList("SELECT nickname,role_id,career_level,graduation,avatar_id FROM studio_profile WHERE user_id=?",id);
        if(rows.isEmpty()){
            String name=db.queryForObject("SELECT user_name FROM user_info WHERE id=?",String.class,id);
            try{db.update("INSERT INTO studio_profile(user_id,nickname,role_id,career_level,graduation) VALUES(?,?,'java','校招','2027')",id,name==null?"同学":name);}
            catch(org.springframework.dao.DuplicateKeyException ignored){}
            rows=db.queryForList("SELECT nickname,role_id,career_level,graduation,avatar_id FROM studio_profile WHERE user_id=?",id);
        }
        var p=rows.get(0);Map<String,Object> out=new LinkedHashMap<>();out.put("userId",id);out.put("nickname",p.get("nickname"));out.put("roleId",p.get("role_id"));out.put("level",p.get("career_level"));out.put("graduation",p.get("graduation"));out.put("hasAvatar",p.get("avatar_id")!=null);return out;
    }
    @GetMapping("/me") public Map<String,Object> me(HttpServletRequest req){return profile(user(req));}
    @DeleteMapping("/me/avatar") public Map<String,Object> removeAvatar(HttpServletRequest req){
        long id=user(req);profile(id);db.update("UPDATE studio_profile SET avatar_id=NULL WHERE user_id=?",id);return profile(id);
    }
    public record Profile(String nickname,String roleId,String level,String graduation){}
    @PutMapping("/me") public Map<String,Object> update(@RequestBody Profile p,HttpServletRequest req){
        long id=user(req);profile(id);
        if(p.nickname==null||p.nickname.isBlank()||p.nickname.length()>20||!List.of("java","web","algorithm","testing").contains(Objects.toString(p.roleId,""))||!List.of("校招","初级","中级").contains(Objects.toString(p.level,""))||p.graduation==null||!p.graduation.matches("20[0-9]{2}"))throw new ResponseStatusException(BAD_REQUEST,"请检查昵称、岗位、阶段和毕业年份");
        db.update("UPDATE studio_profile SET nickname=?,role_id=?,career_level=?,graduation=? WHERE user_id=?",p.nickname.trim(),p.roleId,p.level,p.graduation,id);return profile(id);
    }
    @PostMapping("/me/avatar") public Map<String,Object> avatar(@RequestParam("file") MultipartFile file,HttpServletRequest req)throws IOException {long id=user(req);profile(id);String key=resumes.storeAvatar(file);db.update("UPDATE studio_profile SET avatar_id=? WHERE user_id=?",key,id);return profile(id);}
    @GetMapping(value="/me/avatar",produces="image/png") public byte[] avatar(HttpServletRequest req)throws IOException {profile(user(req));String key=db.queryForObject("SELECT avatar_id FROM studio_profile WHERE user_id=?",String.class,user(req));if(key==null)throw new ResponseStatusException(NOT_FOUND);return resumes.avatar(key);}
    @GetMapping("/resumes") public List<Map<String,Object>> list(HttpServletRequest req){return resumes.list(user(req));}
    @PostMapping("/resumes") @ResponseStatus(ACCEPTED) public Map<String,Object> upload(@RequestParam("file") MultipartFile file,HttpServletRequest req)throws IOException{return resumes.upload(user(req),file);}
    @GetMapping("/resumes/{id}") public Map<String,Object> detail(@PathVariable String id,HttpServletRequest req){return resumes.detail(user(req),id);}
    public record Review(int revision,List<Map<String,Object>> fragments){}
    @PutMapping("/resumes/{id}") public Map<String,Object> save(@PathVariable String id,@RequestBody Review input,HttpServletRequest req)throws IOException{return resumes.save(user(req),id,input.revision,input.fragments,false);}
    @PostMapping("/resumes/{id}/confirm") public Map<String,Object> confirm(@PathVariable String id,@RequestBody Review input,HttpServletRequest req)throws IOException{return resumes.save(user(req),id,input.revision,input.fragments,true);}
    @PostMapping("/resumes/{id}/retry") public Map<String,Object> retry(@PathVariable String id,HttpServletRequest req){return resumes.retry(user(req),id);}
    @PostMapping("/resumes/{id}/versions") public Map<String,Object> fork(@PathVariable String id,HttpServletRequest req)throws IOException{return resumes.fork(user(req),id);}
    @GetMapping("/resumes/{id}/file") public ResponseEntity<byte[]> download(@PathVariable String id,HttpServletRequest req)throws IOException{return ResponseEntity.ok().header("Content-Disposition","attachment; filename=resume.bin").contentType(MediaType.APPLICATION_OCTET_STREAM).body(resumes.download(user(req),id));}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,Object>> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"请求失败")));}
    @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> unexpected(Exception e){return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(Map.of("message","服务暂不可用，请稍后重试；已保存数据不会自动替换为示例"));}
}
