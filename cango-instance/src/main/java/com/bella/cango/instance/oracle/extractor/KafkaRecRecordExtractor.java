package com.bella.cango.instance.oracle.extractor;

import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.db.meta.Table;
import com.bella.cango.instance.oracle.common.db.meta.TableMetaGenerator;
import com.bella.cango.instance.oracle.common.db.sql.SqlTemplates;
import com.bella.cango.instance.oracle.common.model.ExtractStatus;
import com.bella.cango.instance.oracle.common.model.YuGongContext;
import com.bella.cango.instance.oracle.exception.YuGongException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @version : Ver 1.0
 * @date : 2016-05-06 PM02:48
 */
public class KafkaRecRecordExtractor extends AbstractOracleRecordExtractor {
    private YuGongContext context;

    public KafkaRecRecordExtractor(YuGongContext context) {
        this.context = context;
    }

    public void start() {
        super.start();
    }

    public void stop() {
        super.stop();
    }

    @Override
    public Map<String, Object> extract(Table table) throws YuGongException {
        markIncPosition(table);
        return new HashMap<>();
    }

    public ExtractStatus status() {
        return ExtractStatus.TABLE_END;// 直接返回退出
    }

    @Override
    public void clearMlog(List<ColumnValue> records, String mlogCleanSql) {
        // 不作处理
    }

    /**
     * 做一下inc的增量标记
     */
    protected void markIncPosition(Table table) {
        String CREATE_MLOG_FORMAT = "CREATE MATERIALIZED VIEW LOG ON {0}.{1} WITH SEQUENCE({2}), {3} INCLUDING NEW VALUES ";
        String schemaName = table.getSchema();
        String tableName = table.getName();
        String columnStr = SqlTemplates.COMMON.makeColumn(table.getColumns());
        String createMlogSql = MessageFormat.format(CREATE_MLOG_FORMAT, new Object[]{schemaName, tableName, columnStr,
                " primary key"});

        String mlogName = TableMetaGenerator.getMLogTableName(context.getSourceDs(), schemaName, tableName);
        if (mlogName == null) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getSourceDs());
            jdbcTemplate.execute(createMlogSql);
            // 基于MLOG不需要返回position
            logger.info("create mlog successed. sql : {}", createMlogSql);
        } else {
            logger.warn("mlog[{}] is exist, just have fun. ", mlogName);
        }
    }

}
