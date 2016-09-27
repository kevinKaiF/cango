package com.bella.cango.instance.oracle.common.model.record;

import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @version : Ver 1.0
 * @date : 2016-05-05 PM05:09
 */
public class KafkaIncrementRecord extends IncrementRecord {
    private ColumnValue rowId;
    private Long executeTime;
    private List<ColumnValue> beforeColumns = Lists.newArrayList(); // 变动之前的值
    private List<ColumnValue> afterColumns = Lists.newArrayList(); // 变动之后的值

    public KafkaIncrementRecord() {
    }

    public KafkaIncrementRecord(String schemaName, String tableName, List<ColumnValue> primaryKeys, List<ColumnValue> columns) {
        super(schemaName, tableName, primaryKeys, columns);
    }

    public ColumnValue getRowId() {
        return rowId;
    }

    public void setRowId(ColumnValue rowId) {
        this.rowId = rowId;
    }

    public Long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }

    public List<ColumnValue> getBeforeColumns() {
        return beforeColumns;
    }

    public void setBeforeColumns(List<ColumnValue> beforeColumns) {
        this.beforeColumns = beforeColumns;
    }

    public List<ColumnValue> getAfterColumns() {
        return afterColumns;
    }

    public void setAfterColumns(List<ColumnValue> afterColumns) {
        this.afterColumns = afterColumns;
    }

    @Override
    public KafkaIncrementRecord clone() {
        KafkaIncrementRecord record = new KafkaIncrementRecord();
        super.clone();
        record.setRowId(this.rowId);
        record.setExecuteTime(this.executeTime);
        record.setBeforeColumns(new ArrayList<ColumnValue>(this.beforeColumns));
        record.setAfterColumns(new ArrayList<ColumnValue>(this.afterColumns));
        return record;
    }
}
