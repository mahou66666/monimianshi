package com.example.springbootfront.studio;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Component @Order(1)
@ConditionalOnProperty(name="studio.enabled",havingValue="true")
public class StudioFilter extends OncePerRequestFilter {
    private final StudioAuth auth;
    @Value("${studio.exclusive-api:true}") private boolean exclusive;
    public StudioFilter(StudioAuth auth) { this.auth=auth; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String path=req.getRequestURI();
        res.setHeader("Cache-Control","no-store"); res.setHeader("X-Content-Type-Options","nosniff");
        if(!path.startsWith("/api/studio/")) {
            if(exclusive && !path.equals("/error")) { res.sendError(404); return; }
            chain.doFilter(req,res); return;
        }
        boolean open=path.equals("/api/studio/auth/login") && req.getMethod().equals("POST");
        open |= java.util.List.of("/api/studio/auth/sms/code","/api/studio/auth/sms/login","/api/studio/auth/register/code","/api/studio/auth/reset/code","/api/studio/auth/register","/api/studio/auth/reset").contains(path) && req.getMethod().equals("POST");
        open |= path.equals("/api/studio/auth/options") && req.getMethod().equals("GET");
        open |= path.equals("/api/studio/capabilities") && req.getMethod().equals("GET");
        try { if(!open) req.setAttribute("studioUserId",auth.authenticate(req.getHeader("Authorization"))); }
        catch(ResponseStatusException e) { res.setStatus(e.getStatusCode().value());res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"登录已失效，请先登录真实账户\"}");return; }
        chain.doFilter(req,res);
    }
}
