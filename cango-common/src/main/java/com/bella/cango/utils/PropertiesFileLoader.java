package com.bella.cango.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * properties file loader
 *
 * @author kevin
 * @date 2016/9/29
 */
public class PropertiesFileLoader {
    public static final String PROJECT_DIR;

    public static final String CLASSPATH_DIR;

    public static final String DEFAULT_CONF_DIR;

    public static final String DEFAULT_KAFKA_PATH;

    public static final String DEFAULT_LOG_HOME;

    static {
        File classFile = new File(Thread.currentThread().getContextClassLoader().getResource(".").getFile());
        CLASSPATH_DIR = classFile.getAbsolutePath();
        PROJECT_DIR = classFile.getParentFile().getParentFile().getAbsolutePath();
        File confFile = new File(PROJECT_DIR, "src/conf");
        DEFAULT_CONF_DIR = confFile.getAbsolutePath();
        DEFAULT_KAFKA_PATH = DEFAULT_CONF_DIR + File.separator + "kafkaProduce.properties";
        DEFAULT_LOG_HOME = DEFAULT_CONF_DIR + File.separator + "log";
    }

    public static Properties loadKafkaProperties() throws IOException {
        String kafkaPath = System.getProperty("kafkaProduce");
        if (StringUtils.isEmpty(kafkaPath)) {
            kafkaPath = DEFAULT_KAFKA_PATH;
        }

        return PropertiesLoaderUtils.loadProperties(new FileSystemResource(kafkaPath));
    }

    public static Properties load(String path) throws IOException {
        if (StringUtils.isEmpty(path)) {
            return new Properties();
        }

        return PropertiesLoaderUtils.loadProperties(new FileSystemResource(path));
    }
}
