package com.baracklee.ui.service;

import com.baracklee.mq.biz.dal.meta.MqLockRepository;
import com.baracklee.mq.biz.dto.request.BaseUiRequst;
import com.baracklee.mq.biz.entity.MqLockEntity;
import com.baracklee.mq.biz.service.MqLockService;
import com.baracklee.mq.biz.service.impl.MqLockServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author： Barack Lee
 */
@Service
public class UiMqLockService {
    @Autowired
    private MqLockRepository mqLockRepository;
    MqLockService mqLockService = null;

    @PostConstruct
    private  void init(){
        mqLockService=new MqLockServiceImpl(mqLockRepository);
    }
    public MqLockGetListResponse findBy(BaseUiRequst baseUiRequst) {

        Map<String, Object> parameterMap = new HashMap<>();
        long count = mqLockService.count(parameterMap);
        List<MqLockEntity> consumerGroupList = mqLockService.getList(parameterMap,
                Long.valueOf(baseUiRequst.getPage()), Long.valueOf(baseUiRequst.getLimit()));

        return new MqLockGetListResponse(count,consumerGroupList);
    }

    public MqLockDeleteResponse delete(String lockId){
        mqLockService.delete(Long.parseLong(lockId));
        return new MqLockDeleteResponse();
    }
}
