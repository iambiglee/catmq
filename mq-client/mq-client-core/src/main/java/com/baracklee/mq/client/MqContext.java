package com.baracklee.mq.client;

import com.baracklee.mq.biz.dto.base.ConsumerGroupOneDto;
import com.baracklee.mq.client.config.ConsumerGroupVo;
import com.baracklee.mq.client.event.MqEvent;
import com.baracklee.mq.client.resource.IMqResource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作为上下文队列客户端的上下文对象，记录所有的必要信息
 */
public class MqContext {

    private volatile long consumerId;

    // 获取记录broker的随机ip，防止出现通过域名访问时，host和dns访问错误
    private String brokerIp;

    private String consumerName;
    private String sdkVersion;

    // 此参数表示broker端模式
    private volatile int brokerMetaMode = 0;
    // 记录所有的配置，key为consumergroupName

    private Map<String, ConsumerGroupVo> configConsumerGroup = new ConcurrentHashMap<>();
    // key为consumerGroupName,value 为版本号
    private Map<String, Long> consumerGroupVersion = new ConcurrentHashMap<>();
    private Map<String, ConsumerGroupOneDto> consumerGroupMap = new ConcurrentHashMap<>();


    private String configPath;

    private volatile Set<String> appSubEnv = new HashSet<>();

    private transient IMqResource mqResource = null;

    private transient IMqResource mqHtResource = null;

    private transient IMqResource mqPollingResource = null;
    private transient IMqResource mqBakResource = null;

    private List<String> lstGroup1 = null, lstGroup2 = null;

    private volatile String metricUrl;
    // 标记是否记录原始数据
    private String bakUrl;

    private MqConfig config = new MqConfig();

    private MqEvent mqEvent = new MqEvent();
    private MqEnvironment mqEnvironment = null;

    public long getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(long consumerId) {
        this.consumerId = consumerId;
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public void setBrokerIp(String brokerIp) {
        this.brokerIp = brokerIp;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public int getBrokerMetaMode() {
        return brokerMetaMode;
    }

    public void setBrokerMetaMode(int brokerMetaMode) {
        this.brokerMetaMode = brokerMetaMode;
    }

    public Map<String, ConsumerGroupVo> getConfigConsumerGroup() {
        return configConsumerGroup;
    }

    public void setConfigConsumerGroup(Map<String, ConsumerGroupVo> configConsumerGroup) {
        this.configConsumerGroup = configConsumerGroup;
    }

    public Map<String, Long> getConsumerGroupVersion() {
        return consumerGroupVersion;
    }

    public void setConsumerGroupVersion(Map<String, Long> consumerGroupVersion) {
        this.consumerGroupVersion = consumerGroupVersion;
    }

    public Map<String, ConsumerGroupOneDto> getConsumerGroupMap() {
        return consumerGroupMap;
    }

    public void setConsumerGroupMap(Map<String, ConsumerGroupOneDto> consumerGroupMap) {
        this.consumerGroupMap = consumerGroupMap;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public Set<String> getAppSubEnv() {
        return appSubEnv;
    }

    public void setAppSubEnv(Set<String> appSubEnv) {
        this.appSubEnv = appSubEnv;
    }

    public IMqResource getMqResource() {
        return mqResource;
    }

    public void setMqResource(IMqResource mqResource) {
        this.mqResource = mqResource;
    }

    public IMqResource getMqHtResource() {
        return mqHtResource;
    }

    public void setMqHtResource(IMqResource mqHtResource) {
        this.mqHtResource = mqHtResource;
    }

    public IMqResource getMqPollingResource() {
        return mqPollingResource;
    }

    public void setMqPollingResource(IMqResource mqPollingResource) {
        this.mqPollingResource = mqPollingResource;
    }

    public IMqResource getMqBakResource() {
        return mqBakResource;
    }

    public void setMqBakResource(IMqResource mqBakResource) {
        this.mqBakResource = mqBakResource;
    }

    public List<String> getLstGroup1() {
        return lstGroup1;
    }

    public void setLstGroup1(List<String> lstGroup1) {
        this.lstGroup1 = lstGroup1;
    }

    public List<String> getLstGroup2() {
        return lstGroup2;
    }

    public void setLstGroup2(List<String> lstGroup2) {
        this.lstGroup2 = lstGroup2;
    }

    public String getMetricUrl() {
        return metricUrl;
    }

    public void setMetricUrl(String metricUrl) {
        this.metricUrl = metricUrl;
    }

    public String getBakUrl() {
        return bakUrl;
    }

    public void setBakUrl(String bakUrl) {
        this.bakUrl = bakUrl;
    }

    public MqConfig getConfig() {
        return config;
    }

    public void setConfig(MqConfig config) {
        this.config = config;
    }

    public MqEvent getMqEvent() {
        return mqEvent;
    }

    public void setMqEvent(MqEvent mqEvent) {
        this.mqEvent = mqEvent;
    }

    public MqEnvironment getMqEnvironment() {
        return mqEnvironment;
    }

    public void setMqEnvironment(MqEnvironment mqEnvironment) {
        this.mqEnvironment = mqEnvironment;
    }
    public void clear() {
        // configConsumerGroup = new ConcurrentHashMap<>();
        // key为consumerGroupName,value 为版本号
        consumerGroupVersion = new ConcurrentHashMap<>();
        // 此时是为了修正以备后用
        configConsumerGroup = new ConcurrentHashMap<>();
        consumerGroupMap = new ConcurrentHashMap<>();
    }
    public Map<String, ConsumerGroupVo> getOrignConfig() {
        Map<String, ConsumerGroupVo> configConsumerGroup1 = new ConcurrentHashMap<>();
        getConfigConsumerGroup().values().forEach(t1 -> {
            t1.getMeta().setName(t1.getMeta().getOriginName());
            configConsumerGroup1.put(t1.getMeta().getName(), t1);
        });
        return configConsumerGroup1;
    }
    // brokerUrls1 重要地址分组，brokerUrls2为非重要地址分组
    public void setBrokerUrls(List<String> brokerUrls1, List<String> brokerUrls2) {
        // 如果非重要节点列表为空，则将重要节点数据赋值给非重要节点
        if (brokerUrls2 == null || brokerUrls2.size() == 0) {
            brokerUrls2 = brokerUrls1;
        }
        if (mqHtResource != null) {
            mqHtResource.setUrls(brokerUrls1, brokerUrls2);
        }
        if (mqResource != null) {
            mqResource.setUrls(brokerUrls1, brokerUrls2);
        }
        if (mqPollingResource != null) {
            mqPollingResource.setUrls(brokerUrls1, brokerUrls2);
        }
        lstGroup1 = brokerUrls1;
        lstGroup2 = brokerUrls2;
    }

}
