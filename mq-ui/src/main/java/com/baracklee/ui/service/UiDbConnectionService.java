package com.baracklee.ui.service;

import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.ui.dto.response.DbNodeConnectionsResponse;
import com.baracklee.mq.biz.ui.dto.response.QueueCountResponse;
import com.baracklee.mq.biz.ui.vo.ConnectionsVo;
import com.baracklee.mq.biz.ui.vo.OnLineNumsVo;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Barack Lee
 */
@Service
public class UiDbConnectionService {
    private Message01Service message01Service;

    private DbNodeService dbNodeService;

    private DbService dbService;

    private Environment environment;

    private ConsumerService consumerService;

    private UiQueueOffsetService uiQueueOffsetService;

    private ServerService serverService;

    private UiQueueService uiQueueService;

    private QueueOffsetService queueOffsetService;

    public UiDbConnectionService(Message01Service message01Service,
                                 DbNodeService dbNodeService,
                                 DbService dbService,
                                 Environment environment,
                                 ConsumerService consumerService,
                                 UiQueueOffsetService uiQueueOffsetService,
                                 ServerService serverService,
                                 UiQueueService uiQueueService,
                                 QueueOffsetService queueOffsetService) {
        this.message01Service = message01Service;
        this.dbNodeService = dbNodeService;
        this.dbService = dbService;
        this.environment = environment;
        this.consumerService = consumerService;
        this.uiQueueOffsetService = uiQueueOffsetService;
        this.serverService = serverService;
        this.uiQueueService = uiQueueService;
        this.queueOffsetService = queueOffsetService;
    }


    public DbNodeConnectionsResponse getConnections(){
        Map<Long, DbNodeEntity> dbNodeMap = dbNodeService.getCache();
        Map<String, DbNodeEntity> dataSourceMap = new HashMap<>();
        List<ConnectionsVo>connectionsVoList=new ArrayList<>();
        try {
            for (long dbId : dbNodeMap.keySet()) {
                if (!dataSourceMap.containsKey(dbNodeMap.get(dbId).getIp())) {
                    dataSourceMap.put(dbNodeMap.get(dbId).getIp(), dbNodeMap.get(dbId));
                }
            }
            for (String ip : dataSourceMap.keySet()) {
                message01Service.setDbId(dataSourceMap.get(ip).getId());
                String maxConnection = message01Service.getMaxConnectionsCount();
                message01Service.setDbId(dataSourceMap.get(ip).getId());
                int conCount = message01Service.getConnectionsCount();
                ConnectionsVo connectionsVo=new ConnectionsVo();
                connectionsVo.setIp(ip);
                connectionsVo.setMaxConnection(maxConnection);
                connectionsVo.setCurConnection(conCount+"");
                connectionsVoList.add(connectionsVo);
            }
            String basicMaxConnection=dbService.getMaxConnectionsCount();
            int basicConCount=dbService.getConnectionsCount();
            String basicDbUrl=environment.getProperty("spring.datasource.url");
            String basicIp=basicDbUrl.substring(basicDbUrl.indexOf("//")+2,basicDbUrl.lastIndexOf(":"));
            ConnectionsVo connectionsVo=new ConnectionsVo();
            connectionsVo.setIp(basicIp);
            connectionsVo.setMaxConnection(basicMaxConnection);
            connectionsVo.setCurConnection(basicConCount+"");
            connectionsVoList.add(connectionsVo);
            return new DbNodeConnectionsResponse(new Long(connectionsVoList.size()),connectionsVoList);
        } catch (Exception e) {
            return new DbNodeConnectionsResponse("1","获取连接数异常，异常信息为：" + e.getMessage());
        }
    }


    public BaseUiResponse<List<OnLineNumsVo>> getOnlineNums(){
        Long consumerCount=consumerService.countBy(new HashMap<>());
        Long onLineServerNum=new Long(serverService.getOnlineServerNum());
        QueueCountResponse normalQueueCountResponse=uiQueueService.count(1);
        QueueCountResponse failQueueCountResponse=uiQueueService.count(2);
        List<OnLineNumsVo> onLineNumsVoList=new ArrayList<>();
        List<String> allBroadcastGroupList=new ArrayList<>();
        List<String> onlineBroadcastGroupList=new ArrayList<>();
        List<QueueOffsetEntity> queueOffsetList=queueOffsetService.getCacheData();
        for (QueueOffsetEntity queueOffset:queueOffsetList) {
            if(queueOffset.getConsumerGroupMode()==2){
                if(!allBroadcastGroupList.contains(queueOffset.getConsumerGroupName())){
                    allBroadcastGroupList.add(queueOffset.getConsumerGroupName());
                }
                if(!StringUtils.isEmpty(queueOffset.getConsumerName())&&!onlineBroadcastGroupList.contains(queueOffset.getConsumerGroupName())){
                    onlineBroadcastGroupList.add(queueOffset.getConsumerGroupName());
                }
            }
        }

        onLineNumsVoList.add(new OnLineNumsVo("在线consumer数",consumerCount));
        onLineNumsVoList.add(new OnLineNumsVo("消费者组总数",uiQueueOffsetService.getConsumerGroupNum()));
        onLineNumsVoList.add(new OnLineNumsVo("在线消费者组个数",uiQueueOffsetService.getUsingConsumerGroupNum()));
        onLineNumsVoList.add(new OnLineNumsVo("离线消费者组个数",uiQueueOffsetService.getUselessConsumerGroupNum()));
        onLineNumsVoList.add(new OnLineNumsVo("在线server数量",onLineServerNum));
        onLineNumsVoList.add(new OnLineNumsVo("正常队列已分配数量",normalQueueCountResponse.getData().get("distributedCount")));
        onLineNumsVoList.add(new OnLineNumsVo("失败队列已分配数量",failQueueCountResponse.getData().get("distributedCount")));
        onLineNumsVoList.add(new OnLineNumsVo("广播组总数",new Long(allBroadcastGroupList.size())));
        onLineNumsVoList.add(new OnLineNumsVo("在线广播组数",new Long(onlineBroadcastGroupList.size())));

        BaseUiResponse<List<OnLineNumsVo>> listBaseUiResponse = new BaseUiResponse<>(onLineNumsVoList);
        return listBaseUiResponse;
    }

}
