package com.a05.admin.service.impl;

import com.a05.admin.controller.dto.AdminUserQuery;
import com.a05.admin.controller.dto.CreateAdminUserRequest;
import com.a05.admin.controller.dto.UpdateAdminUserRequest;
import com.a05.admin.entity.Permission;
import com.a05.admin.entity.UserInfo;
import com.a05.admin.entity.UserPermissionRel;
import com.a05.admin.mapper.PermissionMapper;
import com.a05.admin.mapper.ResumeFileMapper;
import com.a05.admin.mapper.UserInfoMapper;
import com.a05.admin.mapper.UserPermissionRelMapper;
import com.a05.admin.service.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter QUERY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserPermissionRelMapper userPermissionRelMapper;

    @Autowired
    private ResumeFileMapper resumeFileMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> listUsers(AdminUserQuery query) {
        int pageNo = normalizePageNo(query.getPageNo());
        int pageSize = normalizePageSize(query.getPageSize());

        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getIsDeleted, 0)
                .orderByDesc(UserInfo::getId);

        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(UserInfo::getPhone, query.getPhone().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(UserInfo::getUserName, keyword).or().like(UserInfo::getPhone, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(UserInfo::getStatus, query.getStatus());
        }

        LocalDateTime activeStart = parseQueryTime(query.getActiveStart());
        LocalDateTime activeEnd = parseQueryTime(query.getActiveEnd());
        if (activeStart != null) {
            wrapper.ge(UserInfo::getLastActiveTime, activeStart);
        }
        if (activeEnd != null) {
            wrapper.le(UserInfo::getLastActiveTime, activeEnd);
        }

        List<UserInfo> users = userInfoMapper.selectList(wrapper);
        List<Long> userIds = users.stream()
                .map(UserInfo::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        Map<Long, List<Long>> userPermIdsMap = loadUserPermissionIdsMap(userIds);
        Map<Long, Long> resumeCountMap = loadResumeCountMap(userIds);
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (UserInfo user : users) {
            Long userId = user.getId();
            List<Long> permIds = userPermIdsMap.getOrDefault(userId, new ArrayList<>());
            boolean hasPaidService = permIds != null && !permIds.isEmpty();
            boolean hasResumeFile = resumeCountMap.getOrDefault(userId, 0L) > 0;
            boolean abnormal = user.getStatus() != null && user.getStatus() != 1;

            if (query.getHasPaidService() != null && query.getHasPaidService() != hasPaidService) {
                continue;
            }
            if (query.getHasResumeFile() != null && query.getHasResumeFile() != hasResumeFile) {
                continue;
            }
            if (query.getIsAbnormal() != null && query.getIsAbnormal() != abnormal) {
                continue;
            }
            if (query.getPaidServiceId() != null && query.getPaidServiceId() > 0 && (permIds == null || !permIds.contains(query.getPaidServiceId()))) {
                continue;
            }

            List<String> tags = buildUserTags(user, hasPaidService, hasResumeFile, abnormal);

            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("phone", user.getPhone());
            item.put("userName", user.getUserName());
            item.put("status", user.getStatus());
            item.put("tags", tags);
            item.put("createTime", formatTime(user.getCreateTime()));
            item.put("lastActiveTime", formatTime(user.getLastActiveTime()));
            filtered.add(item);
        }

        int total = filtered.size();
        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<Map<String, Object>> items = fromIndex >= total ? new ArrayList<>() : filtered.subList(fromIndex, toIndex);

        Map<String, Object> data = new HashMap<>();
        data.put("pageNo", pageNo);
        data.put("pageSize", pageSize);
        data.put("total", total);
        data.put("items", items);
        return data;
    }

    @Override
    public List<Map<String, Object>> listPermissionOptions() {
        List<Permission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .eq(Permission::getStatus, 1)
                        .orderByAsc(Permission::getId)
        );
        return permissions.stream().map(p -> {
            Map<String, Object> option = new HashMap<>();
            option.put("id", p.getId());
            option.put("serviceName", p.getPermName());
            option.put("permCode", p.getPermCode());
            return option;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createUser(CreateAdminUserRequest request) {
        if (!StringUtils.hasText(request.getPhone())) {
            throw new RuntimeException("手机号不能为空");
        }
        String phone = request.getPhone().trim();
        if (userInfoMapper.selectByPhone(phone) != null) {
            throw new RuntimeException("手机号已存在");
        }

        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword().trim() : "123456";
        String userName = StringUtils.hasText(request.getUserName()) ? request.getUserName().trim() : phone;
        int status = request.getStatus() == null ? 1 : request.getStatus();

        UserInfo user = new UserInfo();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setUserName(userName);
        user.setStatus(status);
        user.setIsDeleted(0);
        userInfoMapper.insert(user);

        return getUserDetail(user.getId());
    }

    @Override
    public Map<String, Object> updateUser(Long userId, UpdateAdminUserRequest request) {
        UserInfo user = requireUser(userId);

        if (StringUtils.hasText(request.getPhone())) {
            String newPhone = request.getPhone().trim();
            UserInfo existing = userInfoMapper.selectByPhone(newPhone);
            if (existing != null && !existing.getId().equals(userId)) {
                throw new RuntimeException("手机号已存在");
            }
            user.setPhone(newPhone);
        }
        if (request.getUserName() != null) {
            user.setUserName(request.getUserName().trim());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }
        userInfoMapper.updateById(user);
        return getUserDetail(userId);
    }

    @Override
    public Map<String, Object> getUserDetail(Long userId) {
        UserInfo user = requireUser(userId);
        List<Long> permIds = userPermissionRelMapper.listPermIdsByUserId(userId);
        List<Permission> permissions = (permIds == null || permIds.isEmpty())
                ? new ArrayList<>()
                : permissionMapper.selectBatchIds(permIds);

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("userName", user.getUserName());
        data.put("status", user.getStatus());
        data.put("createTime", formatTime(user.getCreateTime()));
        data.put("updateTime", formatTime(user.getUpdateTime()));
        data.put("lastActiveTime", formatTime(user.getLastActiveTime()));
        data.put("hasResumeFile", resumeFileMapper.countByUserId(userId) > 0);
        data.put("permissionIds", permIds == null ? new ArrayList<>() : permIds);
        data.put("permissions", permissions.stream().map(this::toPermissionMap).collect(Collectors.toList()));
        return data;
    }

    @Override
    public Map<String, Object> deleteUser(Long userId) {
        UserInfo user = requireUser(userId);
        user.setIsDeleted(1);
        userInfoMapper.updateById(user);

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return data;
    }

    @Override
    public Map<String, Object> disableUser(Long userId) {
        UserInfo user = requireUser(userId);
        user.setStatus(2);
        userInfoMapper.updateById(user);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return data;
    }

    @Override
    public Map<String, Object> enableUser(Long userId) {
        UserInfo user = requireUser(userId);
        user.setStatus(1);
        userInfoMapper.updateById(user);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return data;
    }

    @Override
    public Map<String, Object> getUserStatusPanel(Long userId) {
        UserInfo user = requireUser(userId);
        List<Long> permIds = userPermissionRelMapper.listPermIdsByUserId(userId);
        List<Permission> perms = (permIds == null || permIds.isEmpty())
                ? new ArrayList<>()
                : permissionMapper.selectBatchIds(permIds);
        long resumeCount = resumeFileMapper.countByUserId(userId);

        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step(1, "账号注册", "done", List.of("注册时间：" + formatTime(user.getCreateTime()))));

        if (perms.isEmpty()) {
            steps.add(step(2, "权限配置", "todo", List.of("尚未分配权限")));
        } else {
            String joined = perms.stream().map(Permission::getPermName).collect(Collectors.joining("、"));
            steps.add(step(2, "权限配置", "done", List.of("已分配 " + perms.size() + " 项权限", joined)));
        }

        if (resumeCount > 0) {
            steps.add(step(3, "简历上传", "done", List.of("已上传简历文件 " + resumeCount + " 份")));
        } else {
            steps.add(step(3, "简历上传", "todo", List.of("尚未上传简历")));
        }

        if (user.getStatus() != null && user.getStatus() == 1) {
            steps.add(step(4, "账号状态", "done", List.of("当前状态：启用")));
        } else {
            steps.add(step(4, "账号状态", "todo", List.of("当前状态：禁用")));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("userName", user.getUserName());
        data.put("status", user.getStatus());
        data.put("steps", steps);
        return data;
    }

    @Override
    public Map<String, Object> getUserPermissions(Long userId) {
        requireUser(userId);
        List<Long> permIds = userPermissionRelMapper.listPermIdsByUserId(userId);
        List<Permission> permissions = (permIds == null || permIds.isEmpty())
                ? new ArrayList<>()
                : permissionMapper.selectBatchIds(permIds);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("permissionIds", permIds == null ? new ArrayList<>() : permIds);
        data.put("permissions", permissions.stream().map(this::toPermissionMap).collect(Collectors.toList()));
        return data;
    }

    @Override
    public Map<String, Object> getUserPermissionsBatch(List<Long> userIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (userIds != null) {
            for (Long userId : userIds) {
                if (userId != null && userId > 0) {
                    normalized.add(userId);
                }
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        if (normalized.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            return data;
        }

        List<Long> idList = new ArrayList<>(normalized);
        List<UserInfo> users = userInfoMapper.selectBatchIds(idList);
        Set<Long> validUserIds = users.stream()
                .filter(u -> u.getIsDeleted() == null || u.getIsDeleted() == 0)
                .map(UserInfo::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (validUserIds.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            return data;
        }

        List<UserPermissionRel> rels = userPermissionRelMapper.selectList(
                new LambdaQueryWrapper<UserPermissionRel>().in(UserPermissionRel::getUserId, validUserIds)
        );
        Map<Long, List<Long>> userPermIdsMap = new HashMap<>();
        Set<Long> allPermIds = new LinkedHashSet<>();
        for (UserPermissionRel rel : rels) {
            if (rel == null || rel.getUserId() == null || rel.getPermId() == null) {
                continue;
            }
            userPermIdsMap.computeIfAbsent(rel.getUserId(), k -> new ArrayList<>()).add(rel.getPermId());
            allPermIds.add(rel.getPermId());
        }

        Map<Long, Permission> permMap = allPermIds.isEmpty()
                ? new HashMap<>()
                : permissionMapper.selectBatchIds(new ArrayList<>(allPermIds)).stream()
                .collect(Collectors.toMap(Permission::getId, p -> p, (a, b) -> a));

        for (Long userId : validUserIds) {
            List<Long> permIds = userPermIdsMap.getOrDefault(userId, new ArrayList<>());
            List<Map<String, Object>> permissions = permIds.stream()
                    .map(permMap::get)
                    .filter(p -> p != null)
                    .map(this::toPermissionMap)
                    .collect(Collectors.toList());

            Map<String, Object> item = new HashMap<>();
            item.put("userId", userId);
            item.put("permissionIds", permIds);
            item.put("permissions", permissions);
            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> assignUserPermissions(Long userId, List<Long> permissionIds) {
        requireUser(userId);

        Set<Long> normalized = new LinkedHashSet<>();
        if (permissionIds != null) {
            for (Long permissionId : permissionIds) {
                if (permissionId != null && permissionId > 0) {
                    normalized.add(permissionId);
                }
            }
        }

        List<Permission> validPerms = normalized.isEmpty()
                ? new ArrayList<>()
                : permissionMapper.selectBatchIds(new ArrayList<>(normalized));
        Set<Long> validIds = validPerms.stream().map(Permission::getId).collect(Collectors.toSet());

        userPermissionRelMapper.deleteByUserId(userId);
        if (!validIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (Long permId : validIds) {
                UserPermissionRel rel = new UserPermissionRel();
                rel.setUserId(userId);
                rel.setPermId(permId);
                rel.setCreateTime(now);
                rel.setUpdateTime(now);
                userPermissionRelMapper.insert(rel);
            }
        }
        return getUserPermissions(userId);
    }

    private UserInfo requireUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("userId 不能为空");
        }
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private List<String> buildUserTags(UserInfo user, boolean hasPaidService, boolean hasResumeFile, boolean abnormal) {
        List<String> tags = new ArrayList<>();
        if (hasPaidService) {
            tags.add("已购服务");
        }
        if (hasResumeFile) {
            tags.add("已上传简历");
        }
        if (abnormal) {
            tags.add("异常账号");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            tags.add("已禁用");
        }
        return tags;
    }

    private Map<Long, List<Long>> loadUserPermissionIdsMap(List<Long> userIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<UserPermissionRel> rels = userPermissionRelMapper.selectList(
                new LambdaQueryWrapper<UserPermissionRel>().in(UserPermissionRel::getUserId, userIds)
        );
        for (UserPermissionRel rel : rels) {
            if (rel == null || rel.getUserId() == null || rel.getPermId() == null) {
                continue;
            }
            result.computeIfAbsent(rel.getUserId(), k -> new ArrayList<>()).add(rel.getPermId());
        }
        return result;
    }

    private Map<Long, Long> loadResumeCountMap(List<Long> userIds) {
        Map<Long, Long> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<Map<String, Object>> rows = resumeFileMapper.countByUserIds(userIds);
        for (Map<String, Object> row : rows) {
            Long userId = readLong(row, "userId", "user_id");
            Long count = readLong(row, "resumeCount", "resume_count");
            if (userId == null || count == null) {
                continue;
            }
            result.put(userId, count);
        }
        return result;
    }

    private Long readLong(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = row.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> step(int index, String name, String status, List<String> details) {
        Map<String, Object> step = new HashMap<>();
        step.put("index", index);
        step.put("name", name);
        step.put("status", status);
        step.put("details", details == null ? new ArrayList<>() : details);
        return step;
    }

    private Map<String, Object> toPermissionMap(Permission permission) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", permission.getId());
        item.put("permCode", permission.getPermCode());
        item.put("permName", permission.getPermName());
        item.put("menuPath", permission.getMenuPath());
        item.put("status", permission.getStatus());
        return item;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(API_TIME);
    }

    private LocalDateTime parseQueryTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim(), QUERY_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }
}
