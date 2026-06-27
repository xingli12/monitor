package com.filecollection.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "file-collection")
public class FileCollectionProperties {
    
    private long rateLimit = 10 * 1024 * 1024; // 默认 10MB/s
    private List<UpstreamConfig> upstreams = new ArrayList<>();
    private List<DownstreamConfig> downstreams = new ArrayList<>();
    
    @Data
    public static class UpstreamConfig {
        private String name;
        private String type; // FTP, CIFS, LOCAL
        private String host;
        private int port;
        private String shareName;
        private String username;
        private String password;
        private String path;
        private String filePattern = "*.*";
    }
    
    @Data
    public static class DownstreamConfig {
        private String name;
        private String type; // LOCAL
        private String path;
        private String conflictStrategy = "OVERWRITE";
    }
}
