package com.baracklee.ui.service;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.ServerEntity;
import com.baracklee.mq.biz.service.ServerService;
import com.baracklee.mq.biz.service.UserInfoHolder;
import com.baracklee.mq.biz.ui.dto.request.ServerGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.ServerChangeStatusResponse;
import com.baracklee.mq.biz.ui.dto.response.ServerGetListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UiServerService {
    private ServerService serverService;
    private UserInfoHolder userInfoHolder;

    private SoaConfig soaConfig;

    @Autowired
    public UiServerService(ServerService serverService, UserInfoHolder userInfoHolder, SoaConfig soaConfig) {
        this.serverService = serverService;
        this.userInfoHolder = userInfoHolder;
        this.soaConfig = soaConfig;
    }

    public ServerGetListResponse findBy(ServerGetListRequest serverGetListRequest){
        Map<String, Object> parameterMap = new HashMap<>();
        if(!StringUtils.isEmpty(serverGetListRequest.getStatusFlag())){
            parameterMap.put(ServerEntity.FdStatusFlag,serverGetListRequest.getStatusFlag());
        }
        if(!StringUtils.isEmpty(serverGetListRequest.getServerVersion())){
            parameterMap.put(ServerEntity.FdServerVersion,serverGetListRequest.getServerVersion());
        }
        long count = serverService.count(parameterMap);
        List<ServerEntity> serverList = serverService.getList(parameterMap,
                Long.valueOf(serverGetListRequest.getPage()), Long.valueOf(serverGetListRequest.getLimit()));

        return new ServerGetListResponse(count,serverList);
    }

    private BaseUiResponse batchPull(List<ServerEntity> serverList){
        BaseUiResponse baseUiResponse=new BaseUiResponse();
        List<Long> serverIds=new ArrayList<>();
        try {
            for (ServerEntity serverEntity:serverList) {
                serverIds.add(serverEntity.getId());
            }
            baseUiResponse=checkServerCount(serverIds.size(),baseUiResponse);
            if(baseUiResponse.isSuc()){
                serverService.batchUpdate(serverIds,1);
            }

        }catch (Exception e){
            baseUiResponse.setSuc(false);
            baseUiResponse.setCode("1");
            baseUiResponse.setMsg("拉入异常："+e);
        }
        return baseUiResponse;

    }
    public BaseUiResponse batchPush(List<ServerEntity> serverList){
        BaseUiResponse baseUiResponse=new BaseUiResponse();
        List<Long> serverIds=new ArrayList<>();
        try {
            for (ServerEntity serverEntity:serverList) {
                serverIds.add(serverEntity.getId());
            }
            int onlineServer=serverService.getOnlineServerNum();
            if ((onlineServer - serverIds.size()) < soaConfig.getMinServerCount()
                    && soaConfig.getMinServerCount() > 0) {
                baseUiResponse.setSuc(false);
                baseUiResponse.setMsg("在线实例数量不能少于"+soaConfig.getMinServerCount());
            }else{
                serverService.batchUpdate(serverIds,0);
                baseUiResponse.setSuc(true);
            }

        }catch (Exception e){
            baseUiResponse.setSuc(false);
            baseUiResponse.setCode("1");
            baseUiResponse.setMsg("拉出异常："+e);
        }
        return baseUiResponse;

    }
    private BaseUiResponse checkServerCount(int batchCount, BaseUiResponse baseUiResponse) {
        int onlineServer=serverService.getOnlineServerNum();

        if((onlineServer+batchCount)>soaConfig.getMaxServerCount()){
            baseUiResponse.setSuc(false);
            baseUiResponse.setMsg("在线实例不能超过:"+soaConfig.getMaxServerCount());
        }else if((onlineServer+batchCount)>soaConfig.getMinServerCount()&&batchCount>soaConfig.getBatchNum()) {
            baseUiResponse.setSuc(false);
            baseUiResponse.setMsg("一次拉取不能超过："+soaConfig.getBatchNum());
        }else{
            baseUiResponse.setSuc(true);
        }
        return  baseUiResponse;

    }

    public ServerChangeStatusResponse changeStatusFlag(String serverId) {
        ServerEntity serverEntity=serverService.get(Long.parseLong(serverId));
        serverEntity.setStatusFlag(serverEntity.getStatusFlag()==1 ? 0:1);
        serverEntity.setUpdateBy(userInfoHolder.getUser().getUserId());
        serverService.update(serverEntity);
        return new ServerChangeStatusResponse();
    }

    public BaseUiResponse<String> onLineServer(){
        BaseUiResponse baseUiResponse=new BaseUiResponse();
        //如果在线server数量小于apollo配置数量
        if(soaConfig.getMinServerCount()>serverService.getOnlineServerNum()){
            baseUiResponse.setMsg("在线server的数量小于："+soaConfig.getMinServerCount()+"请尽快处理！");
            baseUiResponse.setCode("1");
        }else{
            baseUiResponse.setCode("0");
        }
        return baseUiResponse;
    }
}
