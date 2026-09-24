package com.example.springbootfront.studio;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@ConditionalOnProperty(name="studio.enabled", havingValue="true")
public class StudioAuth {
    private final JdbcTemplate db;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();
    @Value("${studio.sms-demo:false}") private boolean smsDemo;
    private final Map<String,SmsChallenge> smsChallenges=new HashMap<>();
    private static class SmsChallenge {
        String phone;long created;int tries;boolean used;
        SmsChallenge(String phone,long now){this.phone=phone;this.created=now;}
    }
    public boolean smsAvailable(){return smsDemo;}
    private void checkSms(String address){
        if(!smsDemo)throw new ResponseStatusException(SERVICE_UNAVAILABLE,"手机演示验证码未开启");
        if(!List.of("127.0.0.1","::1","0:0:0:0:0:0:0:1").contains(address))throw new ResponseStatusException(FORBIDDEN,"手机演示验证码仅允许本机访问");
    }
    public synchronized Map<String,Object> sendSms(String phone,String address){
        checkSms(address);long now=System.currentTimeMillis();
        if(phone==null||!phone.matches("1[0-9]{10}"))throw new ResponseStatusException(BAD_REQUEST,"请输入有效手机号");
        attempts.entrySet().removeIf(e->e.getValue()[1]<now);
        long[] count=attempts.computeIfAbsent("sms:"+address,k->new long[]{0,now+60000});
        if(++count[0]>10)throw new ResponseStatusException(TOO_MANY_REQUESTS,"获取过于频繁，请稍后重试");
        smsChallenges.entrySet().removeIf(e->e.getValue().created+300000<now);
        if(smsChallenges.values().stream().anyMatch(c->c.phone.equals(phone)&&c.created+60000>now))throw new ResponseStatusException(TOO_MANY_REQUESTS,"请等待 60 秒后重新获取");
        String id=UUID.randomUUID().toString();smsChallenges.put(id,new SmsChallenge(phone,now));
        return Map.of("challengeId",id,"expiresIn",300,"message","手机演示验证码：123456。不发送短信，仅支持已注册账号。");
    }
    public synchronized Map<String,Object> loginSms(String phone,String code,String challengeId,String address){
        checkSms(address);var challenge=smsChallenges.get(challengeId);
        if(challenge==null||challenge.used||challenge.tries>=5||challenge.created+300000<System.currentTimeMillis())throw new ResponseStatusException(BAD_REQUEST,"验证码无效或已过期，请重新获取");
        challenge.tries++;
        if(!challenge.phone.equals(phone)||!"123456".equals(code))throw new ResponseStatusException(BAD_REQUEST,"手机号或验证码不正确");
        challenge.used=true;
        var users=db.queryForList("SELECT id FROM user_info WHERE phone=? AND status=1 AND is_deleted=0",phone);
        if(users.isEmpty())throw new ResponseStatusException(BAD_REQUEST,"账号不存在或不可用，请先注册");
        return session(((Number)users.getFirst().get("id")).longValue(),address);
    }
    public StudioAuth(JdbcTemplate db) { this.db = db; }
    public static String hash(byte[] value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public synchronized Map<String,Object> login(String phone, String password, String address) {
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> e.getValue()[1] < now);
        String key = address; // Limit guesses across accounts, not just one phone number.
        long[] count = attempts.computeIfAbsent(key, k -> new long[]{0, now+60000});
        if (++count[0] > 10) throw new ResponseStatusException(TOO_MANY_REQUESTS,"登录过于频繁，请稍后重试");
        if (phone == null || !phone.matches("[0-9]{11}") || password == null || password.length()>72)
            throw new ResponseStatusException(UNAUTHORIZED,"账号或密码不正确");
        var users = db.queryForList("SELECT id,password FROM user_info WHERE phone=? AND status=1 AND is_deleted=0",phone);
        // Always perform a password hash check, including when the account does not exist.
        String encoded = users.isEmpty() ? "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy" : String.valueOf(users.get(0).get("password"));
        boolean match = passwords.matches(password,encoded);
        if (users.isEmpty() || !match) throw new ResponseStatusException(UNAUTHORIZED,"账号或密码不正确");
        long userId = ((Number)users.get(0).get("id")).longValue();
        return session(userId,address);
    }
    private Map<String,Object> session(long userId,String address) {
        long now=System.currentTimeMillis();
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expires = now+8*60*60*1000;
        db.update("DELETE FROM studio_session WHERE expires_at<?",now);
        db.update("INSERT INTO studio_session(token_hash,user_id,expires_at) VALUES(?,?,?)",hash(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)),userId,expires);
        db.update("INSERT INTO studio_login_history(id,user_id,logged_at,address) VALUES(?,?,?,?)",UUID.randomUUID().toString(),userId,now,Objects.toString(address,"").substring(0,Math.min(64,Objects.toString(address,"").length())));
        return Map.of("token",token,"expiresAt",expires,"userId",userId);
    }
    public long authenticate(String header) {
        if (header==null || !header.matches("Bearer [A-Za-z0-9_-]{43}")) throw new ResponseStatusException(UNAUTHORIZED,"请先登录真实账户");
        var rows=db.queryForList("SELECT s.user_id FROM studio_session s JOIN user_info u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>? AND u.status=1 AND u.is_deleted=0",hash(header.substring(7).getBytes(java.nio.charset.StandardCharsets.UTF_8)),System.currentTimeMillis());
        if(rows.isEmpty()) throw new ResponseStatusException(UNAUTHORIZED,"登录已失效，请重新登录");
        return ((Number)rows.get(0).get("user_id")).longValue();
    }
    public void logout(String header) { db.update("DELETE FROM studio_session WHERE token_hash=?",hash(header.substring(7).getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
}
