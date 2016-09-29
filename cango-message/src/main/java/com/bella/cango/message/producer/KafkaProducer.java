package com.bella.cango.message.producer;

import com.alibaba.fastjson.JSON;
import com.bella.cango.exception.CangoException;
import com.bella.cango.utils.PropertiesFileLoader;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TODO
 *
 * @author kevin
 * @date 2016/9/17
 */
public class KafkaProducer {
    private static Logger LOGGER = LoggerFactory.getLogger(KafkaProducer.class);
    private Producer<String, String> producer;
    private String topic;
    private Thread worker;
    private BlockingQueue<String> messageQueue;
    private volatile boolean start;

    public static class KafkaProducerHolder {
        private static KafkaProducer KafkaProducer = new KafkaProducer();
        public static KafkaProducer getInstance() {
            KafkaProducer.start();
            return KafkaProducer;
        }
    }

    private KafkaProducer() {
        init();
        messageQueue = new LinkedBlockingQueue<>();
        start = false;
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String message = messageQueue.poll();
                        producer.send(new KeyedMessage<String, String>(topic, message));
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void send(String message) {
        messageQueue.offer(message);
    }

    public void send(Object object) {
        messageQueue.offer(JSON.toJSONString(object));
    }

    public void start() {
        if (!start) {
            worker.start();
        }
    }

    private void init() {
        try {
            Properties props = PropertiesFileLoader.loadKafkaProperties();
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("serializer.class", "kafka.serializer.StringEncoder");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("request.required.acks", "0");
            producer = new Producer<>(new ProducerConfig(props));
            topic = props.getProperty("topic", "cango");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LOGGER.error("not found kafkaProduce.properties, please check it");
            throw new CangoException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
