package com.example.springbootfront.studio;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service @ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioAccount {
    private final JdbcTemplate db;private final StudioMail mail;private final TransactionTemplate tx;
    @org.springframework.beans.factory.annotation.Value("${studio.sms-demo:false}") private boolean smsDemo;
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder();
    public StudioAccount(JdbcTemplate db,StudioMail mail){this.db=db;this.mail=mail;tx=new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(db.getDataSource())));}
    private String email(String input){String value=Objects.toString(input,"").trim().toLowerCase(Locale.ROOT);if(value.length()>254||!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new ResponseStatusException(BAD_REQUEST,"请输入有效邮箱");return value;}
    private void password(String value){if(value==null||value.length()<8||value.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(BAD_REQUEST,"密码至少 8 位，且 UTF-8 编码不超过 72 字节");}
    private void verifyPassword(long id,String value){String encoded=db.queryForObject("SELECT password FROM user_info WHERE id=? AND status=1 AND is_deleted=0",String.class,id);if(value==null||value.getBytes(StandardCharsets.UTF_8).length>72||!passwords.matches(value,encoded))throw new ResponseStatusException(BAD_REQUEST,"当前密码不正确");}
    private String codeHash(String id,String code){return StudioAuth.hash((id+":"+code).getBytes(StandardCharsets.UTF_8));}
    public Map<String,Object> capabilities(){return Map.of("emailVerification",mail.available(),"registration",mail.available(),"emailPasswordReset",mail.available());}
    public synchronized Map<String,Object> sendCode(String purpose,Long userId,String input,String currentPassword,String address){
        if(!List.of("register","bind","reset").contains(purpose))throw new ResponseStatusException(BAD_REQUEST,"无效请求");
        if(!mail.available())throw new ResponseStatusException(SERVICE_UNAVAILABLE,"演示邮箱验证未开启");
        if(!List.of("127.0.0.1","0:0:0:0:0:0:0:1","::1").contains(address))throw new ResponseStatusException(FORBIDDEN,"演示验证仅允许本机访问");
        String target=email(input);long now=System.currentTimeMillis();
        if("bind".equals(purpose)){if(userId==null)throw new ResponseStatusException(UNAUTHORIZED);verifyPassword(userId,currentPassword);}
        db.update("DELETE FROM studio_email_challenge WHERE created_at<?",now-86400000);
        Integer ipCount=db.queryForObject("SELECT COUNT(*) FROM studio_email_challenge WHERE address=? AND created_at>?",Integer.class,address,now-3600000);
        Integer recent=db.queryForObject("SELECT COUNT(*) FROM studio_email_challenge WHERE email=? AND created_at>?",Integer.class,target,now-60000);
        if(ipCount>=10||recent>0)throw new ResponseStatusException(TOO_MANY_REQUESTS,"发送过于频繁，请稍后重试");
        var owners=db.queryForList("SELECT s.user_id FROM studio_security s JOIN user_info u ON u.id=s.user_id WHERE s.email=? AND u.status=1 AND u.is_deleted=0",target);
        boolean deliver=true;
        if("reset".equals(purpose)){userId=owners.isEmpty()?null:((Number)owners.getFirst().get("user_id")).longValue();deliver=userId!=null;}
        else if(!owners.isEmpty())deliver=false;
        String id=UUID.randomUUID().toString(),code="123456";
        db.update("INSERT INTO studio_email_challenge(id,purpose,user_id,email,code_hash,created_at,expires_at,address,consumed) VALUES(?,?,?,?,?,?,?,?,?)",id,purpose,userId,target,codeHash(id,code),now,now+300000,address,deliver?0:1);
        if(deliver){try{mail.send(target,code);}catch(RuntimeException e){db.update("UPDATE studio_email_challenge SET consumed=1 WHERE id=?",id);throw e;}}
        return Map.of("challengeId",id,"expiresIn",300,"message","本地演示验证码：123456。不发送邮件，不验证邮箱所有权。");
    }
    // Failed attempts are committed before the business transaction, so rollback cannot reset the guess limit.
    private Map<String,Object> verifyCode(String id,String code,String purpose,Long owner){
        if(!mail.available())throw new ResponseStatusException(SERVICE_UNAVAILABLE,"演示邮箱验证未开启");
        if(id==null||code==null||!code.matches("[0-9]{6}"))throw new ResponseStatusException(BAD_REQUEST,"验证码无效或已过期");
        int changed=db.update("UPDATE studio_email_challenge SET attempts=attempts+1 WHERE id=? AND purpose=? AND consumed=0 AND attempts<5 AND expires_at>?",id,purpose,System.currentTimeMillis());
        if(changed!=1)throw new ResponseStatusException(BAD_REQUEST,"验证码无效或已过期，请重新获取");
        var row=db.queryForMap("SELECT * FROM studio_email_challenge WHERE id=?",id);
        Long actual=row.get("user_id")==null?null:((Number)row.get("user_id")).longValue();
        if(!Objects.equals(row.get("code_hash"),codeHash(id,code))||("bind".equals(purpose)&&!Objects.equals(owner,actual)))throw new ResponseStatusException(BAD_REQUEST,"验证码无效或已过期");
        return row;
    }
    private void consume(String id){if(db.update("UPDATE studio_email_challenge SET consumed=1 WHERE id=? AND consumed=0 AND expires_at>?",id,System.currentTimeMillis())!=1)throw new ResponseStatusException(BAD_REQUEST,"验证码已失效，请重新获取");}
    public synchronized void register(String phone,String nickname,String password,String challenge,String code){
        if(phone==null||!phone.matches("1[0-9]{10}")||nickname==null||nickname.isBlank()||nickname.trim().length()>20)throw new ResponseStatusException(BAD_REQUEST,"请检查手机号和昵称");password(password);
        var proof=verifyCode(challenge,code,"register",null);
        try{tx.executeWithoutResult(status->{consume(challenge);
            if(db.queryForObject("SELECT COUNT(*) FROM user_info WHERE phone=?",Integer.class,phone)>0)throw new ResponseStatusException(CONFLICT,"该手机号已注册，请登录或找回密码");
            db.update("INSERT INTO user_info(phone,password,status,is_deleted,user_name) VALUES(?,?,1,0,?)",phone,passwords.encode(password),nickname.trim());
            Long id=db.queryForObject("SELECT id FROM user_info WHERE phone=?",Long.class,phone);
            db.update("INSERT INTO studio_security(user_id,email,password_updated_at) VALUES(?,?,?)",id,proof.get("email"),System.currentTimeMillis());
        });}catch(org.springframework.dao.DuplicateKeyException e){throw new ResponseStatusException(CONFLICT,"该手机号或邮箱已注册，请使用已有账户");}
    }
    public synchronized void bind(long id,String currentPassword,String challenge,String code){
        verifyPassword(id,currentPassword);var proof=verifyCode(challenge,code,"bind",id);
        try{tx.executeWithoutResult(status->{consume(challenge);ensureSecurity(id);db.update("UPDATE studio_security SET email=? WHERE user_id=?",proof.get("email"),id);});}
        catch(org.springframework.dao.DuplicateKeyException e){throw new ResponseStatusException(CONFLICT,"该邮箱已被绑定");}
    }
    private void ensureSecurity(long id){if(db.queryForObject("SELECT COUNT(*) FROM studio_security WHERE user_id=?",Integer.class,id)==0)db.update("INSERT INTO studio_security(user_id) VALUES(?)",id);}
    private void setPassword(long id,String next){
        if(db.update("UPDATE user_info SET password=? WHERE id=? AND status=1 AND is_deleted=0",passwords.encode(next),id)!=1)throw new ResponseStatusException(BAD_REQUEST,"账户不可用");
        ensureSecurity(id);db.update("UPDATE studio_security SET password_updated_at=? WHERE user_id=?",System.currentTimeMillis(),id);
        db.update("DELETE FROM studio_session WHERE user_id=?",id);db.update("UPDATE studio_email_challenge SET consumed=1 WHERE user_id=? AND purpose='reset'",id);
    }
    public synchronized void changePassword(long id,String current,String next){password(next);verifyPassword(id,current);tx.executeWithoutResult(status->setPassword(id,next));}
    public synchronized void resetPassword(String challenge,String code,String next){
        password(next);var proof=verifyCode(challenge,code,"reset",null);
        if(proof.get("user_id")==null)throw new ResponseStatusException(BAD_REQUEST,"验证码无效");
        tx.executeWithoutResult(status->{consume(challenge);long id=((Number)proof.get("user_id")).longValue();
            if(db.queryForObject("SELECT COUNT(*) FROM studio_security WHERE user_id=? AND email=?",Integer.class,id,proof.get("email"))!=1)throw new ResponseStatusException(BAD_REQUEST,"绑定邮箱已变更，请重新获取验证码");
            setPassword(id,next);
        });
    }
    public List<Map<String,Object>> history(long id){return db.queryForList("SELECT logged_at,address FROM studio_login_history WHERE user_id=? ORDER BY logged_at DESC LIMIT 50",id);}
    public Map<String,Object> security(long id){
        String phone=db.queryForObject("SELECT phone FROM user_info WHERE id=?",String.class,id);var out=new LinkedHashMap<String,Object>();out.put("phone",phone!=null&&phone.length()==11?phone.substring(0,3)+"****"+phone.substring(7):"");
        var rows=db.queryForList("SELECT email,password_updated_at FROM studio_security WHERE user_id=?",id);out.put("email",rows.isEmpty()?null:rows.getFirst().get("email"));out.put("passwordUpdatedAt",rows.isEmpty()?null:rows.getFirst().get("password_updated_at"));
        out.put("lastLoginAt",db.queryForObject("SELECT MAX(logged_at) FROM studio_login_history WHERE user_id=?",Long.class,id));out.put("passwordLogin",true);out.put("smsLogin",smsDemo);out.put("mailAvailable",mail.available());return out;
    }
}
