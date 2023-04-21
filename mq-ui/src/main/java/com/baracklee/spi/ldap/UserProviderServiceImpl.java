package com.baracklee.spi.ldap;

import com.baracklee.mq.biz.dto.Organization;
import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.spi.UserProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Author:  BarackLee
 */
public class UserProviderServiceImpl implements UserProviderService {
    private final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());



    @Override
    public Map<String, Organization> getOrgs() {
        return null;
    }

    @Override
    public Map<String, UserInfo> getUsers() {
        return null;
    }

    @Override
    public boolean login(String username, String password) {
        return false;
    }
}
