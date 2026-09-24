package com.example.springbootfront.studio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

/** Explicit local demonstration only; never sends mail or claims email ownership verification. */
@Service @ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioMail {
    @Value("${studio.email-demo:false}") private boolean demo;
    public boolean available(){return demo;}
    public void send(String email,String code){
        if(!demo)throw new ResponseStatusException(SERVICE_UNAVAILABLE,"邮箱验证尚未开放");
    }
}
