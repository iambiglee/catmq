package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.dal.meta.DicRepository;
import com.baracklee.mq.biz.entity.DicEntity;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @author dal-generator
 */
@Service
public class DicServiceImpl extends AbstractBaseService<DicEntity>  {
    @Autowired
    private DicRepository dicRepository;

    @PostConstruct
    private void init() {
        super.setBaseRepository(dicRepository);
    }
}
