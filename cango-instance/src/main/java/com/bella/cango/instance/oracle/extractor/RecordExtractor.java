package com.bella.cango.instance.oracle.extractor;

import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.db.meta.Table;
import com.bella.cango.instance.oracle.common.lifecycle.YuGongLifeCycle;
import com.bella.cango.instance.oracle.common.model.ExtractStatus;
import com.bella.cango.instance.oracle.exception.YuGongException;

import java.util.List;
import java.util.Map;

/**
 * 数据获取
 *
 * @author agapple 2013-9-3 下午2:36:56
 * @since 3.0.0
 */
public interface RecordExtractor extends YuGongLifeCycle {

    /**
     * 获取增量数据
     *
     * @return
     * @throws YuGongException
     */
    Map<String, Object> extract(Table table) throws YuGongException;

    /**
     * @return 当前extractor的状态,{@linkplain ExtractStatus}
     */
    ExtractStatus status();

    /**
     * 清理物化日志中已经读取的记录
     *
     * @param records
     */
    void clearMlog(final List<ColumnValue> records, String mlogCleanSql);

}
