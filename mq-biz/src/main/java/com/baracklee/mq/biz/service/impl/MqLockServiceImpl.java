package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.util.IPUtil;
import com.baracklee.mq.biz.common.util.SpringUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.MqLockRepository;
import com.baracklee.mq.biz.entity.MqLockEntity;
import com.baracklee.mq.biz.service.DbService;
import com.baracklee.mq.biz.service.MqLockService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


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
    private HeartbeatProperty heartbeatProperty;


    public MqLockServiceImpl(MqLockRepository repository){
        this.mqLockRepository=repository;
        setBaseRepository(repository);
    }

    public MqLockServiceImpl(String key) {
        this.key = key;
        this.heartbeatProperty = new HeartbeatProperty() {
            @Override
            public int getValue() {
                return soaConfig.getMqLockHeartBeatTime();
            }
        };
    }

    @Override
    public boolean isMaster() {
        if(!isLoad()) return false;
        init();
        boolean temp=checkMaster();
        if(temp!=isMaster){
            isMaster=temp;
            if(temp) log.warn("ip_{}_key{} 获取到锁",ip,key);
            else log.warn("ip_{}_key{} 失去锁",ip,key);
            isMaster=temp;
            return isMaster;
        }
        return false;
    }

    private boolean checkMaster() {
        if(isInLock()){
            return doCheckMaster();
        }else {
            return false;
        }
    }

    private boolean doCheckMaster() {
        Map<String,Object> mapCond=new HashMap<>();
        mapCond.put(MqLockEntity.FdKey1,key);
        MqLockEntity mqLockEntity = mqLockRepository.get(mapCond);
        if(mqLockEntity==null){
            clearAndInit();
            mqLockEntity=mqLockRepository.get(mapCond);
        }
        Date dbNow = dbService.getDbTime();
        id=mqLockEntity.getId();
        if(mqLockEntity.getHeartTime().getTime()<dbNow.getTime()-getExpired()*1000){
            int count=mqLockRepository.updateHeartTimeByKey1(ip,key,getExpired());
            boolean flag1=count>0;
            return flag1;
        }else {
            return checkMaster(mqLockEntity,dbNow);
        }

    }

    private int getExpired() {
        return getHeartBeatTime() * 2 + 3;
    }

    private int getHeartBeatTime() {
        try {
            return this.heartbeatProperty.getValue();
        } catch (Exception e) {
            return soaConfig.getMqLockHeartBeatTime();
        }
    }

    private void clearAndInit() {
        Map<String,Object> mapCond=new HashMap<>();
        mapCond.put(MqLockEntity.FdKey1,key);
        MqLockEntity mqLockEntity = mqLockRepository.get(mapCond);
        if(mqLockEntity==null){
            insert();
        }
    }

    public void insert(){
        MqLockEntity entity = new MqLockEntity();
        entity.setIp(ip);
        entity.setKey1(key);
        mqLockRepository.insert1(entity);
    }
    private boolean checkMaster(MqLockEntity mqLockEntity, Date dbNow) {
        boolean flag1=mqLockEntity.getIp().equals(ip);
        return flag1;
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
        objInit = (mqLockRepository != null && soaConfig != null && dbService != null);
        return objInit;
    }


    @Override
    public boolean isInLock() {
        return false;
    }


    public void init(){
        if(!flag){
            synchronized (lockObj){
                if(!flag){
                    flag=true;
                    ip = String.format("%s_%s_%s", IPUtil.getLocalIP().replaceAll("\\.", "_"), Util.getProcessId(),
                            System.currentTimeMillis() % 10000);
                    if(!clearOld()){
                        Util.sleep(getExpired() * 1000);
                    }
                    clearAndInit();
                }
            }
        }
    }

    private boolean clearOld() {
        return mqLockRepository.deleteOld(key, getExpired()) > 0;
    }

    public interface HeartbeatProperty{
        int getValue();
    }

}
