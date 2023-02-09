package com.baracklee.mq.client.core.impl.queueutils;

import com.baracklee.mq.client.dto.BatchRecordItem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BatchRecorder {
    public Map<Long, BatchRecordItem> getRecordMap() {
        return recordMap;
    }

    public void setRecordMap(Map<Long, BatchRecordItem> recordMap) {
        this.recordMap = recordMap;
    }

    Map<Long, BatchRecordItem> recordMap=new ConcurrentHashMap<>();
    //记录最小的线程编号
    private volatile long start=0L;
    //记录当前开启的线程编号
    private volatile AtomicLong current = new AtomicLong(0);

    public long begin(int threadCount){
        long current1=current.incrementAndGet();
        BatchRecordItem batchRecordItem = new BatchRecordItem();
        batchRecordItem.setBatchReacorderId(current1);
        batchRecordItem.setThreadCount(threadCount);
        batchRecordItem.setBatchFinished(false);
        recordMap.put(current1,batchRecordItem);
        return current1;
    }

    //结束某个线程,同时返回最大连续执行线程批次的id
    public BatchRecordItem end(long batchReacorderId, long maxId) {
        BatchRecordItem finishedItem = recordMap.get(batchReacorderId);
        if (finishedItem == null) {
            return null;
        }
        int count = finishedItem.getCounter().incrementAndGet();
        if (finishedItem.getMaxId() < maxId) {
            finishedItem.setMaxId(maxId);
        }
        if (!finishedItem.isBatchFinished()) {
            finishedItem.setBatchFinished(count == finishedItem.getThreadCount());
        }
        if (finishedItem.isBatchFinished()) {
            BatchRecordItem rs = getLastestItem();
            return rs;
        }
        return null;
    }

    // 获取最大的连续执行线程批次
    public BatchRecordItem getLastestItem() {
        BatchRecordItem finishedItemPre = null;
        long current1 = current.get();
        for (long i = start; i <= current1; i++) {
            BatchRecordItem finishedItem = recordMap.get(i);
            if (finishedItem == null) {
                continue;
            }
            if (!finishedItem.isBatchFinished()) {
                break;
            } else {
                finishedItemPre = finishedItem;
            }
        }
        return finishedItemPre;
    }

}
