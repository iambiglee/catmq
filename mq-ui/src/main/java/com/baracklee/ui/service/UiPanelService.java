package com.baracklee.ui.service;

import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.QueueService;
import com.baracklee.mq.biz.ui.enums.NodeTypeEnum;
import com.baracklee.mq.biz.ui.enums.NormalFlagEnum;
import com.baracklee.mq.biz.ui.enums.ReadWriteEnum;
import com.baracklee.mq.biz.ui.vo.PanelNodeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Barack Lee
 */
@Service
public class UiPanelService {

    private QueueService queueService;

    private DbNodeService dbNodeService;

    @Autowired
    public UiPanelService(QueueService queueService, DbNodeService dbNodeService) {
        this.queueService = queueService;
        this.dbNodeService = dbNodeService;
    }

    public List<PanelNodeVo> getNodePanel(){
        Map<Long, DbNodeEntity> nodeMap =  dbNodeService.getCache();
        List<PanelNodeVo> panelNodeVoList = new ArrayList<>();
        Map<Long, QueueEntity> queueEntityMap = queueService.getAllQueueMap();
        for (DbNodeEntity dbNodeEntity : nodeMap.values()) {
            PanelNodeVo panelNodeVo = new PanelNodeVo();
            panelNodeVo.setId(dbNodeEntity.getId());
            panelNodeVo.setNormalFlag(NormalFlagEnum.getDescByCode(dbNodeEntity.getNormalFlag()));
            panelNodeVo.setNodeType(NodeTypeEnum.getDescByCode(dbNodeEntity.getNodeType()));
            panelNodeVo.setReadOnly(ReadWriteEnum.getDescByCode(dbNodeEntity.getReadOnly()));
            long undistributedCount = queueEntityMap.values().stream().filter(
                    v -> v.getDbNodeId() == dbNodeEntity.getId() && v.getTopicId() == 0).count();
            long distributedCount = queueEntityMap.values().stream().filter(
                    v -> v.getDbNodeId() == dbNodeEntity.getId() && v.getTopicId() != 0).count();
            panelNodeVo.setDistributedCount(distributedCount);
            panelNodeVoList.add(panelNodeVo);
        }
        return panelNodeVoList;
    }


}
