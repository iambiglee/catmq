package com.baracklee.mq.biz.service;

public interface RoleService {
    int getRole(String userId, String ownerIds);

    int getRole(String userId);

    boolean isAdmin(String userId);

    String getRoleName(String userId);
}
