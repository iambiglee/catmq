package com.baracklee.mq.client;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.JsonUtil;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterRequest;
import com.baracklee.mq.biz.dto.client.ConsumerGroupRegisterResponse;
import com.baracklee.mq.biz.event.PreHandleListener;
import com.baracklee.mq.client.config.ClientConfigHelper;
import com.baracklee.mq.client.config.ConsumerGroupVo;
import com.baracklee.mq.client.core.IConsumerPollingService;
import com.baracklee.mq.client.factory.IMqFactory;
import com.baracklee.mq.client.factory.MqFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqClient {
    private static Logger log = LoggerFactory.getLogger(MqClient.class);

    private static AtomicBoolean initFlag = new AtomicBoolean(false);

    private static MqContext mqContext = new MqContext();
    private static MqEnvironment mqEnvironment=null;
    private static Object lockObj = new Object();

    private static AtomicBoolean startFlag=new AtomicBoolean(false);

    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 5L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50), SoaThreadFactory.create("MqClient", true),
            new ThreadPoolExecutor.CallerRunsPolicy());
    public static MqContext getContext() {
        return mqContext;
    }

    public static MqEnvironment getMqEnvironment() {
        return mqEnvironment;
    }

    private static IMqFactory mqFactory = new MqFactory();

    private static IConsumerPollingService consumerPollingService = null;


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
        }
            groupNames+=consumerGroup.getMeta().getName()+",";
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
                        //注册成功的拦截器运行
                        fireConsumerGroupRegisterEvent(consumerGroup);
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
}
