package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.QueueRepository;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.service.Message01Service;
import com.baracklee.mq.biz.service.QueueService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueueServiceImpl extends AbstractBaseService<QueueEntity>  implements QueueService {

    @Resource
    QueueRepository queueRepository;
    @Resource
    Message01Service message01Service;

    private void init(){
        super.setBaseRepository(queueRepository);
    }
    @Override
    public List<QueueEntity> getQueuesByTopicId(Long topicId) {
        Map<String, Object> conditionMap=new HashMap<>();
        conditionMap.put("topicId",topicId);
        return getList(conditionMap);
    }

    @Override
    public long getMaxId(long id, String tbName) {
        long maxid=message01Service.getMaxId(tbName);
        return maxid;
    }

    @Override
    public Map<Long, QueueEntity> getAllQueueMap() {
        return null;
    }

    @Override
    public void deleteMessage(List<QueueEntity> queueEntities, long consumerGroupId) {

    }

    @Override
    public Map<String, List<QueueEntity>> getAllLocatedTopicWriteQueue() {
        return null;
    }

    @Override
    public Map<String, List<QueueEntity>> getAllLocatedTopicQueue() {
        return null;
    }
}
