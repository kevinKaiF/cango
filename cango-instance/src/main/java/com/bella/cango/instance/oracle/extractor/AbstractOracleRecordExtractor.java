package com.bella.cango.instance.oracle.extractor;


import com.bella.cango.instance.oracle.common.db.meta.ColumnMeta;
import com.bella.cango.instance.oracle.common.db.meta.ColumnValue;
import com.bella.cango.instance.oracle.common.utils.YuGongUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public abstract class AbstractOracleRecordExtractor extends AbstractRecordExtractor {

    /**
     * 从oracle的resultset中得到value
     * <p/>
     * <pre>
     * 1. 对于DATE类型特殊处理成TIMESTAMP类型，否则复制过去会丢掉时间部分
     * 2.  如果为字符串类型，并且需要进行转码，那么进行编码转换。
     * </pre>
     *
     * @param col
     * @return
     * @throws java.sql.SQLException
     */
    protected ColumnValue getColumnValue(ResultSet rs, String encoding, ColumnMeta col) throws SQLException {
        Object value = null;
        if (col.getType() == Types.DATE) {
            value = rs.getTimestamp(col.getName());
            col = new ColumnMeta(col.getName(), Types.TIMESTAMP);
        } else if (col.getType() == Types.TIMESTAMP) {
            value = rs.getTimestamp(col.getName());
            col = new ColumnMeta(col.getName(), Types.TIMESTAMP);
        } else if (YuGongUtils.isCharType(col.getType())) {
            value = rs.getString(col.getName());
        } else if (YuGongUtils.isClobType(col.getType())) {
            value = rs.getString(col.getName());
        } else if (YuGongUtils.isBlobType(col.getType())) {
            value = rs.getBytes(col.getName());
        } else {
            value = rs.getObject(col.getName());
        }

        // 使用clone对象，避免translator修改了引用
        return new ColumnValue(col.clone(), value);
    }
}
