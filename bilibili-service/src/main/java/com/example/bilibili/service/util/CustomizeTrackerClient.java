package com.example.bilibili.service.util;

import com.github.tobato.fastdfs.domain.conn.TrackerConnectionManager;
import com.github.tobato.fastdfs.domain.fdfs.StorageNode;
import com.github.tobato.fastdfs.domain.fdfs.StorageNodeInfo;
import com.github.tobato.fastdfs.service.DefaultTrackerClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// Fixed the problem that new version of fastFDFS is retrieving Storage port as 0
@Primary
@Component("customizeTrackerClient")
public class CustomizeTrackerClient extends DefaultTrackerClient {

    private static final String OVERRIDE_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 23000;

    public CustomizeTrackerClient(TrackerConnectionManager trackerConnectionManager) {
        super();
    }

    public StorageNode getStoreStorage() {
        StorageNode node = super.getStoreStorage();
        rewrite(node);
        return node;
    }

    public StorageNode getStoreStorage(String groupName) {
        StorageNode node = super.getStoreStorage(groupName);
        rewrite(node);
        return node;
    }

    public StorageNodeInfo getFetchStorage(String groupName, String filename) {
        StorageNodeInfo info = super.getFetchStorage(groupName, filename);
        rewrite(info);
        return info;
    }

    public StorageNodeInfo getUpdateStorage(String groupName, String filename) {
        StorageNodeInfo info = super.getUpdateStorage(groupName, filename);
        rewrite(info);
        return info;
    }

    private void rewrite(StorageNode node) {
        node.setIp(OVERRIDE_HOST);
        if (node.getPort() == 0) {
            node.setPort(DEFAULT_PORT);
        }
    }

    private void rewrite(StorageNodeInfo info) {
        info.setIp(OVERRIDE_HOST);
        if (info.getPort() <= 0) {
            info.setPort(DEFAULT_PORT);
        }
    }
}
