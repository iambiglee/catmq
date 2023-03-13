package com.baracklee.mq.biz.service.impl;

import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.biz.dal.msg.Message01Repository;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.Message01Entity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TableInfoEntity;
import com.baracklee.mq.biz.service.DbNodeService;
import com.baracklee.mq.biz.service.Message01Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class Message01ServiceImpl implements Message01Service {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public String getMaxConnectionsCount() {
        setMaster(true);
        Map<String, String> connectionsCount = message01Repository.getMaxConnectionsCount();
        clearDbId();
        if (CollectionUtils.isEmpty(connectionsCount)) return "0";
        else return connectionsCount.get("Value");
    }

    @Override
    public Integer getConnectionsCount() {
        try {
            setMaster(true);
            Integer count = message01Repository.getConnectionsCount();
            if (count == null)
                return 0;
            return count;
        } finally {
            clearDbId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public void updateFailMsgResult(String tbName, List<Long> ids, int retryCount) {
        try {
            setMaster(true);
            message01Repository.updateFailMsgResult(getDbName() + "." + tbName, ids, retryCount);
        } finally {
            clearDbId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public int deleteOldFailMsg(String tbName, long id, int retryCount) {
        try {
            setMaster(true);
            return message01Repository.deleteOldFailMsg(getDbName() + "." + tbName, id, retryCount);
        } finally {
            clearDbId();
        }
    }

    @Override
    public long getNextId(String tbName, long id, int size) {
        try {
            setMaster(true);
            Long maxId = message01Repository.getNextId(getDbName() + "." + tbName, id, size);
            if (maxId == null) {
                return 0;
            }
            return maxId;
        } catch (Throwable e) {
            log.error("getNextId_error", e);
        } finally {
            clearDbId();
        }
        return 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public TableInfoEntity getSingleTableInfoFromCache(QueueEntity queueEntity) {
        Map<String, Map<String, Map<String, TableInfoEntity>>> tableInfoCache = getTableInfoCache();
        if (tableInfoCache.containsKey(queueEntity.getIp())
                && tableInfoCache.get(queueEntity.getIp()).containsKey(queueEntity.getDbName())
                && tableInfoCache.get(queueEntity.getIp()).get(queueEntity.getDbName()).containsKey(queueEntity.getTbName())) {
            return tableInfoCache.get(queueEntity.getIp()).get(queueEntity.getDbName()).get(queueEntity.getTbName());
        }
        return null;
        }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public Message01Entity getMinIdMsg(String tbName) {
        try {
            setMaster(false);
            return message01Repository.getMinIdMsg(getDbName() + "." + tbName);
        } catch (Exception e) {
            log.error("getMinIdMsg_error", e);
        } finally {
            clearDbId();
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public void insertBatchDy(String topic, String tbName, List<Message01Entity> entities) {
        setMaster(true);
        message01Repository.insertBatchDy(getDbName()+"."+tbName,entities);
        clearDbId();
    }

    @Override
    public List<Message01Entity> getListDy(String topic, String tbName, long start, long end) {
        setMaster(false);
        return message01Repository.getListDy(getDbName()+"."+tbName,start,end);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public List<Message01Entity> getListByPage(Map<String, Object> parameterMap) {
        setMaster(false);
        return message01Repository.getListByPageSize(parameterMap);
    }

    @Override
    public Long getTableMinId(String tbName) {
        return message01Repository.getTableMinId(getDbName()+"."+tbName);
    }

    @Override
    public long countByPage(Map<String, Object> parameterMap) {
        return message01Repository.countByPage(parameterMap);
    }

    @Override
    public Message01Entity getMessageById(String tbName, long id) {
        setMaster(false);
        Message01Entity message01Entity = null;
        try {
            message01Entity = message01Repository.getMessageById(getDbName() + "." + tbName, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            clearDbId();
        }
        return message01Entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public List<Message01Entity> getMessageByIds(String tbName, List<Long> ids) {
        setMaster(false);
        List<Message01Entity> message01Entitys = null;
        try {
            message01Entitys = message01Repository.getMessageByIds(getDbName() + "." + tbName, ids);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            clearDbId();
        }
        return message01Entitys;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public Message01Entity getNearByMessageById(String tbName, long id) {
        setMaster(false);
        Message01Entity nearByMessageById = message01Repository.getNearByMessageById(getDbName() + "." + tbName, id);
        clearDbId();
        return nearByMessageById;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public int deleteDy(String tbName, long nextId, String date, int size, long maxId) {
        try {
            setMaster(true);
            return message01Repository.deleteDy(getDbName() + "." + tbName, nextId, date, size,maxId);
        } catch (Throwable e) {
            return 0;
        } finally {
            clearDbId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public void deleteByIds(String tbName, List<Long> ids) {
        try {
            setMaster(true);
            message01Repository.deleteByIds(getDbName() + "." + tbName, ids);
        } catch (Exception e) {
            log.error("deleteByIds_error", e);
        } finally {
            clearDbId();
        }
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
        return dbNodeService.getDataSource(dbId.get(),isMaster.get());
    }

    /**
     * 获取所有的队列名称, 同一个schema的发到一个map中返回, map(schema(tablename, maxid))
     * 顺便更新了一波另一个容器, key 是IP, value是数据库表在数据库中的信息
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
                if (!dbMap.containsKey(entity.getDbName())) {
                    dbMap.put(entity.getDbName(), tbMap);
                }
                dbMap.get(entity.getDbName()).put(entity.getTbName(), entity);
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
        return tbInfoRef.get();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public void truncate(String tbName) {
        try {
            setMaster(true);
            message01Repository.truncate(getDbName() + "." + tbName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            clearDbId();
        }
    }

    @Override
    public int getTableQuantityByDbName(String dbName) {
        Map<String, Map<String, Long>> data = getMaxId();
        if (data.containsKey(dbName)) {
            return data.get(dbName).size();
        } else {
            return 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public List<String> getTableNamesByDbName(String dbName) {
        Map<String, Map<String, Long>> data = getMaxId();
        List<String> tableNames = new ArrayList<>();
        if (data.containsKey(dbName)) {
            return new ArrayList<>(data.get(dbName).keySet());
        }
        return tableNames;
    }

    @Override
    public void createMessageTable(String tbName) {
        setMaster(true);
        message01Repository.createMessageTable(tbName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.NOT_SUPPORTED, value = "msgTransactionManager")
    public List<Message01Entity> getListByTime(String tbName, String insertTime) {
        List<Message01Entity> rs = new ArrayList<>();
        try {
            setMaster(false);
            rs = message01Repository.getListByTime(getDbName() + "." + tbName, insertTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            clearDbId();
        }
        return rs;
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
