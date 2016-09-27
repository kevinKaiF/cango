package com.bella.cango.service;

import com.bella.cango.entity.CangoTable;

import java.util.List;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
public interface CangoTableService {
    void save(CangoTable cangoTable);

    CangoTable query(CangoTable cangoTable);

    void deleteByInstancesName(CangoTable cangoTable);

    List<CangoTable> findByInstancesName(CangoTable cangoTable);

    void deleteAll();

    void batchSave(List<CangoTable> cangoTables);
}
