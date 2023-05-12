package com.baracklee.ui.service;

import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.ui.dto.response.DepartmentReportResponse;
import com.baracklee.mq.biz.ui.vo.DepartmentVo;
import com.baracklee.mq.biz.ui.vo.TopicVo;
import com.baracklee.ui.spi.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Barack Lee
 */
@Service
public class UiDepartmentService {


    ConsumerGroupService consumerGroupService;

    UiTopicService uiTopicService;

    UserService userService;

    public UiDepartmentService(ConsumerGroupService consumerGroupService,
                               UiTopicService uiTopicService,
                               UserService userService) {
        this.consumerGroupService = consumerGroupService;
        this.uiTopicService = uiTopicService;
        this.userService = userService;
    }


    public DepartmentReportResponse getDepartmentReport(){
        Map<String, ConsumerGroupEntity> consumerGroupMap=consumerGroupService.getCache();
        List<DepartmentVo> departmentVos=new ArrayList<>();
        Map<String,DepartmentVo> departmentVoMap=new HashMap<>();
        Map<String, TopicVo> topicVoMap=new HashMap<>();
        for (TopicVo topicVo:uiTopicService.getTopicVos()) {
            //只统计常规topic，不统计失败topic
            if(topicVo.getTopicType()==1){
                topicVoMap.put(topicVo.getName(),topicVo);
                String userDepartment="";
                List<String> ownerList= Arrays.asList(topicVo.getOwnerIds().split(","));
                UserInfo user=userService.findByUserId(ownerList.get(0));
                if(user!=null){
                    userDepartment=user.getDepartment();
                }else{
                    userDepartment=topicVo.getDptName();
                }
                if(departmentVoMap.containsKey(userDepartment)){
                    departmentVoMap.get(userDepartment).setPublishNum(departmentVoMap.get(userDepartment).getPublishNum()+topicVo.getAvgCount()*7);
                }else{
                    DepartmentVo departmentVo=new DepartmentVo();
                    departmentVo.setName(userDepartment);
                    //该topic一周的发送量
                    departmentVo.setPublishNum(topicVo.getAvgCount()*7);
                    departmentVoMap.put(userDepartment,departmentVo);
                }
            }

        }

        for (String groupName:consumerGroupMap.keySet()) {
            String userDpt="";
            List<String> ownerList= Arrays.asList(consumerGroupMap.get(groupName).getOwnerIds().split(","));
            UserInfo user=userService.findByUserId(ownerList.get(0));
            if(user!=null){
                userDpt=user.getDepartment();
            }else{
                userDpt=consumerGroupMap.get(groupName).getDptName();
            }

            String topicNames=consumerGroupMap.get(groupName).getTopicNames();
            List<String> topicList=new ArrayList<>();
            if(StringUtils.isEmpty(topicNames)){
                continue;
            }else{
                topicList=Arrays.asList(topicNames.split(","));
            }

            long groupConsumerCount=0;
            for (String topicName:topicList) {
                if(topicVoMap.get(topicName)!=null){
                    groupConsumerCount+=topicVoMap.get(topicName).getAvgCount()*7;
                }

            }

            if(departmentVoMap.containsKey(userDpt)){
                departmentVoMap.get(userDpt).setConsumerNum(departmentVoMap.get(userDpt).getConsumerNum()+groupConsumerCount);
            }else{
                DepartmentVo departmentVo=new DepartmentVo();
                departmentVo.setName(userDpt);
                departmentVo.setConsumerNum(groupConsumerCount);
                departmentVoMap.put(userDpt,departmentVo);
            }

        }

        for (String dpt:departmentVoMap.keySet()) {
            departmentVos.add(departmentVoMap.get(dpt));
        }

        return new DepartmentReportResponse((long) departmentVos.size(),departmentVos);
    }

}
