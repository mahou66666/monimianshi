package com.example.springbootfront.studio;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/studio")
@ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioAccountController {
    private final StudioAccount accounts;
    public StudioAccountController(StudioAccount accounts){this.accounts=accounts;}
    public record CodeRequest(String email,String currentPassword){}
    public record Register(String phone,String nickname,String password,String challengeId,String code){}
    public record Password(String currentPassword,String password,String challengeId,String code){}
    public record Bind(String currentPassword,String challengeId,String code){}
    private long user(HttpServletRequest req){return (Long)req.getAttribute("studioUserId");}
    @GetMapping("/auth/options") public Map<String,Object> options(){return accounts.capabilities();}
    @PostMapping("/auth/register/code") public Map<String,Object> registerCode(@RequestBody CodeRequest p,HttpServletRequest req){return accounts.sendCode("register",null,p.email,null,req.getRemoteAddr());}
    @PostMapping("/auth/reset/code") public Map<String,Object> resetCode(@RequestBody CodeRequest p,HttpServletRequest req){return accounts.sendCode("reset",null,p.email,null,req.getRemoteAddr());}
    @PostMapping("/auth/register") public Map<String,Object> register(@RequestBody Register p){accounts.register(p.phone,p.nickname,p.password,p.challengeId,p.code);return Map.of("success",true);}
    @PostMapping("/auth/reset") public Map<String,Object> reset(@RequestBody Password p){accounts.resetPassword(p.challengeId,p.code,p.password);return Map.of("success",true);}
    @GetMapping("/me/security") public Map<String,Object> security(HttpServletRequest req){return accounts.security(user(req));}
    @GetMapping("/me/login-history") public List<Map<String,Object>> history(HttpServletRequest req){return accounts.history(user(req));}
    @PostMapping("/me/email/code") public Map<String,Object> bindCode(@RequestBody CodeRequest p,HttpServletRequest req){return accounts.sendCode("bind",user(req),p.email,p.currentPassword,req.getRemoteAddr());}
    @PutMapping("/me/email") public Map<String,Object> bind(@RequestBody Bind p,HttpServletRequest req){accounts.bind(user(req),p.currentPassword,p.challengeId,p.code);return accounts.security(user(req));}
    @PutMapping("/me/password") public Map<String,Object> password(@RequestBody Password p,HttpServletRequest req){accounts.changePassword(user(req),p.currentPassword,p.password);return Map.of("success",true);}
}
