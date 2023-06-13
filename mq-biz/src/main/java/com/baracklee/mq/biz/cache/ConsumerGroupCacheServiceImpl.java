package com.baracklee.mq.biz.cache;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.common.inf.ConsumerGroupChangedListener;
import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dto.base.ConsumerGroupDto;
import com.baracklee.mq.biz.dto.base.ConsumerGroupMetaDto;
import com.baracklee.mq.biz.dto.base.ConsumerQueueDto;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.ConsumerGroupTopicEntity;
import com.baracklee.mq.biz.entity.QueueOffsetEntity;
import com.baracklee.mq.biz.service.ConsumerGroupService;
import com.baracklee.mq.biz.service.DbService;
import com.baracklee.mq.biz.service.NotifyMessageService;
import com.baracklee.mq.biz.service.QueueOffsetService;
import com.codahale.metrics.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 提供一个 client 获取数据的接口的工厂对象
 * 主要是返回consumerGroup topic 和 queue offset
 * 以 consumerGroupName 为key, 获取一个自定义的对象
 */
@Service
public class ConsumerGroupCacheServiceImpl implements ConsumerGroupCacheService {
    private Logger log= LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private volatile long lastMaxId=0;

    private volatile boolean stop = true;

    private volatile long currentMaxId =0;


    private NotifyMessageService notifyMessageService;

    private ConsumerGroupService consumerGroupService;

    private QueueOffsetService queueOffsetService;

    private DbService dbService;

    private SoaConfig soaConfig;


    @Autowired
    public ConsumerGroupCacheServiceImpl(NotifyMessageService notifyMessageService,
                                         ConsumerGroupService consumerGroupService,
                                         QueueOffsetService queueOffsetService,
                                         DbService dbService,
                                         SoaConfig soaConfig) {
        this.notifyMessageService = notifyMessageService;
        this.consumerGroupService = consumerGroupService;
        this.queueOffsetService = queueOffsetService;
        this.dbService = dbService;
        this.soaConfig = soaConfig;
    }


    //核心工厂类
    private AtomicReference<Map<String, ConsumerGroupDto>> consumerGroupRefMap = new AtomicReference<>(
            new ConcurrentHashMap<>());


