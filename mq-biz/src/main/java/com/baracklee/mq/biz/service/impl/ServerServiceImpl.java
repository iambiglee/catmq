package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.BrokerTimerService;
import com.baracklee.mq.biz.common.inf.PortalTimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.IPUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.meta.ServerRepository;
import com.baracklee.mq.biz.entity.AuditLogEntity;
import com.baracklee.mq.biz.entity.ServerEntity;
import com.baracklee.mq.biz.service.AuditLogService;
import com.baracklee.mq.biz.service.ServerService;
import com.baracklee.mq.biz.service.common.AbstractBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.spi.ServiceRegistry;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


@Service
public class ServerServiceImpl extends AbstractBaseService<ServerEntity>
		implements ServerService, BrokerTimerService, PortalTimerService {

    private Environment environment;
    private SoaConfig soaConfig;

    private AuditLogService auditLogService;

    private ServerRepository serverRepository;

    @Autowired
    public ServerServiceImpl(Environment environment,
                             SoaConfig soaConfig,
                             AuditLogService auditLogService,
                             ServerRepository serverRepository){
        this.environment=environment;
        this.auditLogService=auditLogService;
        this.soaConfig=soaConfig;
        this.serverRepository=serverRepository;
    }
    private volatile boolean isRunning = true;
    private volatile long id = 0;
    private volatile boolean isBroker = false;
    private AtomicBoolean startFlag = new AtomicBoolean(false);
    private ThreadPoolExecutor executor = null;
    private AtomicReference<List<String>> cacheBrokerDataMap = new AtomicReference<>(new ArrayList<>());
    private AtomicReference<List<String>> cachePortalDataMap = new AtomicReference<>(new ArrayList<>());
    private volatile int onlineServerCount = 0;

    @PostConstruct
    private void init() {
        super.setBaseRepository(serverRepository);
    }


    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Override
    public List<String> getBrokerUrlCache() {
        return cacheBrokerDataMap.get();
    }

    @Override
    public int getOnlineServerNum() {
        return onlineServerCount;
    }

    @Override
    public void batchUpdate(List<Long> serverIds, int serverStatus) {
        serverRepository.batchUpdate(serverIds,serverStatus);
    }

    private  String getServerVersion() {
        try {
            return environment.getProperty("mq.broker.version",Util.formateDate(new Date(), "yyyyMMdd"));
        } catch (Exception e) {
            return Util.formateDate(new Date(), "yyyyMMdd");
        }
    }


    @Override
    public void startBroker() {
        isBroker=true;
        heartbeatAndUpdate();
    }

    private void heartbeatAndUpdate() {
        if (startFlag.compareAndSet(false, true)) {
            executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(50),
                    SoaThreadFactory.create("ServerServiceImpl", true), new ThreadPoolExecutor.DiscardOldestPolicy());
            updateHeartBeat();
            updateCache();
            executor.execute(() -> {
                while (isRunning) {
                    try {
                        updateHeartBeat();
                        updateCache();
                    } catch (Throwable e) {
                        log.error("ServerServiceImpl_updateHeartBeat_error", e);
                    }
                    Util.sleep(soaConfig.getServerHeartbeat() * 1000);
                }
            });
        }
    }

    private void updateCache() {
        List<String> brokerIps = new ArrayList<>();
        List<String> portalIps = new ArrayList<>();
        int count=0;


        List<ServerEntity> lst = serverRepository.getNoramlServer(soaConfig.getServerHeartbeat() + 10);

        for (ServerEntity server : lst) {
            if(server.getServerType()==1&&server.getStatusFlag()==1){
                brokerIps.add("http://" + server.getIp() + ":" + server.getPort());
                count++;
            }else if(server.getServerType()==0){
                portalIps.add("http://" + server.getIp() + ":" + server.getPort());
            }
        }
        onlineServerCount = count;
        cacheBrokerDataMap.set(brokerIps);
        cachePortalDataMap.set(portalIps);
    }

    private void updateHeartBeat() {
        if (isBroker) {
            if (serverRepository.updateHeartTimeById(id) <= 0) {
                id = 0;
                doInitData();
            }
        } else {
            if (environment.getProperty("server.delete", "1").equals("1")) {
                try {
                    int count = serverRepository.deleteOld(soaConfig.getServerExpireTime());
                    if (count > 0) {
                        AuditLogEntity auditLog = new AuditLogEntity();
                        auditLog.setTbName(ServerEntity.TABLE_NAME);
                        auditLog.setRefId(0);
                        auditLog.setInsertBy(IPUtil.getLocalIP());
                        auditLog.setContent("deleted instance,count is " + count + ",expiretime config is "
                                + soaConfig.getServerExpireTime());
                        auditLogService.insert(auditLog);
                        log.info(auditLog.getContent());
                    }
                } catch (Exception e) {
                }
            }
        }

    }

    private void doInitData() {
        if (id <= 0) {
            ServerEntity serverEntity = new ServerEntity();
            try {
                serverEntity.setIp(IPUtil.getLocalIP(environment.getProperty("mq.broker.netCard")));
                serverEntity.setPort(Integer.parseInt(environment.getProperty("server.port", "8080")));
                serverEntity.setHeartTime(new Date());
                serverEntity.setServerType(isBroker?1:0);
                serverEntity.setServerVersion(getServerVersion());
                serverEntity.setStatusFlag(0);
                serverRepository.insert1(serverEntity);
                id = serverEntity.getId();
            } catch (Exception e) {
                try {
                    Map<String, Object> conditionMap = new HashMap<>();
                    conditionMap.put(ServerEntity.FdIp, serverEntity.getIp());
                    conditionMap.put(ServerEntity.FdPort, serverEntity.getPort());
                    serverEntity = serverRepository.get(conditionMap);
                    if (serverEntity != null) {
                        id = serverEntity.getId();
                        serverEntity.setServerType(isBroker?1:0);
                        serverEntity.setServerVersion(getServerVersion());
                        serverRepository.update(serverEntity);
                    }
                } catch (Exception e1) {

                }
            }
        }
    }

    @Override
    public void stopBroker() {
        try {
            executor.shutdown();
            isRunning = false;
            id = 0;
        } catch (Throwable e) {
        }

    }

    @Override
    public void startPortal() {
        heartbeatAndUpdate();
    }

    @Override
    public void stopPortal() {
        stopBroker();
    }

    @Override
    public String info() {
        return null;
    }
}
