package com.bella.cango.instance.oracle.extractor;

import com.bella.cango.instance.oracle.common.lifecycle.AbstractYuGongLifeCycle;
import com.bella.cango.instance.oracle.common.model.ExtractStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author agapple 2014年2月25日 下午11:38:06
 * @since 1.0.0
 */
public abstract class AbstractRecordExtractor extends AbstractYuGongLifeCycle implements RecordExtractor {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected volatile ExtractStatus status = ExtractStatus.NORMAL;

    public void setStatus(ExtractStatus status) {
        this.status = status;
    }

    public ExtractStatus status() {
        return status;
    }


}
