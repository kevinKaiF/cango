package com.bella.cango.service.impl;

import com.bella.cango.entity.CangoInstances;
import com.bella.cango.service.CangoInstancesService;
import com.bella.cango.utils.ValidateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/8/30
 */
@Service("cangoInstancesService")
public class CangoInstancesServiceImpl implements CangoInstancesService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Date getCurrentTime() {
        return new Date(System.currentTimeMillis());
    }

    @Override
    public void save(final CangoInstances cangoInstances) {
        ValidateUtil.validate(cangoInstances);
        String insertSql = "insert into cango_instances (name, host, port, db_name, username, password, db_type, black_tables, slave_id, state, create_time) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, cangoInstances.getName());
                ps.setString(2, cangoInstances.getHost());
                ps.setInt(3, cangoInstances.getPort());
                ps.setString(4, cangoInstances.getDbName());
                ps.setString(5, cangoInstances.getUserName());
                ps.setString(6, cangoInstances.getPassword());
                ps.setInt(7, cangoInstances.getDbType());
                ps.setString(8, cangoInstances.getBlackTables());
                ps.setInt(9, cangoInstances.getSlaveId());
                ps.setInt(10, cangoInstances.getState());
                ps.setDate(11, getCurrentTime());
            }
        });
    }

    @Override
    public void updateState(final CangoInstances cangoInstances) {
        String updateSql = "update cango_instances set state = ?, update_time = ? where name = ?";
        jdbcTemplate.update(updateSql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setInt(1, cangoInstances.getState());
                ps.setDate(2, getCurrentTime());
                ps.setString(3, cangoInstances.getName());
            }
        });
    }

    @Override
    public void delete(final CangoInstances cangoInstances) {
        String deleteSql = "delete from cango_instances where name = ?";
        jdbcTemplate.update(deleteSql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, cangoInstances.getName());
            }
        });
    }

    @Override
    public CangoInstances findByName(final CangoInstances cangoInstances) {
        String querySql = "select * from cango_instances where name = ?";
        List<CangoInstances> cangoInstancesList = jdbcTemplate.query(querySql, new Object[]{cangoInstances.getName()}, new BeanPropertyRowMapper<>(CangoInstances.class));
        return CollectionUtils.isEmpty(cangoInstancesList)? null : cangoInstancesList.get(0);
    }

    @Override
    public void deleteAll() {
        String deleteAllSql = "delete from cango_instances";
        jdbcTemplate.execute(deleteAllSql);
    }

    @Override
    public List<CangoInstances> findByCondition(CangoInstances cangoInstances) {
        String querySql = "select * from cango_instances where 1=1 ";
        List params = new ArrayList();
        if (cangoInstances.getDbType() != null) {
            querySql += " and db_type = ? ";
            params.add(cangoInstances.getDbType());
        }

        if (cangoInstances.getState() != null) {
            querySql += " and state = ? ";
            params.add(cangoInstances.getState());
        }

        if (cangoInstances.getHost() != null) {
            querySql += " and host = ? ";
            params.add(cangoInstances.getHost());
        }

        if (cangoInstances.getPort() != null) {
            querySql += " and port = ? ";
            params.add(cangoInstances.getPort());
        }
        return jdbcTemplate.query(querySql, new Object[]{cangoInstances.getState()}, new BeanPropertyRowMapper<>(CangoInstances.class));
    }

    @Override
    public void batchUpdateState(final List<CangoInstances> instancesList) {
        if (instancesList != null && instancesList.size() > 0) {
            String updateSql = "update cango_instances set state = ?, update_time = ? where name = ?";
            final Date currentTime = getCurrentTime();
            jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    CangoInstances cangoInstances = instancesList.get(i);
                    ps.setInt(1, cangoInstances.getState());
                    ps.setDate(2, currentTime);
                    ps.setString(3, cangoInstances.getName());
                }

                @Override
                public int getBatchSize() {
                    return instancesList.size();
                }
            });
        }
    }

    @Override
    public void saveOrUpdate(CangoInstances cangoInstances) {
        final CangoInstances cangoInstancesPo = this.findByName(cangoInstances);
        if (cangoInstancesPo == null) {
            this.save(cangoInstances);
        } else {
            this.updateState(cangoInstances);
        }
    }

    class CangoInstancesRowMapper implements RowMapper<CangoInstances> {
        @Override
        public CangoInstances mapRow(ResultSet resultSet, int i) throws SQLException {
            CangoInstances cangoInstances = new CangoInstances();
            cangoInstances.setName(resultSet.getString("name"));
            cangoInstances.setDbName(resultSet.getString("db_name"));
            cangoInstances.setDbType(resultSet.getInt("db_type"));
            cangoInstances.setHost(resultSet.getString("host"));
            cangoInstances.setUserName(resultSet.getString("username"));
            cangoInstances.setPassword(resultSet.getString("password"));
            cangoInstances.setSlaveId(resultSet.getInt("slave_id"));
            cangoInstances.setState(resultSet.getInt("state"));
            cangoInstances.setCreateTime(resultSet.getDate("create_time"));
            cangoInstances.setUpdateTime(resultSet.getDate("update_time"));
            return cangoInstances;
        }
    }
}
