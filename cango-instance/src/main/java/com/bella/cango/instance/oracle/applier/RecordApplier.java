package com.bella.cango.instance.oracle.applier;


import com.bella.cango.instance.oracle.common.db.meta.Table;
import com.bella.cango.instance.oracle.common.lifecycle.YuGongLifeCycle;
import com.bella.cango.instance.oracle.common.model.record.Record;
import com.bella.cango.instance.oracle.exception.YuGongException;

import java.util.List;

/**
 * 数据提交
 *
 * @author agapple 2013-9-9 下午5:57:19
 * @since 3.0.0
 */
public interface RecordApplier extends YuGongLifeCycle {

    public void apply(List<Record> records, Table tableMeta) throws YuGongException;

}
