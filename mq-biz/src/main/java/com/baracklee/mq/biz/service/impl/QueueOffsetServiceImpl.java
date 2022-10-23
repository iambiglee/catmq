package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.QueueOffsetRepository;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.QueueOffsetService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import com.baracklee.mq.biz.service.common.BaseService;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class QueueOffsetServiceImpl extends AbstractBaseService<QueueOffsetEntity> implements QueueOffsetService {

    @Resource
    private QueueOffsetRepository queueOffsetRepository;
    @Override
    public void updateConsumerId(QueueOffsetEntity t1) {
        queueOffsetRepository.updateConsumerId(t1);
    }

    @Override
    public List<QueueOffsetEntity> getByConsumerGroupIds(ArrayList<Long> consumerGroupIds) {
        if (CollectionUtils.isEmpty(consumerGroupIds)) {
            return new ArrayList<>();
        }
        return queueOffsetRepository.getByConsumerGroupIds(consumerGroupIds);
    }
}
