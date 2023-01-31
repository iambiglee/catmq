package com.baracklee.mq.client.bootStrap;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.IPUtil;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.client.MqClient;
import com.baracklee.mq.client.MqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqClientStartup {
    private static final Logger logger = LoggerFactory.getLogger(MqClientStartup.class);
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100), SoaThreadFactory.create("mqconfig-scan", true),
            new ThreadPoolExecutor.DiscardPolicy());

    private static Environment env;

    private static  volatile boolean isRunning=true;

    protected static AtomicBoolean initFlag = new AtomicBoolean(false);
    private static AtomicBoolean startFlag = new AtomicBoolean(false);

    public static void springInitComplete() {
        MqClient.start();
        monitorConfig();
    }

    public static void init(Environment env1) {
        if (initFlag.compareAndSet(false, true)) {
            env = env1;
            initConfig();
        }
    }

    private static void initConfig() {
        MqConfig config = new MqConfig();
        String netCard = System.getProperty("mq.network.netCard", env.getProperty("mq.network.netCard", ""));
        String url =System.getProperty("mq.broker.url", env.getProperty("mq.broker.url", ""));
        String host = System.getProperty("mq.client.host", env.getProperty("mq.client.host", ""));
        String serverPort = System.getProperty("server.port", env.getProperty("server.port", "8080"));
        String asynCapacity = System.getProperty("mq.asyn.capacity", env.getProperty("mq.asyn.capacity", "2000"));
        String rbTimes = System.getProperty("mq.rb.times", env.getProperty("mq.rb.times", "4"));
        String pbRetryTimes = System.getProperty("mq.pb.retry.times", env.getProperty("mq.pb.retry.times", "10"));
        String readTimeOut = System.getProperty("mq.http.timeout", env.getProperty("mq.http.timeout", "10000"));
        String pullDeltaTime = System.getProperty("mq.pull.time.delta", env.getProperty("mq.pull.time.delta", "150"));
        String timeOutWarn=System.getProperty("mq.msg.warn.timeout", env.getProperty("mq.msg.warn.timeout", "300"));

        boolean metaMode = "true"
                .equals(System.getProperty("mq.broker.metaMode", env.getProperty("mq.broker.metaMode", "true")));
        if(!Util.isEmpty(netCard)){
            logger.warn("请注意你指定了网卡名称mq.network.netCard="+netCard);
        }
        if (Util.isEmpty(host)) {
            host = IPUtil.getLocalIP(netCard);
            logger.info("自动获取当前的ip地址是"+host);
        }else{
            logger.info("当前配置生效的机器ip地址是：mq.client.host="+host);
        }

        if (Util.isEmpty(url)) {
            throw new RuntimeException("没有配置broker地址。");
        }
        config.setIp(host);
        config.setMetaMode(metaMode);
        config.setServerPort(serverPort);
        config.setUrl(url);
        config.setAsynCapacity(Integer.parseInt(asynCapacity));
        config.setRbTimes(Math.max(Integer.parseInt(rbTimes), 4));
        config.setPbRetryTimes(Math.max(Integer.parseInt(pbRetryTimes),2));
        config.setPullDeltaTime(Integer.parseInt(pullDeltaTime));
        config.setReadTimeOut(Long.parseLong(readTimeOut));
        config.setWarnTimeout(Integer.parseInt(timeOutWarn));
        logger.info("当前生效的配置是："+ JsonUtil.toJsonNull(config));
        MqClient.init(config);
        updateConfig();
    }
    public static void close() {
        isRunning = false;
    }


    protected static String mqLogOrig = "-2";
    protected static String rbTimes = "4";
    protected static String pbTimes = "0";
    protected static String metaMode = "-2";
    protected static String asynCapacity = "-2";
    protected static String pullDeltaTime1 = "150";
    protected static String subEnvs1 = " ";
    protected static String publishAsynTimeout1="1000";
    protected static Map<String, String> properties = null;
    protected static String warnTimeout1="300";
    protected static String commitFlag1 = "0";
    protected static String commitInterval1 = "2000";

    private static void monitorConfig() {
        executor.execute(()->{
            while (isRunning){
                updateConfig();
                Util.sleep(2000);
            }
        });
    }

    protected static void  updateConfig(){
        if(properties==null){
            properties = MqClient.getContext().getConfig().getProperties();
        }
        setRbTimes();

        setPbTimes();


        setMetaMode();

        setPullDeltaTime();
        setWarnTimeout();
        //setAppSubEnvs();
        setCommit();
        setPublishAsynTimeout();
    }

    private static void setRbTimes() {
        String timesTemp = System.getProperty("mq.rb.times", env.getProperty("mq.rb.times", "4"));
        properties.put("mq.rb.times", timesTemp);
        if (Integer.parseInt(timesTemp)<4) {return;}
        if(!timesTemp.equals(rbTimes)){
            rbTimes=timesTemp;
            MqClient.getContext().getConfig().setRbTimes(Integer.parseInt(timesTemp));
        }
    }
    private static void setPbTimes() {
        try {
            String timesTemp = System.getProperty("mq.pb.retry.times", env.getProperty("mq.pb.retry.times", "5"));
            properties.put("mq.pb.retry.times", timesTemp);
            if (!timesTemp.equals(pbTimes)) {
                pbTimes = timesTemp;
                int times1 = Math.max(Integer.parseInt(timesTemp),2);
                if (MqClient.getContext() != null && MqClient.getContext().getConfig().getPbRetryTimes() != times1) {
                    MqClient.getContext().getConfig().setPbRetryTimes(times1);
                }
            }
        } catch (Exception e) {
            logger.error("setRbTimes_error", e);
        }

    }

}
