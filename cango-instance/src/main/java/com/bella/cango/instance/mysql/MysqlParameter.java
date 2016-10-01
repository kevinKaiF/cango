package com.bella.cango.instance.mysql;

import com.alibaba.otter.canal.instance.manager.model.CanalParameter;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/10/1
 */
public class MysqlParameter extends CanalParameter {
    private String canalName;

    public String getCanalName() {
        return canalName;
    }

    public void setCanalName(String canalName) {
        this.canalName = canalName;
    }
}
