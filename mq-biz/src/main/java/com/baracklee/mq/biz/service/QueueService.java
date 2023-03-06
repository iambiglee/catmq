package com.baracklee.mq.biz.service;

import com.baracklee.mq.biz.dto.AnalyseDto;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.service.common.BaseService;

import java.util.List;
import java.util.Map;

public interface QueueService extends BaseService<QueueEntity> {
    /*
     * key为topicName
     */
    Map<String, List<QueueEntity>> getAllLocatedTopicQueue();

    /*
     * key为topicName
     */
    Map<String, List<QueueEntity>> getAllLocatedTopicWriteQueue();

    /*
     * key为queueId
     */
    Map<Long, QueueEntity> getAllQueueMap();

    /**
     * @return 获取所有的Queue的注册信息
     */
    List<QueueEntity> getAllLocatedQueue();

    /**
     *
     * @param nodeIds id 分区
     * @param topicId topic
     * @return 获取指定范围的topic
     */
    List<QueueEntity> getDistributedList(List<Long> nodeIds, Long topicId);

    /**
     *
     * @param topicId topic
     * @return 分区号
     */
    List<Long> getTopDistributedNodes(Long topicId);

    /**
     * 更新队列消息乐观锁
     * @param queueEntity 队列实体
     */
    void updateWithLock(QueueEntity queueEntity);

    // key为queueid，值为最大id+1
    Map<Long, Long> getMax();

    long getMaxId(long queueId, String tbName);

    /**
     * IP/db name change
     * @param ip
     * @param dbName 失败队列 or 成功队列
     * @param oldIp
     * @param oldDbName
     */
    void updateForDbNodeChange(String ip, String dbName, String oldIp, String oldDbName);

    /**
     *
     * @param dbNodeId node id
     * @return 获取表名字 ""message_01"
     */
    List<String> getTableNamesByDbNode(Long dbNodeId);

    /**
     *
     * @param id
     * @param page
     * @param limit
     * @return 分页获取的DB 数据
     */
    List<AnalyseDto> countTopicByNodeId(Long id, Long page, Long limit);

    /**
     * 根据参数dbNodeId获取该节点下所有Topic的分布节点
     *
     * @param dbNodeId
     * @return
     */
    List<AnalyseDto> getDistributedNodes(Long dbNodeId);

    /**
     *
     * @return 所有的QTY和可写QTY的数量
     */
    Map<Long, AnalyseDto> getQueueQuantity();

    int updateMinId(Long id, Long minId);

    long getLastVersion();

    void resetCache();

    void updateCache();

    void forceUpdateCache();

    List<QueueEntity> getQueuesByTopicId(long topicId);

    void deleteMessage(List<QueueEntity> queueEntities, long consumerGroupId);

    void doDeleteMessage(QueueEntity queueEntity);

    List<QueueEntity> getTopUndistributed(int topNum, int nodeType, Long topicId);

    void truncate(QueueEntity queueEntity);

    List<QueueEntity> getListBy(Map<String, Object> conditionMap, long page, long pageSize);

    long countBy(Map<String, Object> conditionMap);
}
