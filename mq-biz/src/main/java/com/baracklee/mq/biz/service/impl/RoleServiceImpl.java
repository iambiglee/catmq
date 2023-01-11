package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.UserRoleEnum;
import com.baracklee.mq.biz.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;


import java.util.Arrays;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {
    private SoaConfig soaConfig;

    @Autowired
    public RoleServiceImpl(SoaConfig soaConfig) {
        this.soaConfig = soaConfig;
    }

    @Override
    public int getRole(String userId, String ownerIds) {
        if (StringUtils.isNotEmpty(userId) && isAdmin(userId)) {
            return UserRoleEnum.SUPER_USER.getRoleCode();
        } else if (StringUtils.isNotEmpty(userId) && StringUtils.isNotEmpty(ownerIds)
                && Arrays.asList(ownerIds.split(",")).contains(userId)) {
            return UserRoleEnum.OWNER.getRoleCode();
        } else {
            return UserRoleEnum.USER.getRoleCode();
        }
    }

    @Override
    public int getRole(String userId) {
        if (StringUtils.isNotEmpty(userId) && isAdmin(userId)) {
            return UserRoleEnum.SUPER_USER.getRoleCode();
        } else {
            return UserRoleEnum.USER.getRoleCode();
        }    }

    @Override
    public boolean isAdmin(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return false;
        }

        return false;
    }

    @Override
    public String getRoleName(String userId) {
        if (StringUtils.isNotEmpty(userId) && isAdmin(userId)) {
            return UserRoleEnum.SUPER_USER.getDescription();
        } else {
            return UserRoleEnum.USER.getDescription();
        }
    }
}
