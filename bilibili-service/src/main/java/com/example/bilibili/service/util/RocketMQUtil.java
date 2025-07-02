package com.example.bilibili.service.util;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQUtil {

    public static void syncSendMsg(DefaultMQProducer producer, Message msg) throws Exception {
        SendResult sendResult = producer.send(msg);
        System.out.println(sendResult);
    }

    public static void asyncSendMsg(DefaultMQProducer producer, Message msg) throws Exception {
        producer.send(msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                Logger logger = LoggerFactory.getLogger(RocketMQUtil.class);
                logger.info("Asynchronized message sent successfully, message id: " + sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                e.printStackTrace();
            }
        });
    }

}
