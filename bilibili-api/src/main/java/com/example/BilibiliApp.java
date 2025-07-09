package com.example;

import com.example.bilibili.service.util.FastDFSUtil;
import com.example.bilibili.service.websocket.WebSocketService;
import com.github.tobato.fastdfs.FdfsClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@Import(FdfsClientConfig.class)
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class BilibiliApp {
    public static void main( String[] args ) {
        ApplicationContext app = SpringApplication.run(BilibiliApp.class, args);
        WebSocketService.setApplicationContext(app);
    }
}
