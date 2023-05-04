package com.baracklee.ui.service;

import com.baracklee.mq.biz.dal.meta.ConsumerGroupRepository;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.RoleService;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.mq.biz.ui.dto.request.ConsumerGroupGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupGetListResponse;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupGetNamesResponse;
import com.baracklee.mq.biz.ui.dto.response.ConsumerGroupSelectResponse;
import com.baracklee.mq.biz.ui.vo.ConsumerGroupVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Barack Lee
 */
@Service
public class UiConsumerGroupService {
    private ConsumerGroupRepository consumerGroupRepository;

    private ConsumerGroupService consumerGroupService;

    private RoleService roleService;

    private UserInfoHolder userInfoHolder;

    public UiConsumerGroupService(ConsumerGroupRepository consumerGroupRepository,
                                  ConsumerGroupService consumerGroupService,
                                  RoleService roleService,
                                  UserInfoHolder userInfoHolder) {
        this.consumerGroupRepository = consumerGroupRepository;
        this.consumerGroupService = consumerGroupService;
        this.roleService = roleService;
        this.userInfoHolder = userInfoHolder;
    }

    public ConsumerGroupGetListResponse findBy(ConsumerGroupGetListRequest consumerGroupGetListRequest){
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("name", consumerGroupGetListRequest.getConsumerGroupName());
        parameterMap.put("appId", consumerGroupGetListRequest.getAppId());
        parameterMap.put("ownerNames", consumerGroupGetListRequest.getOwnerNames());
        parameterMap.put("subEnv",consumerGroupGetListRequest.getSubEnv());
        if (StringUtils.isNotBlank(consumerGroupGetListRequest.getId())) {
            parameterMap.put("id", Long.valueOf(consumerGroupGetListRequest.getId()));
        }
        if (StringUtils.isNotEmpty(consumerGroupGetListRequest.getMode())) {
            parameterMap.put("mode", Integer.parseInt(consumerGroupGetListRequest.getMode()));
        }
        long page = Long.valueOf(consumerGroupGetListRequest.getPage());
        long pageSize = Long.valueOf(consumerGroupGetListRequest.getLimit());
        parameterMap.put("start1", (page - 1) * pageSize);
        parameterMap.put("offset1", pageSize);
        long count = consumerGroupService.countByOwnerNames(parameterMap);
        List<ConsumerGroupEntity> consumerGroupList = consumerGroupService.getByOwnerNames(parameterMap);
        String currentUserId = userInfoHolder.getUserId();
        List<ConsumerGroupVo> consumerGroupVoList = consumerGroupList.stream().map(consumerGroupEntity -> {
            ConsumerGroupVo consumerGroupVo = new ConsumerGroupVo(consumerGroupEntity);
            consumerGroupVo.setRole(roleService.getRole(currentUserId, consumerGroupEntity.getOwnerIds()));
            return consumerGroupVo;
        }).collect(Collectors.toList());
        return new ConsumerGroupGetListResponse(count,consumerGroupVoList);
    }

    public ConsumerGroupEntity findById(long consumerGroupId){
        return consumerGroupRepository.getById(consumerGroupId);
    }

    public ConsumerGroupGetNamesResponse getConsumerGpNames(String keyword, int offset, int limit) {
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
        List<String> consumerGroupList = new LinkedList<>();
        for (String name : consumerGroupMap.keySet()) {
            if (name.toLowerCase().startsWith(keyword.toLowerCase())) {
                consumerGroupList.add(name);
            }
        }

        if (offset + limit > consumerGroupList.size()) {
            limit = consumerGroupList.size() - offset;
        }
        return new ConsumerGroupGetNamesResponse(new Long(consumerGroupList.subList(offset, limit).size()),
                consumerGroupList.subList(offset, limit));

    }

    public ConsumerGroupSelectResponse searchConsumerGroups(String keyword, int offset, int limit) {
        Map<String, ConsumerGroupEntity> consumerGroupMap = consumerGroupService.getCache();
        List<String> consumerGroupList = new LinkedList<>();
        for (String name : consumerGroupMap.keySet()) {
            if (name.indexOf(keyword) != -1) {
                consumerGroupList.add(name);
            }
        }
        Collections.sort(consumerGroupList, new Comparator<String>() {
            @Override
            public int compare(String q1, String q2) {
                return q1.compareTo(q2);
            }
        });

        if (offset + limit > consumerGroupList.size()) {
            limit = consumerGroupList.size() - offset;
        }

        return new ConsumerGroupSelectResponse(new Long(consumerGroupList.subList(offset, limit).size()),
                consumerGroupList.subList(offset, limit));
    }

}
