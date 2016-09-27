package com.bella.cango.instance.oracle.extractor;


import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.db.meta.Table;
import com.bella.cango.instance.oracle.common.model.ExtractStatus;
import com.bella.cango.instance.oracle.common.model.YuGongContext;
import com.bella.cango.instance.oracle.exception.YuGongException;

import java.util.List;
import java.util.Map;

/**
 *
 * @version : Ver 1.0
 * @date : 2016-05-06 PM02:37
 */
public class KafkaAllRecordExtractor extends AbstractRecordExtractor {
    private AbstractOracleRecordExtractor kafkaIncExtractor;  // 增量抽取接口
    private YuGongContext context;

    public KafkaAllRecordExtractor(YuGongContext context) {
        this.context = context;

    }

    public void start() {
        super.start();
        kafkaIncExtractor.start();
    }

    public void stop() {
        super.stop();
        if (kafkaIncExtractor.isStart()) {
            kafkaIncExtractor.stop();
        }
    }

    @Override
    public Map<String, Object> extract(Table table) throws YuGongException {
        if (kafkaIncExtractor.isStart()) {
            return kafkaIncExtractor.extract(table);
        } else {
            return null;
        }
    }

    @Override
    public void clearMlog(List<ColumnValue> records, String mlogCleanSql) {
        if (kafkaIncExtractor.isStart()) {
            kafkaIncExtractor.clearMlog(records, mlogCleanSql);
        }

    }

    public AbstractOracleRecordExtractor getKafkaIncExtractor() {
        return kafkaIncExtractor;
    }

    public void setKafkaIncExtractor(AbstractOracleRecordExtractor kafkaIncExtractor) {
        this.kafkaIncExtractor = kafkaIncExtractor;
    }

    @Override
    public ExtractStatus status() {
        return kafkaIncExtractor.status();
    }
}
