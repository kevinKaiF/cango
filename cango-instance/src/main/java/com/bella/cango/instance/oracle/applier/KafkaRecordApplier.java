package com.bella.cango.instance.oracle.applier;

import com.bella.cango.dto.CangoMsgDto;
import com.bella.cango.dto.ColumnDto;
import com.bella.cango.enums.EventType;
import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.db.meta.Table;
import com.bella.cango.instance.oracle.common.model.YuGongContext;
import com.bella.cango.instance.oracle.common.model.record.KafkaIncrementRecord;
import com.bella.cango.instance.oracle.common.model.record.Record;
import com.bella.cango.instance.oracle.common.utils.YuGongUtils;
import com.bella.cango.instance.oracle.exception.YuGongException;
import com.bella.cango.message.producer.KafkaProducer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class KafkaRecordApplier extends AbstractRecordApplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRecordApplier.class);
    private static KafkaProducer kafkaProducer;
    protected YuGongContext context;

    public KafkaRecordApplier(YuGongContext context) {
        this.context = context;
    }

    private void initKafkaService() {
        synchronized (KafkaRecordApplier.class) {
            if (kafkaProducer == null) {
                kafkaProducer = KafkaProducer.Builder.getInstance();
            }
        }
    }

    public void start() {
        super.start();
        initKafkaService();
    }

    public void stop() {
        super.stop();
    }

    public void apply(List<Record> records, Table tableMeta) throws YuGongException {
        // no one,just return
        if (YuGongUtils.isEmpty(records)) {
            return;
        }

        doApply(records, tableMeta);
    }

    protected void doApply(List records, Table tableMeta) {
        applyOneByOne(records, tableMeta);
    }

    private void applyOneByOne(List<KafkaIncrementRecord> recordList, Table tableMeta) {
        List<CangoMsgDto> canalMsgDtoList = new ArrayList<>(recordList.size());
        for (final KafkaIncrementRecord record : recordList) {
            CangoMsgDto msgDto = null;
            switch (record.getOpType()) {
                case I:
                    msgDto = getInsertMsgDto(tableMeta, record);
                    break;
                case D:
                    msgDto = getDeleteMsgDto(tableMeta, record);
                    break;
                case U:
                    msgDto = getUpdateMsgDto(tableMeta, record);
                    break;
            }

            if (msgDto != null) {
                // 从tableMeta获取真实的schema
                canalMsgDtoList.add(convertDbName(msgDto, tableMeta.getSchema()));
            }
        }

        if (!CollectionUtils.isEmpty(canalMsgDtoList)) {
            for (CangoMsgDto cangoMsgDto : canalMsgDtoList) {
                LOGGER.info("OracleInstance received message，dbHost:{}, EventType:{}, dbName:{}, tableName:{}, 时间:{}",
                        cangoMsgDto.getDbHost(),
                        cangoMsgDto.getEventType(),
                        cangoMsgDto.getDbName(),
                        cangoMsgDto.getTableName(),
                        new DateTime(cangoMsgDto.getExecuteTime()).toString("yyyy-MM-dd HH:mm:ss"));
                kafkaProducer.send(cangoMsgDto);
            }
        }
    }

    /**
     * Oracle数据库同步时，只需要service Id即可
     *
     * @param msgDto
     * @param schema
     */
    private CangoMsgDto convertDbName(CangoMsgDto msgDto, String schema) {
        msgDto.setDbName(schema);
        return msgDto;
    }

    private CangoMsgDto getInsertMsgDto(Table tableMeta, KafkaIncrementRecord record) {
        CangoMsgDto msgDto = new CangoMsgDto()
                .setDbHost(context.getDbHost())
                .setDbPort(context.getDbPort())
                .setDbName(tableMeta.getSchema())
                .setTableName(tableMeta.getName())
                .setEventType(EventType.INSERT)
                .setExecuteTime(record.getExecuteTime())
                .setName(context.getName());
        List<ColumnValue> primaryKeyValues = record.getPrimaryKeys();
        if (!CollectionUtils.isEmpty(primaryKeyValues)) {
            ColumnValue firstColumnValue = primaryKeyValues.get(0);
            msgDto.setPkName(firstColumnValue.getColumn().getName()).setPkValue(firstColumnValue.getValue());
        }

        List<ColumnDto> columnDtoList = new ArrayList<ColumnDto>();
        // 插入只需要获取变动之后的值即可
        List<ColumnValue> afterColumns = record.getAfterColumns();
        for (ColumnValue afterColumn : afterColumns) {
            ColumnDto columnDto = new ColumnDto();
            columnDto.setName(afterColumn.getColumn().getName())
                    .setJdbcType(afterColumn.getColumn().getType())
                    .setOldStrValue(null)
                    .setOldValue(null)
                    .setStrValue(convertToString(afterColumn.getValue()))
                    .setValue(afterColumn.getValue());
            columnDtoList.add(columnDto);
        }
        msgDto.setColumns(columnDtoList);
        return msgDto;
    }

    private CangoMsgDto getUpdateMsgDto(Table tableMeta, KafkaIncrementRecord record) {
        CangoMsgDto msgDto = new CangoMsgDto()
                .setDbHost(context.getDbHost())
                .setDbPort(context.getDbPort())
                .setDbName(tableMeta.getSchema())
                .setTableName(tableMeta.getName())
                .setEventType(EventType.UPDATE)
                .setExecuteTime(record.getExecuteTime())
                .setName(context.getName());
        List<ColumnValue> primaryKeyValues = record.getPrimaryKeys();
        if (!CollectionUtils.isEmpty(primaryKeyValues)) {
            ColumnValue firstColumnValue = primaryKeyValues.get(0);
            msgDto.setPkName(firstColumnValue.getColumn().getName()).setPkValue(firstColumnValue.getValue());
        }

        List<ColumnDto> columnDtoList = new ArrayList<ColumnDto>();
        List<ColumnValue> beforeColumns = record.getBeforeColumns();
        List<ColumnValue> afterColumns = record.getAfterColumns();
        for (int i = 0, len = beforeColumns.size(); i < len; i++) {
            ColumnValue beforeColumn = beforeColumns.get(i);
            ColumnValue afterColumn = afterColumns.get(i);
            ColumnDto columnDto = new ColumnDto();
            columnDto.setName(beforeColumn.getColumn().getName())
                    .setJdbcType(beforeColumn.getColumn().getType())
                    .setOldStrValue(convertToString(beforeColumn.getValue()))
                    .setOldValue(beforeColumn.getValue())
                    .setStrValue(convertToString(afterColumn.getValue()))
                    .setValue(afterColumn.getValue());
            columnDtoList.add(columnDto);
        }
        msgDto.setColumns(columnDtoList);
        return msgDto;
    }

    private CangoMsgDto getDeleteMsgDto(Table tableMeta, KafkaIncrementRecord record) {
        CangoMsgDto msgDto = new CangoMsgDto()
                .setDbHost(context.getDbHost())
                .setDbPort(context.getDbPort())
                .setDbName(tableMeta.getSchema())
                .setTableName(tableMeta.getName())
                .setEventType(EventType.DELETE)
                .setExecuteTime(record.getExecuteTime())
                .setName(context.getName());
        List<ColumnValue> primaryKeyValues = record.getPrimaryKeys();
        if (!CollectionUtils.isEmpty(primaryKeyValues)) {
            ColumnValue firstColumnValue = primaryKeyValues.get(0);
            msgDto.setPkName(firstColumnValue.getColumn().getName()).setPkValue(firstColumnValue.getValue());
        }

        List<ColumnDto> columnDtoList = new ArrayList<ColumnDto>();
        List<ColumnValue> columnValues = record.getBeforeColumns();
        for (ColumnValue columnValue : columnValues) {
            ColumnDto columnDto = new ColumnDto();
            columnDto.setName(columnValue.getColumn().getName())
                    .setJdbcType(columnValue.getColumn().getType())
                    .setOldStrValue(convertToString(columnValue.getValue()))
                    .setOldValue(columnValue.getValue())
                    .setStrValue(null)
                    .setValue(null);
            columnDtoList.add(columnDto);
        }
        msgDto.setColumns(columnDtoList);
        return msgDto;
    }

    private String convertToString(Object object) {
        if (object == null) {
            return null;
        } else {
            if (object instanceof Timestamp) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return sdf.format(object);
            } else {
                return object.toString();
            }
        }
    }

}
