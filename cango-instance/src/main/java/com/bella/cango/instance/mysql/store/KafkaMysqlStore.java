package com.bella.cango.instance.mysql.store;

import com.alibaba.otter.canal.instance.manager.model.CanalParameter;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.position.Position;
import com.alibaba.otter.canal.store.AbstractCanalStoreScavenge;
import com.alibaba.otter.canal.store.CanalEventStore;
import com.alibaba.otter.canal.store.CanalStoreException;
import com.alibaba.otter.canal.store.CanalStoreScavenge;
import com.alibaba.otter.canal.store.model.Event;
import com.alibaba.otter.canal.store.model.Events;
import com.bella.cango.instance.cache.CangoInstanceCache;
import com.bella.cango.dto.CangoMsgDto;
import com.bella.cango.dto.ColumnDto;
import com.bella.cango.enums.EventType;
import com.bella.cango.instance.mysql.MysqlInstance;
import com.bella.cango.message.producer.KafkaProducer;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The type Kafka mysql store.
 */
public class KafkaMysqlStore extends AbstractCanalStoreScavenge implements CanalEventStore<Event>, CanalStoreScavenge {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMysqlStore.class);

    private static KafkaProducer kafkaProducer;

    protected CanalParameter parameters;

    protected String dbHost;

    protected int dbPort;

    public KafkaMysqlStore(CanalParameter parameters) {
        this.parameters = parameters;
        initKafkaService();
        if (parameters != null) {
            List<InetSocketAddress> inetSocketAddresses = parameters.getDbAddresses();
            if (CollectionUtils.isNotEmpty(inetSocketAddresses)) {
                this.dbHost = inetSocketAddresses.get(0).getHostName();
                this.dbPort = inetSocketAddresses.get(0).getPort();
            } else {
                LOGGER.error("host cannot null");
            }
        }
    }

    private void initKafkaService() {
        if (kafkaProducer == null) {
            synchronized (KafkaMysqlStore.class) {
                if (kafkaProducer == null) {
                    kafkaProducer = KafkaProducer.KafkaProducerHolder.getInstance();
                }
            }
        }
    }


    @Override
    public void cleanAll() throws CanalStoreException {

    }

    @Override
    public void cleanUntil(Position position) throws CanalStoreException {

    }

    @Override
    public void ack(Position position) throws CanalStoreException {

    }

    @Override
    public void put(List<Event> list) throws InterruptedException, CanalStoreException {
        if (CollectionUtils.isNotEmpty(list)) {
            for (Event event : list) {
                CanalEntry.Entry entry = event.getEntry();
                try {
                    CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                    CanalEntry.EventType eventType = rowChange.getEventType();

                    if (eventType == CanalEntry.EventType.INSERT || eventType == CanalEntry.EventType.UPDATE || eventType == CanalEntry.EventType.DELETE) {
                        List<CanalEntry.RowData> rowDatas = rowChange.getRowDatasList();
                        for (CanalEntry.RowData rowData : rowDatas) {
                            sendKafkaMsg(rowData, entry, eventType);
                        }
                    }
                } catch (InvalidProtocolBufferException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Send kafka msg.
     *
     * @param rowData   the row data
     * @param entry     the entry
     * @param eventType the event type
     */
    private void sendKafkaMsg(CanalEntry.RowData rowData, CanalEntry.Entry entry, CanalEntry.EventType eventType) {
        String schemaName = entry.getHeader().getSchemaName();
        String tableName = entry.getHeader().getTableName();
        String key = CangoInstanceCache.createKey(dbHost, dbPort);

        LOGGER.info("mysqlInstance receive data，KEY:{}, EventType:{}, dbName:{}, tableName:{}, time:{}",
                key,
                eventType,
                schemaName,
                tableName,
                new DateTime(entry.getHeader().getExecuteTime()).toString("yyyy-MM-dd HH:mm:ss"));

        MysqlInstance mysqlInstance = CangoInstanceCache.getMysqlInstance(key);

        // 如果未找到实例则抛弃消息
        if (mysqlInstance == null) {
            //抛弃消息！
            LOGGER.info("[MysqlInstance] -> not found {} in CangoInstanceCache, so discard it!", key);
            return;
        }

        String fullName = schemaName + "." + tableName;
        // 过滤未注册的表
        if (!contains(mysqlInstance.getFullTableList(), fullName)) {
            LOGGER.info("[MysqlInstance] -> discard not registered {}.{}", schemaName, tableName);
            return;
        }

        //创建消息对象
        CangoMsgDto msgDto = new CangoMsgDto()
                .setDbHost(dbHost)
                .setDbPort(dbPort)
                .setDbName(schemaName)
                .setTableName(tableName)
                .setEventType(EventType.valueOf(eventType.getNumber()))
                .setExecuteTime(entry.getHeader().getExecuteTime())
                .setName(mysqlInstance.getName());

        List<ColumnDto> columnDtos = new ArrayList<>();
        if (eventType == CanalEntry.EventType.DELETE) {
            List<CanalEntry.Column> columns = rowData.getBeforeColumnsList();
            encapsulationColumn(msgDto, columns, columnDtos, false);
        } else if (eventType == CanalEntry.EventType.INSERT) {
            List<CanalEntry.Column> columns = rowData.getAfterColumnsList();
            encapsulationColumn(msgDto, columns, columnDtos, true);
        } else if (eventType == CanalEntry.EventType.UPDATE) {
            List<CanalEntry.Column> oldColumns = rowData.getBeforeColumnsList();
            encapsulationColumn(msgDto, oldColumns, columnDtos, false);
            List<CanalEntry.Column> columns = rowData.getAfterColumnsList();
            encapsulationColumn(msgDto, columns, columnDtos, true);
        }

        msgDto.setColumns(columnDtos);

        kafkaProducer.send(msgDto);
        LOGGER.debug("CanalInstance host : {}, dbName : {}, tableName : {}", dbHost, schemaName, tableName);
    }

    /**
     * Contains boolean.
     *
     * @param list   the list
     * @param target the target
     * @return the boolean
     *
     * @date : 2016-07-18 17:59:52
     */
    private boolean contains(Collection<String> list, String target) {
        if (CollectionUtils.isEmpty(list)) {
            return false;
        } else {
            for (String item : list) {
                if (target.equalsIgnoreCase(item)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void encapsulationColumn(CangoMsgDto msgDto, List<CanalEntry.Column> columns, List<ColumnDto> columnDtos, boolean isNewData) {
        for (CanalEntry.Column column : columns) {
            if (column.getIsKey()) {
                msgDto.setPkName(column.getName()).setPkValue(convertValue(column.getValue(), column.getSqlType()));
            }

            ColumnDto columnDto = getColumnDto(columnDtos, column.getName());
            columnDto.setJdbcType(column.getSqlType());

            if (isNewData) {
                columnDto.setStrValue(column.getValue());
                columnDto.setValue(convertValue(column.getValue(), column.getSqlType()));
            } else {
                columnDto.setOldStrValue(column.getValue());
                columnDto.setOldValue(convertValue(column.getValue(), column.getSqlType()));
            }
        }
    }

    /**
     * @param columnDtos the column dtos
     * @param columnName the column name
     * @return the column dto
     */
    private ColumnDto getColumnDto(List<ColumnDto> columnDtos, String columnName) {
        if (CollectionUtils.isNotEmpty(columnDtos)) {
            for (ColumnDto columnDto : columnDtos) {
                if (columnDto.getName().equals(columnName)) {
                    return columnDto;
                }
            }
        }
        ColumnDto columnDto = new ColumnDto();
        columnDto.setName(columnName);
        columnDtos.add(columnDto);
        return columnDto;
    }

    private Object convertValue(String origin, int jdbcType) {
        if (origin != null) {
            try {
                switch (jdbcType) {
                    case 12:
                        return origin;
                    case 1:
                        return origin;
                    case -4:
                        return null; //不支持BLOB
                    case -1:
                        return origin;
                    case 4:
                        return StringUtils.isNotBlank(origin) ? Integer.valueOf(origin) : null;
                    case -6:
                        return StringUtils.isNotBlank(origin) ? Integer.valueOf(origin) : null;
                    case 5:
                        return StringUtils.isNotBlank(origin) ? Integer.valueOf(origin) : null;
                    case -7:
                        return StringUtils.isNotBlank(origin) ? Integer.valueOf(origin) : null;
                    case -5:
                        return StringUtils.isNotBlank(origin) ? Long.valueOf(origin) : null;
                    case 7:
                        return StringUtils.isNotBlank(origin) ? Float.valueOf(origin) : null;
                    case 8:
                        return StringUtils.isNotBlank(origin) ? Double.valueOf(origin) : null;
                    case 3:
                        return StringUtils.isNotBlank(origin) ? new BigDecimal(origin) : null;
                    case 91:
                        return StringUtils.isNotBlank(origin) ? Date.valueOf(origin) : null;
                    case 92:
                        return StringUtils.isNotBlank(origin) ? Time.valueOf(origin) : null;
                    case 93:
                        return StringUtils.isNotBlank(origin) ? DateTime.parse(origin, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")).toDate() : null;
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return null;
    }


    @Override
    public boolean put(List<Event> list, long l, TimeUnit timeUnit) throws InterruptedException, CanalStoreException {
        put(list);
        return true;
    }

    @Override
    public boolean tryPut(List<Event> list) throws CanalStoreException {
        try {
            put(list);
        } catch (InterruptedException e) {
            throw new CanalStoreException(e);
        }
        return true;
    }

    @Override
    public void put(Event event) throws InterruptedException, CanalStoreException {
        put(Collections.singletonList(event));
    }

    @Override
    public boolean put(Event event, long l, TimeUnit timeUnit) throws InterruptedException, CanalStoreException {
        return put(Collections.singletonList(event), l, timeUnit);
    }

    @Override
    public boolean tryPut(Event event) throws CanalStoreException {
        return tryPut(Collections.singletonList(event));
    }

    @Override
    public Events<Event> get(Position position, int i) throws InterruptedException, CanalStoreException {
        return null;
    }

    @Override
    public Events<Event> get(Position position, int i, long l, TimeUnit timeUnit) throws InterruptedException, CanalStoreException {
        return null;
    }

    @Override
    public Events<Event> tryGet(Position position, int i) throws CanalStoreException {
        return null;
    }

    @Override
    public Position getLatestPosition() throws CanalStoreException {
        return null;
    }

    @Override
    public Position getFirstPosition() throws CanalStoreException {
        return null;
    }

    @Override
    public void rollback() throws CanalStoreException {

    }
}
