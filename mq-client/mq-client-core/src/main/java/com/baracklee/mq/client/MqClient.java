package com.baracklee.mq.client;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterResponse;
import com.baracklee.mq.biz.dto.client.ConsumerRegisterRequest;
import com.baracklee.mq.biz.dto.client.PublishMessageRequest;
import com.baracklee.mq.biz.event.PreHandleListener;
import com.baracklee.mq.client.config.ClientConfigHelper;
import com.baracklee.mq.client.config.ConsumerGroupVo;
import com.baracklee.mq.client.core.*;
import com.baracklee.mq.client.core.impl.MqMeticsReporterService;
import com.baracklee.mq.client.factory.IMqFactory;
import com.baracklee.mq.client.factory.MqFactory;
import com.baracklee.mq.client.resolver.ISubscriberResolver;
import com.baracklee.mq.client.resource.IMqResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqClient {
    private static Logger log = LoggerFactory.getLogger(MqClient.class);

    private static AtomicBoolean initFlag = new AtomicBoolean(false);

    private static MqContext mqContext = new MqContext();
    private static MqEnvironment mqEnvironment=null;
    private static Object lockObj = new Object();
    private static AtomicBoolean registerFlag = new AtomicBoolean(false);

    private static AtomicBoolean startFlag=new AtomicBoolean(false);

    private static ISubscriberResolver subscriberResolver;

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 5L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50), SoaThreadFactory.create("MqClient", true),
            new ThreadPoolExecutor.CallerRunsPolicy());
    public static MqContext getContext() {
        return mqContext;
    }
    private static BlockingQueue<PublishMessageRequest> msgsAsyn = null;


    public static MqEnvironment getMqEnvironment() {
        return mqEnvironment;
    }

    private static IMqFactory mqFactory = new MqFactory();

    private static IConsumerPollingService consumerPollingService = null;

    private static IMqBrokerUrlRefreshService mqBrokerUrlRefreshService;
    private static IMqHeartbeatService mqHeartbeatService;
    private static IMqCheckService mqCheckService;

    private static IMqCommitService mqCommitService;





    public static void setMqEnvironment(MqEnvironment mqEnvironment) {
        MqClient.mqEnvironment = mqEnvironment;
        getContext().setMqEnvironment(mqEnvironment);
    }

    public static ISubscriberResolver getSubscriberResolver() {
        return subscriberResolver;
    }

    public static void setSubscriberResolver(ISubscriberResolver subscriberResolver) {
        MqClient.subscriberResolver = subscriberResolver;
    }
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                close();
            }
        });
    }
    public static void registerPreHandleEvent(PreHandleListener preHandleListener1) {
        synchronized (lockObj) {
            getContext().getMqEvent().setPreHandleListener(preHandleListener1);
        }
    }

    public static boolean start() {
        if (startFlag.compareAndSet(false, true)) {
            registerConsumerGroup();
        }
        return false;
    }

    private static void registerConsumerGroup() {
        Map<String, ConsumerGroupVo> localConfig = new ClientConfigHelper(mqContext).getConfig();
        return registerConsumerGroup(localConfig);
    }

    public static boolean registerConsumerGroup(Map<String, ConsumerGroupVo> groups) {
        if(groups.isEmpty()) return false;
        if(hasInit()){
            log.info("已经初始化完成");
            return doRegisterConsumerGroup(groups);
        }else {
            log.warn("异步初始化系统");
            executor.execute(()->{
                try {
                    return doRegisterConsumerGroup(groups);
                } catch (Throwable e) {
                    log.error("registerConsumerGroup",e);
                }
            });
            return true;
        }
    }

    private static boolean doRegisterConsumerGroup(Map<String, ConsumerGroupVo> groups) {
        Map<String, List<String>> consumerGroupNames = new HashMap<>();
        String groupNames="";
        for (ConsumerGroupVo consumerGroup : groups.values()) {
            if (!checkVaild(consumerGroup)) {
                return false;
            }
            if (mqContext.getConsumerGroupVersion().containsKey(consumerGroup.getMeta().getName())) {
                log.info("ConsumerGroup:" + consumerGroup.getMeta().getName() + " has  subscribed,已订阅！");
                return false;
            }
            if (consumerGroup.getMeta().getOriginName().isEmpty()) {
                consumerGroup.getMeta().setOriginName(consumerGroup.getMeta().getName());
            }
            if (consumerGroup.getTopics() != null) {
                consumerGroupNames.put(consumerGroup.getMeta().getOriginName(), new ArrayList<>(consumerGroup.getTopics().keySet()));
            } else {
                consumerGroupNames.put(consumerGroup.getMeta().getOriginName(), new ArrayList<>());
            }
            groupNames+=consumerGroup.getMeta().getName()+",";
        }
            register();
            ConsumerGroupRegisterRequest request = new ConsumerGroupRegisterRequest();
            request.setConsumerGroupNames(consumerGroupNames);
            request.setConsumerId(mqContext.getConsumerId());
            request.setClientIp(mqContext.getConfig().getIp());
            request.setConsumerName(mqContext.getConsumerName());
            try {
                //http 调用注册消费者组
                ConsumerGroupRegisterResponse consumerGroupRegisterResponse
                        = mqContext.getMqResource().registerConsumerGroup(request);

                //如果返回成功
                if (consumerGroupRegisterResponse.isSuc()){
                    Map<String, String> broadcastConsumerGroupNames = consumerGroupRegisterResponse.getConsumerGroupNameNew();
                    //这里兼容广播模式的消息group 名字
                    for (ConsumerGroupVo consumerGroup : groups.values()) {
                        if(broadcastConsumerGroupNames!=null&&broadcastConsumerGroupNames.containsKey(consumerGroup.getMeta().getOriginName())){
                            consumerGroup.getMeta().setName(broadcastConsumerGroupNames.get(consumerGroup.getMeta().getOriginName()));
                        }
                        mqContext.getConfigConsumerGroup().put(consumerGroup.getMeta().getName(),consumerGroup);
                        mqContext.getConsumerGroupVersion().put(consumerGroup.getMeta().getName(),0L);
                        //注册成功的拦截器运行,可以加,为了代码精简,就没加了
//                        fireConsumerGroupRegisterEvent(consumerGroup);
                    }
                    consumerPollingService=mqFactory.createConsumerPollingService();
                    consumerPollingService.start();
                    mqBrokerUrlRefreshService=mqFactory.createMqBrokerUrlRefreshService();
                    mqBrokerUrlRefreshService.start();
                    mqCheckService = mqFactory.createMqCheckService();
                    mqCheckService.start();
                    mqCommitService=mqFactory.createCommitService();
                    mqCommitService.start();
                    log.info(groupNames + "  subscribe_suc,订阅成功！ and json is " + JsonUtil.toJson(request));
                }else {
                    throw new RuntimeException("registerConsumerGroup_error, the req is" + JsonUtil.toJsonNull(request)
                            + ",and resp is " + JsonUtil.toJson(consumerGroupRegisterResponse));
                }

        }catch (Exception e){
                log.error("consumer_group_register_error",e);
                throw new RuntimeException(e);
            }
            return true;
    }



    private static void register() {
        if(registerFlag.compareAndSet(false,true)){
            ConsumerRegisterRequest request = new ConsumerRegisterRequest();
            try {
                request.setName(mqContext.getConsumerName());
                request.setClientIp(mqContext.getConfig().getIp());
                mqContext.setConsumerId(mqContext.getMqResource().register(request));
                mqHeartbeatService = mqFactory.createMqHeartbeatService();
                mqHeartbeatService.start();
                log.info("ConsumerName:" + mqContext.getConsumerName() + " has registed,注册成功！consumerId 为"
                        + mqContext.getConsumerId());
                fireRegisterEvent();
            }catch (Exception e){
                registerFlag.set(false);
                log.error("register_error, 注册失败:",JsonUtil.toJson(request));
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 研究一下meta从哪里来的
     * @param localConfig
     * @return
     */
    private static boolean checkVaild(ConsumerGroupVo localConfig) {
        if (localConfig == null) {
            throw new IllegalArgumentException("ConsumerGroupVo can't be null,不能为空");
        }
        if (localConfig.getMeta() == null || Util.isEmpty(localConfig.getMeta().getName())) {
            throw new IllegalArgumentException("ConsumerGroupName can't be null,不能为空");
        }
        return true;
    }

    /**
     * 初始化上下文的配置文件
     * @return 配置成功与否
     */
    private static boolean hasInit() {
        boolean flag=initFlag.get();
        if(!flag&&getContext().getConfig()!=null&&!getContext().getConfig().getUrl().isEmpty()){
            synchronized (MqClient.class){
                init(getContext().getConfig());
                flag=initFlag.get();
            }
        }
        return flag;
    }

    private static void init(MqConfig config) {
        if(initFlag.compareAndSet(false,true)){
            doInit(config);
            fireInitEvent();
            log.info("mq_client has inited");
        }
    }

    public static IMqFactory getMqFactory() {
        return mqFactory;
    }

    public static void close(){
        try {
            doPublishAsyn();
            if (consumerPollingService != null) {
                consumerPollingService.close();
                consumerPollingService = null;
            }
            if(mqCommitService!=null){
                mqCommitService.close();
                mqCommitService = null;
            }
            // ConsumerPollingService.getInstance().close();
            deRegister();
            if (mqBrokerUrlRefreshService != null) {
                mqBrokerUrlRefreshService.close();
                mqBrokerUrlRefreshService = null;
            }
            // MqBrokerUrlRefreshService.getInstance().close();
            if (mqCheckService != null) {
                mqCheckService.close();
                mqCheckService = null;
            }
            // MqCheckService.getInstance().close();
            if (mqHeartbeatService != null) {
                mqHeartbeatService.close();
                mqHeartbeatService = null;
            }
            // MqHeartbeatService.getInstance().close();
            MqMeticsReporterService.getInstance().close();
            mqContext.clear();
            // initFlag.set(false);
            registerFlag.set(false);
            startFlag.set(false);
            // asynFlag.set(false);
            mqFactory = new MqFactory();
        } catch (Throwable e) {
            log.error("Mq_Client:",e);
        }
    }

    private static void doPublishAsyn() {
        long startTime=System.currentTimeMillis();
        while (msgsAsyn!=null&&!msgsAsyn.isEmpty()){
            PublishMessageRequest request=null;
            request=msgsAsyn.poll();
            if (request!=null){
                publish(request,mqContext.getConfig().getPbRetryTimes());
            }
        }
    }

    private static boolean publish(PublishMessageRequest request, int pbRetryTimes) {
        IMqResource resource = mqContext.getMqBakResource();
        return mqContext.getMqResource().publish(request,pbRetryTimes);
    }

}
