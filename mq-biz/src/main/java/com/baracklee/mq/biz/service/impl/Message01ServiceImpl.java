package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.msg.Message01Repository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.Message01Entity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TableInfoEntity;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.Message01Service;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class Message01ServiceImpl implements Message01Service {
    private final ThreadLocal<Long> dbId=new ThreadLocal<>();
    private final ThreadLocal<Boolean> isMaster=new ThreadLocal<>();
    private final AtomicInteger dbCounter=new AtomicInteger(0);
    private AtomicReference<Map<String, Map<String, Map<String, TableInfoEntity>>>> tbInfoRef = new AtomicReference<>(new HashMap<>());

    @Resource
    private Message01Repository message01Repository;

    @Resource
    DbNodeService dbNodeService;
    @Override
    public void setDbId(long dbNodeId) {
        dbId.set(dbNodeId);
        isMaster.set(false);
        dbCounter.incrementAndGet();
    }

    @Override
    public Message01Entity getMaxIdMsg(String tbName) {
        try {
            setMaster(true);
            return message01Repository.getMaxIdMsg(getDbName() + "." + tbName);
        } finally {
            clearDbId();
        }
    }

    @Override
    public String getDbName() {
        Map<Long, DbNodeEntity> cache = dbNodeService.getCache();
        Long id = dbId.get();
        if(cache.containsKey(id)){
            return cache.get(id).getDbName();
        }
        return null;
    }

    @Override
    public String getMaxConnectionsCount() {
        return null;
    }

    @Override
    public Integer getConnectionsCount() {
        return null;
    }

    @Override
    public void updateFailMsgResult(String tbName, List<Long> ids, int retryCount) {

    }

    @Override
    public int deleteOldFailMsg(String tbName, long id, int retryCount) {
        return 0;
    }

    @Override
    public long getNextId(String tbName, long id, int size) {
        return 0;
    }

    @Override
    public TableInfoEntity getSingleTableInfoFromCache(QueueEntity queueEntity) {
        return null;
    }

    @Override
    public Message01Entity getMinIdMsg(String tbName) {
        return null;
    }

    @Override
    public void insertBatchDy(String topic, String tbName, List<Message01Entity> entities) {

    }

    @Override
    public List<Message01Entity> getListDy(String topic, String tbName, long start, long end) {
        return null;
    }

    @Override
    public List<Message01Entity> getListByPage(Map<String, Object> parameterMap) {
        return null;
    }

    @Override
    public Long getTableMinId(String tbName) {
        return null;
    }

    @Override
    public long countByPage(Map<String, Object> parameterMap) {
        return 0;
    }

    @Override
    public Message01Entity getMessageById(String tbName, long id) {
        return null;
    }

    @Override
    public List<Message01Entity> getMessageByIds(String tbName, List<Long> ids) {
        return null;
    }

    @Override
    public Message01Entity getNearByMessageById(String tbName, long id) {
        return null;
    }

    @Override
    public int deleteDy(String tbName, long nextId, String date, int size, long maxId) {
        return 0;
    }

    @Override
    public void deleteByIds(String tbName, List<Long> ids) {

    }

    //最大值为当前最大值的下个一值
    @Override
    public Long getMaxId(String tbName) {
        setMaster(true);
        Long maxId;
        try {
            maxId = message01Repository.getMaxId(getDbName(), tbName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (maxId==null) return 1L;
       return maxId;

    }

    @Override
    public DataSource getDataSource() {
        return null;
    }

    /**
     * 获取所有的表格, 同一个schema的发到一个map中返回, map(schema(tablename, maxid))
     * @param ip ip address
     * @return table map
     */
    @Override
    public Map<String, Map<String, Long>> getMaxIdByIp(String ip) {
        setMaster(true);
        Map<String, Map<String, Long>> map = new HashMap<>();
        Map<String, Map<String, TableInfoEntity>> dbMap = new HashMap<>();
        if (getDataSource()==null) return map;
        List<TableInfoEntity> dataLst = message01Repository.getMaxIdByDb();
        Map<String, Map<String, TableInfoEntity>> finalDbMap = dbMap;
        for (TableInfoEntity entity : dataLst) {
            if (!map.containsKey(entity.getDbName())){
                map.put(entity.getDbName(),new HashMap<>());
            }
            if (!map.get(entity.getDbName()).containsKey(entity.getTbName())) {
                map.get(entity.getDbName()).put(entity.getTbName(),
                        entity.getMaxId() == null ? 1 : entity.getMaxId());
            }
            if (!Util.isEmpty(ip)) {
                Map<String, TableInfoEntity> tbMap = new HashMap<>();
                if (!finalDbMap.containsKey(entity.getDbName())) {
                    finalDbMap.put(entity.getDbName(), tbMap);
                }
                finalDbMap.get(entity.getDbName()).put(entity.getTbName(), entity);
            }
        }
        if (!Util.isEmpty(ip)) {
            tbInfoRef.get().put(ip, dbMap);
        }
        clearDbId();
        return map;
    }

    @Override
    public Map<String, Map<String, Long>> getMaxId() {
        return getMaxIdByIp(null);
    }

    @Override
    public Map<String, Map<String, Map<String, TableInfoEntity>>> getTableInfoCache() {
        return null;
    }

    @Override
    public void truncate(String tbName) {

    }

    @Override
    public int getTableQuantityByDbName(String dbName) {
        return 0;
    }

    @Override
    public List<String> getTableNamesByDbName(String dbName) {
        return null;
    }

    @Override
    public void createMessageTable(String tbName) {

    }

    @Override
    public List<Message01Entity> getListByTime(String tbName, String insertTime) {
        return null;
    }

    @Override
    public void clearDbId() {
        dbId.remove();
        isMaster.remove();
        dbCounter.decrementAndGet();
    }

    private void setMaster(boolean b) {
        isMaster.set(b);
    }
}
