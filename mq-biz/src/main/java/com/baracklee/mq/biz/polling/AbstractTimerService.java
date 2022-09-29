package com.baracklee.mq.biz.polling;

import com.baracklee.mq.biz.common.inf.PortalTimerService;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.trace.TraceFactory;
import com.baracklee.mq.biz.common.trace.TraceMessage;
import com.baracklee.mq.biz.common.util.SoaConfig;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.service.MqLockService;
import com.baracklee.mq.biz.service.impl.MqLockServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTimerService implements PortalTimerService {

    private final Logger log= LoggerFactory.getLogger(this.getClass());
    private String key="";
    private int interval=0;
    private SoaConfig soaConfig;
    private MqLockService mqLockService;
    private ThreadPoolExecutor executor=null;
    private TraceMessage traceMessage;
    private boolean isMaster=false;
    private boolean isRunning=false;
    private final Object lockObj=new Object();

    public void init(String key, int interval, SoaConfig soaConfig){
        this.key = key;
        mqLockService = new MqLockServiceImpl(key);
        executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(10),
                SoaThreadFactory.create(key, true), new ThreadPoolExecutor.DiscardOldestPolicy());
        this.interval = interval;
        this.soaConfig = soaConfig;
        this.traceMessage = TraceFactory.getInstance(key);
    }
    public void updateInterval(int interval){
        this.interval=interval;
    }

    public String info(){
        return String.format("timer key [%s],  master status is %s,enableTimer status is %s", key, isMaster,
                soaConfig.enableTimer());
    }

    @Override
    public void startPortal() {
        if(!isRunning){
            synchronized (lockObj){
                isRunning=true;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        work();
                    }
                });
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        doHeartbeat();
                    }
                });
            }
        }
    }

    private void doHeartbeat() {
        while (isRunning){
            isMaster=mqLockService.updateHeatTime();
        }
        Util.sleep(soaConfig.getMqLockHeartBeatTime() * 1000);
    }

    private void work() {
        while (isRunning){
            isMaster = mqLockService.isMaster();
            if(soaConfig.enableTimer()&&isMaster()){
                try {
                    log.info(key+"_work_start");
                    dostart();
                    log.info(key+"_work_end");
                }catch (Throwable e){
                    log.error(key+":_work_error",e);
                }
            }
            Util.sleep(1000);
        }
    }

    public boolean isMaster(){return mqLockService.isMaster();}

    public abstract void dostart();

    @Override
    public void stopPortal() {
        isRunning=false;
    }
}
