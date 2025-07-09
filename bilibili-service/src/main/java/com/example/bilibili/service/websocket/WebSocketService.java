package com.example.bilibili.service.websocket;

import com.alibaba.fastjson.JSONObject;
import com.example.bilibili.domain.Danmu;
import com.example.bilibili.domain.constant.UserMomentsConstant;
import com.example.bilibili.service.DanmuService;
import com.example.bilibili.service.util.RocketMQUtil;
import com.example.bilibili.service.util.TokenUtil;
import io.netty.util.internal.StringUtil;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/imserver/{token}")
public class WebSocketService {

    private final Logger logger =  LoggerFactory.getLogger(this.getClass());
    // Check how many users are connecting to our server
    // AtomicInteger is provided by Java for process safety
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

    public static final ConcurrentHashMap<String, WebSocketService> WEBSOCKET_MAP = new ConcurrentHashMap<>();

    private Session session;

    private String sessionId;

    private Long userId;

    private static ApplicationContext APPLICATION_CONTEXT;

    public static void setApplicationContext(ApplicationContext applicationContext){
        WebSocketService.APPLICATION_CONTEXT = applicationContext;
    }

    @OnOpen
    public void openConnection(Session session, @PathParam("token") String token){
        try {
            this.userId = TokenUtil.verifyToken(token);
        } catch (Exception ignored){}
        this.sessionId = session.getId();
        this.session = session;
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
            WEBSOCKET_MAP.put(sessionId, this);
        } else {
            WEBSOCKET_MAP.put(sessionId, this);
            ONLINE_COUNT.getAndIncrement();
        }
        logger.info("User connection successful：" + sessionId + ", current online user number is：" + ONLINE_COUNT.get());
        try {
            this.sendMessage("0");
        } catch (Exception e){
            logger.error("Connection exception.");
        }
    }

    @OnClose
    public void closeConnection(){
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
            ONLINE_COUNT.getAndDecrement();
        }
        logger.info("User exit：" + sessionId + ", current online user number is：" + ONLINE_COUNT.get());
    }

    @OnMessage
    public void onMessage(String message) {
        logger.info("User information：" + sessionId + ", message：" + message);
        if (!StringUtil.isNullOrEmpty(message)) {
            try {
                // Broadcast information
                for (Map.Entry<String, WebSocketService> entry : WEBSOCKET_MAP.entrySet()) {
                    WebSocketService webSocketService = entry.getValue();
                    DefaultMQProducer danmusProducer = (DefaultMQProducer)APPLICATION_CONTEXT.getBean("danmusProducer");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", message);
                    jsonObject.put("sessionId", webSocketService.getSessionId());
                    Message msg = new Message(UserMomentsConstant.TOPIC_DANMUS, jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
                    RocketMQUtil.asyncSendMsg(danmusProducer, msg);
                }
                if (this.userId != null) {
                    // Save Danmu to database
                    Danmu danmu = JSONObject.parseObject(message, Danmu.class);
                    danmu.setUserId(userId);
                    danmu.setCreateTime(new Date());
                    DanmuService danmuService = (DanmuService)APPLICATION_CONTEXT.getBean("danmuService");
                    danmuService.asyncAddDanmu(danmu);
                    // Save Danmu to redis
                    danmuService.addDanmusToRedis(danmu);
                }
            } catch (Exception e) {
                logger.error("Danmu reception has problem.");
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Throwable error){
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    // Set interval time，for example：5 seconds
    @Scheduled(fixedRate=5000)
    private void noticeOnlineCount() throws IOException {
        for (Map.Entry<String, WebSocketService> entry : WebSocketService.WEBSOCKET_MAP.entrySet()) {
            WebSocketService webSocketService = entry.getValue();
            if (webSocketService.session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("onlineCount", ONLINE_COUNT.get());
                jsonObject.put("msg", "Current online user number is: " + ONLINE_COUNT.get());
                webSocketService.sendMessage(jsonObject.toJSONString());
            }
        }
    }

    public Session getSession() {
        return session;
    }

    public String getSessionId() {
        return sessionId;
    }
}
