package com.a05.admin.interceptor;

import com.a05.admin.auth.RequirePermission;
import com.a05.admin.common.Result;
import com.a05.admin.entity.Permission;
import com.a05.admin.mapper.PermissionMapper;
import com.a05.admin.mapper.UserPermissionRelMapper;
import com.a05.admin.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserPermissionRelMapper userPermissionRelMapper;
    private final PermissionMapper permissionMapper;

    public AuthInterceptor(JwtUtil jwtUtil,
                           ObjectMapper objectMapper,
                           StringRedisTemplate stringRedisTemplate,
                           UserPermissionRelMapper userPermissionRelMapper,
                           PermissionMapper permissionMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userPermissionRelMapper = userPermissionRelMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = getToken(request);
        if (token == null || token.isBlank()) {
            writeFail(response, 40100, "未登录");
            return false;
        }

        try {
            Claims claims = jwtUtil.parseAndValidate(token);
            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                writeFail(response, 40101, "登录已过期");
                return false;
            }

            String latestToken = stringRedisTemplate.opsForValue().get(tokenKey(userId));
            if (latestToken == null || latestToken.isBlank() || !latestToken.equals(token)) {
                writeFail(response, 40101, "登录已过期");
                return false;
            }

            request.setAttribute("userId", userId);

            Long loginUserId = parseLong(userId);
            if (loginUserId == null) {
                writeFail(response, 40003, "无权限访问该资源");
                return false;
            }

            Set<String> userPermissionCodes = null;

            List<String> requiredPermissionCodes = resolveRequiredPermissionCodes(handler);
            if (!requiredPermissionCodes.isEmpty()) {
                userPermissionCodes = loadUserPermissionCodes(loginUserId);
                boolean matched = requiredPermissionCodes.stream().anyMatch(userPermissionCodes::contains);
                if (!matched) {
                    writeFail(response, 40003, "无权限访问该资源");
                    return false;
                }
            }

            Long pathUserId = resolvePathUserId(request);
            if (pathUserId != null && !pathUserId.equals(loginUserId)) {
                if (userPermissionCodes == null) {
                    userPermissionCodes = loadUserPermissionCodes(loginUserId);
                }
                if (!userPermissionCodes.contains("company:manage")) {
                    writeFail(response, 40003, "无权限访问该用户资源");
                    return false;
                }
            }

            return true;
        } catch (JwtException ex) {
            writeFail(response, 40101, "登录已过期");
            return false;
        }
    }

    private String tokenKey(String userId) {
        return "admin:auth:token:" + userId;
    }

    private String getToken(HttpServletRequest request) {
        String xToken = request.getHeader("X-Token");
        if (xToken != null && !xToken.isBlank()) {
            return xToken;
        }
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        return auth.startsWith(prefix) ? auth.substring(prefix.length()).trim() : auth.trim();
    }

    private List<String> resolveRequiredPermissionCodes(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return new ArrayList<>();
        }

        RequirePermission methodAnnotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (methodAnnotation != null) {
            return normalizeCodes(methodAnnotation.anyOf());
        }

        RequirePermission classAnnotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        if (classAnnotation != null) {
            return normalizeCodes(classAnnotation.anyOf());
        }

        return new ArrayList<>();
    }

    private List<String> normalizeCodes(String[] codes) {
        if (codes == null || codes.length == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(codes)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private Set<String> loadUserPermissionCodes(Long userId) {
        List<Long> permIds = userPermissionRelMapper.listPermIdsByUserId(userId);
        if (permIds == null || permIds.isEmpty()) {
            return Set.of();
        }
        List<Permission> permissions = permissionMapper.selectBatchIds(permIds);
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        return permissions.stream()
                .map(Permission::getPermCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long resolvePathUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables)) {
            return null;
        }
        Object raw = variables.get("userId");
        if (raw == null) {
            return null;
        }
        return parseLong(String.valueOf(raw));
    }

    private void writeFail(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> body = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
