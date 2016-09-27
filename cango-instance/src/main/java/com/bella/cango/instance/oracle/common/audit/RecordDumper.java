package com.bella.cango.instance.oracle.common.audit;

import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.model.record.IncrementOpType;
import com.bella.cango.instance.oracle.common.model.record.IncrementRecord;
import com.bella.cango.instance.oracle.common.model.record.Record;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 记录一下同步过程中的record数据
 *
 * @author agapple 2013-9-23 下午2:13:46
 */
public class RecordDumper {

    private static final Logger extractorLogger = LoggerFactory.getLogger("extractor");
    private static final Logger applierLogger = LoggerFactory.getLogger("applier");
    private static final String SEP = SystemUtils.LINE_SEPARATOR;
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";
    private static String extractor_format = null;
    private static String applier_format = null;
    private static String record_format = null;
    private static int record_default_capacity = 1024;                                // 预设值StringBuilder，减少扩容影响

    static {
        extractor_format = "* batchId : [{0}] , total : [{1}] , Time : {2} " + SEP;
        applier_format = "* batchId : [{0}] , extractorSize : [{1}] , applierSize : [{2}] , Time : {3} "
                + SEP;

        record_format = "-----------------" + SEP;
        record_format += "- Schema: {0} , Table: {1} , Type: {2}" + SEP;
        record_format += "-----------------" + SEP;
        record_format += "---Pks" + SEP;
        record_format += "{3}" + SEP;
        record_format += "---Columns" + SEP;
        record_format += "{4}" + SEP;
        record_format += "---END" + SEP;
    }

    public static void dumpExtractorInfo(Long batchId, List<Record> records, boolean dumpDetail) {
        extractorLogger.info(SEP + "****************************************************" + SEP);
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat(TIMESTAMP_FORMAT);
        extractorLogger.info(MessageFormat.format(extractor_format,
                String.valueOf(batchId),
                records.size(),
                format.format(now)));
        extractorLogger.info("****************************************************" + SEP);
        if (dumpDetail) {// 判断一下是否需要打印详细信息
            extractorLogger.info(dumpRecords(records));
            extractorLogger.info("****************************************************" + SEP);
        }
    }

    public static void dumpApplierInfo(Long batchId, List<Record> extractorRecords, List<Record> applierRecords, boolean dumpDetail) {
        applierLogger.info(SEP + "****************************************************" + SEP);
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat(TIMESTAMP_FORMAT);
        applierLogger.info(MessageFormat.format(applier_format,
                String.valueOf(batchId),
                extractorRecords.size(),
                applierRecords.size(),
                format.format(now)));
        applierLogger.info("****************************************************" + SEP);
        if (dumpDetail) {// 判断一下是否需要打印详细信息，目前只打印applier信息，分开打印
            applierLogger.info(dumpRecords(applierRecords));
            applierLogger.info("****************************************************" + SEP);
        }
    }

    public static String dumpRecords(List<Record> records) {
        if (CollectionUtils.isEmpty(records)) {
            return StringUtils.EMPTY;
        }

        // 预先设定容量大小
        StringBuilder builder = new StringBuilder(record_default_capacity * records.size());
        for (Record record : records) {
            builder.append(dumpRecord(record));
        }
        return builder.toString();
    }

    public static String dumpRecord(Record record) {
        IncrementOpType type = IncrementOpType.I;
        if (record instanceof IncrementRecord) {
            type = ((IncrementRecord) record).getOpType();
        }

        return MessageFormat.format(record_format,
                record.getSchemaName(),
                record.getTableName(),
                type,
                dumpRecordColumns(record.getPrimaryKeys()),
                dumpRecordColumns(record.getColumns()));
    }

    public static String dumpRecordColumns(List<ColumnValue> columns) {
        StringBuilder builder = new StringBuilder(record_default_capacity);
        int size = columns.size();
        for (int i = 0; i < size; i++) {
            ColumnValue column = columns.get(i);
            builder.append("\t").append(ObjectUtils.toString(column));
            if (i < columns.size() - 1) {
                builder.append(SEP);
            }
        }
        return builder.toString();
    }

}
