package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.util.SoaConfig;
import com.baracklee.mq.biz.dal.meta.MqLockRepository;
import com.baracklee.mq.biz.entity.MqLockEntity;
import com.baracklee.mq.biz.service.MqLockService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqLockServiceImpl extends AbstractBaseService<MqLockEntity> implements MqLockService {

    private Logger log = LoggerFactory.getLogger(MqLockServiceImpl.class);
    private String ip;
    private String key = "soa_clean_sk";
    private volatile boolean flag = false;
    private volatile long id = 0;
    // 对象初始化完成
    private volatile boolean objInit = false;
    private volatile Object lockObj = new Object();
    private volatile boolean isMaster = false;
    // @Autowired
    private SoaConfig soaConfig;
    private MqLockRepository mqLockRepository;
    private DbService dbService;
    private EmailUtil emailUtil;
    private HeartbeatProperty heartbeatProperty;

    public MqLockServiceImpl(String key) {
        this.key = key;
    }

    public MqLockServiceImpl(MqLockRepository repository){
        this.mqLockRepository=repository;
        setBaseRepository(repository);
    }

    @Override
    public boolean isMaster() {
        if(!isLoad()) return false;
        init();
        boolean temp=checkMaster();
        if(temp!=isMaster)
        return false;
    }

    @Override
    public boolean updateHeatTime() {
        if(!isLoad()){
            return false;
        }
        return mqLockRepository.updateHeartTimeByIdAndIp(id, ip) > 0;
    }

    // 检查SoaLockRepository和SoaConfig是否注入
    private boolean isLoad() {
        if (objInit) {
            return true;
        }
        if (mqLockRepository == null) {
            mqLockRepository = SpringUtil.getBean(MqLockRepository.class);
            super.setBaseRepository(mqLockRepository);
        }
        if (soaConfig == null) {
            soaConfig = SpringUtil.getBean(SoaConfig.class);
        }
        if (dbService == null) {
            dbService = SpringUtil.getBean(DbService.class);
        }
        if (emailUtil == null) {
            emailUtil = SpringUtil.getBean(EmailUtil.class);
        }
        objInit = (mqLockRepository != null && soaConfig != null && dbService != null && emailUtil != null);
        return objInit;
    }


    @Override
    public boolean isInLock() {
        return false;
    }


    public void init(){

    }
}
