package com.bella.cango.example;

import com.bella.cango.client.CangoClient;
import com.bella.cango.client.RequestCommand;
import com.bella.cango.dto.CangoRequestDto;
import com.bella.cango.dto.CangoResponseDto;
import com.bella.cango.enums.DbType;

import java.io.IOException;
import java.util.HashSet;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/27
 */
public class MainClass {
    public static void main(String[] args) {
        try {
            String cangoHost = "localhost";
            int cangoPort = 8080;
            String cangoAppName = "cango";
            CangoClient cangoClient = new CangoClient(cangoHost, cangoPort, cangoAppName);
            CangoRequestDto cangoRequestDto = new CangoRequestDto()
                    .setHost("192.168.0.155")               // 设置数据库host
                    .setPort(3306)                          // 设置数据库port
                    .setDbName("test")                      // 设置schema
                    .setDbType(DbType.MYSQL)                // 设置数据库类型
                    .setSlaveId(3)                          // mysql必须设置
                    .setUserName("root")                    // 数据库用户名
                    .setPassword("root")                    // 数据库密码
                    .setTableNames(new HashSet<String>() {  // 需要同步的表
                        {
                            add("test.user");
                            add("test.group");
                        }
                    })
                    .setBlackTables("test.company,db.*");   // 需要过滤的表
            CangoResponseDto responseDto = cangoClient.request(RequestCommand.ADD, cangoRequestDto);
            System.out.println(responseDto);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
