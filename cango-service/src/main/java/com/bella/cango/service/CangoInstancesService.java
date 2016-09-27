package com.bella.cango.service;

import com.bella.cango.entity.CangoInstances;

import java.util.List;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/7
 */
public interface CangoInstancesService {
    void save(CangoInstances cangoInstances);

    void updateState(CangoInstances cangoInstances);

    void delete(CangoInstances cangoInstances);

    CangoInstances findByName(CangoInstances cangoInstances);

    void deleteAll();

    /**
     * Find by condition list, including HOST, PORT, STATE, DBTYPE.
     *
     * @param cangoInstances the cango instances
     * @return the list
     */
    List<CangoInstances> findByCondition(CangoInstances cangoInstances);

    void batchUpdateState(List<CangoInstances> instancesList);

    void saveOrUpdate(CangoInstances cangoInstances);
}