    private volatile Date lastDate = new Date();

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 5, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(100), SoaThreadFactory.create("ConsumerGroupCacheService", true),
            new ThreadPoolExecutor.DiscardOldestPolicy());



    private List<ConsumerGroupChangedListener> listListener = new ArrayList<>();

    private Counter initConsumerGroupCounter = null;
    private final Counter pollingCounter = new Counter();

    @Override
    public synchronized void addListener(ConsumerGroupChangedListener listener) {
        if (!listListener.contains(listener)) {
            listListener.add(listener);
        }
    }



    @Override
    public void startBroker() {
        if (stop) {
            stop = false;
            lastMaxId = notifyMessageService.getDataMaxId();
            try {
                initData();
            } catch (Exception e) {
                log.error("ConsumerGroupCacheService_error,异常", e);
                throw e;
            }
            lastDate = new Date();
            executor.execute(() -> {
                checkPollingData();
            });
        }
    }
    private synchronized void initData() {
        Map<String, ConsumerGroupDto> dataMap = doInitData();

        if (dataMap.size() > 0) {
            consumerGroupRefMap.set(dataMap);
        }
        log.info("ConsumerGroup_init_suc, 初始化完成！");
//        initConsumerGroupCounter.inc();
    }

    private Map<String, ConsumerGroupDto> doInitData() {
        List<ConsumerGroupEntity> consumerGroupEntities = consumerGroupService.getList();
        List<QueueOffsetEntity> queueOffsetEntities = queueOffsetService.getAllBasic();
        List<ConsumerGroupTopicEntity> consumerGroupTopicEntities = consumerGroupService.getGroupTopic();
        Map<Long, ConsumerGroupVo> consumerMap = new HashMap<>(consumerGroupEntities.size());
        consumerGroupEntities.forEach(t1 -> {
            consumerMap.put(t1.getId(), new ConsumerGroupVo(t1));
        });
        consumerGroupTopicEntities.forEach(t1 -> {
            if (consumerMap.containsKey(t1.getConsumerGroupId())) {
                consumerMap.get(t1.getConsumerGroupId()).topics.put(t1.getTopicId(), t1);
            }
        });
        queueOffsetEntities.forEach(t1 -> {
            if (!StringUtils.isEmpty(t1.getConsumerName())) {
                if (consumerMap.containsKey(t1.getConsumerGroupId())) {
                    consumerMap.get(t1.getConsumerGroupId()).queueOffsets.add(t1);
                }
            }
        });
        return convertResult(consumerMap);
    }

    private Map<String, ConsumerGroupDto> convertResult(Map<Long, ConsumerGroupVo> consumerMap) {
        Map<String, ConsumerGroupDto> dataMap = new ConcurrentHashMap<>(consumerMap.size());
        consumerMap.values().forEach(t1 -> {
            ConsumerGroupEntity t2 = t1.consumerGroup;
            ConsumerGroupDto consumerGroupDto = new ConsumerGroupDto();
            dataMap.put(t2.getName(), consumerGroupDto);
            ConsumerGroupMetaDto consumerGroupMeta = new ConsumerGroupMetaDto();
            consumerGroupMeta.setMetaVersion(t2.getMetaVersion());
            consumerGroupMeta.setName(t2.getName());
            consumerGroupMeta.setRbVersion(t2.getRbVersion());
            consumerGroupMeta.setVersion(t2.getVersion());
            consumerGroupDto.setMeta(consumerGroupMeta);
            // key为consumerid，里面的key为topicid
            Map<Long, Map<Long, ConsumerQueueDto>> consumers = new HashMap<>();
            consumerGroupDto.setConsumers(consumers);
            t1.queueOffsets.forEach(t3 -> {
                if (!StringUtils.isEmpty(t3.getOriginTopicName())) {
                    if (!consumers.containsKey(t3.getConsumerId())) {
                        // consumers.putIfAbsent(t3.getConsumerId(), new HashMap<>());
                        consumers.put(t3.getConsumerId(), new HashMap<>());
                    }

                    ConsumerQueueDto consumerQueueDto = getConsumerQueue(t1, t2, t3);
                    consumers.get(t3.getConsumerId()).put(consumerQueueDto.getQueueId(), consumerQueueDto);
                } else {
                    log.error("OriginTopicName_is_empty_queueOffsetId_{}", t3.getId());
                }
            });
        });
        return dataMap;
    }


    private ConsumerQueueDto getConsumerQueue(ConsumerGroupVo t2, ConsumerGroupEntity t1, QueueOffsetEntity t3) {
        ConsumerQueueDto consumerQueueDto = new ConsumerQueueDto();
        consumerQueueDto.setOffsetVersion(t3.getOffsetVersion());
        consumerQueueDto.setQueueId(t3.getQueueId());
        consumerQueueDto.setQueueOffsetId(t3.getId());
        consumerQueueDto.setTopicId(t3.getTopicId());
        consumerQueueDto.setTopicName(t3.getTopicName());
        consumerQueueDto.setStopFlag(t3.getStopFlag());
        consumerQueueDto.setTopicType(t3.getTopicType());
        consumerQueueDto.setOriginTopicName(t3.getOriginTopicName());
        consumerQueueDto.setTraceFlag(t1.getTraceFlag());
        consumerQueueDto.setConsumerGroupName(t1.getName());
        consumerQueueDto.setTopicName(t3.getTopicName());
        consumerQueueDto.setOffset(t3.getOffset());
        ConsumerGroupTopicEntity temp = t2.topics.get(t3.getTopicId());
        if (temp != null) {
            consumerQueueDto.setDelayProcessTime(temp.getDelayProcessTime());
            consumerQueueDto.setThreadSize(temp.getThreadSize());
            consumerQueueDto.setPullBatchSize(temp.getPullBatchSize());
            consumerQueueDto.setConsumerBatchSize(temp.getConsumerBatchSize());
            consumerQueueDto.setRetryCount(temp.getRetryCount());
            consumerQueueDto.setTag(temp.getTag());
            consumerQueueDto.setMaxPullTime(temp.getMaxPullTime());
            consumerQueueDto.setTimeout(temp.getTimeOut());
        } else {
            // 大部分情况不会调用此代码，防止极端情况
            consumerQueueDto.setDelayProcessTime(0);
            consumerQueueDto.setThreadSize(10);
            consumerQueueDto.setConsumerBatchSize(1);
            consumerQueueDto.setPullBatchSize(50);
            consumerQueueDto.setRetryCount(10);
            consumerQueueDto.setTag(null);
            consumerQueueDto.setMaxPullTime(5);
            consumerQueueDto.setTimeout(0);
        }
        return consumerQueueDto;
    }

    private void checkPollingData() {
        while (!stop) {
            doCheckPollingData();
            Util.sleep(soaConfig.getCheckPollingDataInterval());
        }
    }

    private void doCheckPollingData() {
        try {
            if (!reInit()) {
                currentMaxId = notifyMessageService.getDataMaxId(lastMaxId);
                if (currentMaxId > 0 && currentMaxId > lastMaxId) {
                    updateCache();
                    lastMaxId = currentMaxId;
                }
            }
        } catch (Throwable e) {
            log.error("doCheckPollingData_error,更新异常", e);
        }
    }

    private void updateCache() {
        List<ConsumerGroupEntity> consumerGroupEntities = new ArrayList<>();
        try {
            pollingCounter.inc();
            consumerGroupEntities = consumerGroupService.getLastMetaConsumerGroup(lastMaxId, currentMaxId);
        } catch (Exception e) {
            log.error("getLastMetaConsumerGroup_error", e);
        }
        // 更新缓存
        updateCacheData(consumerGroupEntities);
        executor.submit(() -> {
            fireListener();
        });

    }

    private void updateCacheData(List<ConsumerGroupEntity> consumerGroupEntities) {
        Map<String, ConsumerGroupDto> dataMap = new HashMap<>();
        if (consumerGroupEntities.size() > 10) {
            dataMap = doInitData();
        } else {
            List<Long> ids = new ArrayList<>(consumerGroupEntities.size());
            consumerGroupEntities.forEach(t1 -> {
                ids.add(t1.getId());
            });

            List<QueueOffsetEntity> queueOffsetEntities = queueOffsetService.getByConsumerGroupIds(ids);
            List<ConsumerGroupTopicEntity> consumerGroupTopicEntities = consumerGroupService.getGroupTopic();
            Map<Long, ConsumerGroupVo> consumerMap = new HashMap<>(consumerGroupEntities.size());
            consumerGroupEntities.forEach(t1 -> {
                consumerMap.put(t1.getId(), new ConsumerGroupVo(t1));
            });
            consumerGroupTopicEntities.forEach(t1 -> {
                if (consumerMap.containsKey(t1.getConsumerGroupId())) {
                    consumerMap.get(t1.getConsumerGroupId()).topics.put(t1.getTopicId(), t1);
                }
            });
            queueOffsetEntities.forEach(t1 -> {
                if (!StringUtils.isEmpty(t1.getConsumerName())) {
                    if (consumerMap.containsKey(t1.getConsumerGroupId())) {
                        consumerMap.get(t1.getConsumerGroupId()).queueOffsets.add(t1);
                    }
                }
            });
            dataMap = convertResult(consumerMap);
        }
        Map<String, ConsumerGroupDto> cache = consumerGroupRefMap.get();
        dataMap.entrySet().forEach(t1 -> {
            if (!cache.containsKey(t1.getKey())) {
                cache.put(t1.getKey(), t1.getValue());
            } else {
                if (cache.get(t1.getKey()).getMeta() != null
                        && cache.get(t1.getKey()).getMeta().getVersion() < t1.getValue().getMeta().getVersion()) {
                    cache.put(t1.getKey(), t1.getValue());
                }
            }
        });

    }

    private boolean reInit() throws Exception {
        // 为了保险起见过30秒重新构建一次
        if (soaConfig.isEnableRebuild()
                && System.currentTimeMillis() - soaConfig.getReinitInterval() * 1000L > lastDate.getTime()) {
            currentMaxId = lastMaxId = notifyMessageService.getDataMaxId();
            try {
                initData();
                executor.execute(() -> {
                    fireListener();
                });
                log.info("重新初始化数据");
            } catch (Exception e) {
                log.error("ConsumerGroupCacheService_reint_error,异常", e);
                throw e;
            }
            lastDate = new Date();
            // continue;
            return true;
        }
        return false;
    }

    @Override
    public void stopBroker() {
        stop=true;
        executor.shutdown();
        executor=null;
    }

    @Override
    public String info() {
        return null;
    }

    @PreDestroy
    private void close(){
        stopBroker();
    }

    @Override
    public Map<String,ConsumerGroupDto> getCache(){
        return consumerGroupRefMap.get();
    }

    private void fireListener() {
        for (ConsumerGroupChangedListener listener : listListener) {
            try {
                listener.onChange();
            } catch (Exception e) {
            }
        }
    }

    class ConsumerGroupVo {
        public ConsumerGroupEntity consumerGroup;
        // key为topicid
        public Map<Long, ConsumerGroupTopicEntity> topics = new HashMap<>();
        public List<QueueOffsetEntity> queueOffsets = new ArrayList<>();

        public ConsumerGroupVo(ConsumerGroupEntity consumerGroup) {
            this.consumerGroup = consumerGroup;
        }
    }

}
