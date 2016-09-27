package com.bella.cango.service.impl;

import com.bella.cango.entity.CangoTable;
import com.bella.cango.service.CangoTableService;
import com.bella.cango.utils.ValidateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/30
 */
@Service("cangoTableService")
public class CangoTableServiceImpl implements CangoTableService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Date getCurrentTime() {
        return new Date(System.currentTimeMillis());
    }

    @Override
    public CangoTable query(CangoTable cangoTable) {
        ValidateUtil.validate(cangoTable);
        String querySql = "select * from cango_table where instances_name = ? and table_name = ?";
        return jdbcTemplate.queryForObject(querySql, new Object[]{cangoTable.getInstancesName(), cangoTable.getTableName()}, new BeanPropertyRowMapper<>(CangoTable.class));
    }

    @Override
    public void save(final CangoTable cangoTable) {
        String insertSql = "insert into cango_table instances_name, table_name, create_time values(?, ?, ?)";
        jdbcTemplate.update(insertSql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, cangoTable.getInstancesName());
                ps.setString(2, cangoTable.getTableName());
                ps.setDate(3, getCurrentTime());
            }
        });
    }

    @Override
    public void deleteByInstancesName(final CangoTable cangoTable) {
        String deleteSql = "delete from cango_table where instances_name = ?";
        jdbcTemplate.update(deleteSql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, cangoTable.getInstancesName());
            }
        });
    }

    @Override
    public List<CangoTable> findByInstancesName(CangoTable cangoTable) {
        String queryListSql = "select * from cango_table where instances_name";
        return jdbcTemplate.query(queryListSql, new Object[]{cangoTable.getInstancesName()}, new BeanPropertyRowMapper<>(CangoTable.class));
    }

    @Override
    public void deleteAll() {
        String deleteAllSql = "delete from cango_table";
        jdbcTemplate.execute(deleteAllSql);
    }

    @Override
    public void batchSave(final List<CangoTable> cangoTables) {
        if (cangoTables != null && cangoTables.size() > 0) {
            String insertSql = "insert into cango_table instances_name, table_name, create_time values(?, ?, ?)";
            final Date current = getCurrentTime();
            jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    CangoTable cangoTable = cangoTables.get(i);
                    ps.setString(1, cangoTable.getInstancesName());
                    ps.setString(2, cangoTable.getTableName());
                    ps.setDate(3, current);
                }

                @Override
                public int getBatchSize() {
                    return cangoTables.size();
                }
            });
        }
    }
}
