package com.bella.cango.instance.mysql;

import com.alibaba.otter.canal.instance.manager.CanalInstanceWithManager;
import com.alibaba.otter.canal.instance.manager.model.Canal;
import com.bella.cango.instance.mysql.store.KafkaMysqlStore;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
public class MysqlInstance extends CanalInstanceWithManager {
    private static Logger LOGGER = LoggerFactory.getLogger(MysqlInstance.class);

    private String name;

    private Set<String> fullTableList = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public MysqlInstance(Canal canal) {
        super(canal, null);
        this.name = canal.getName();
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    protected void initEventStore() {
        LOGGER.debug("init eventStore");
        eventStore = new KafkaMysqlStore(this.parameters);
    }

    @Override
    protected void initEventSink() {
        super.initEventSink();
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getFullTableList() {
        return Collections.unmodifiableSet(fullTableList);
    }

    public void addTables(Collection<String> tables) {
        if (CollectionUtils.isNotEmpty(tables)) {
            fullTableList.addAll(tables);
        }
    }

}
