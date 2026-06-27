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
    
    private static final long DEFAULT_RATE_LIMIT = 10 * 1024 * 1024; // 10MB/s
    private static final String DEFAULT_FILE_PATTERN = "*.*";
    private static final String DEFAULT_CONFLICT_STRATEGY = "OVERWRITE";
    private static final String TYPE_FTP = "FTP";
    private static final String TYPE_CIFS = "CIFS";
    private static final String TYPE_LOCAL = "LOCAL";
    private static final int DEFAULT_RUNNING_TASK_EXPIRY_HOURS = 2;

    private long rateLimit = DEFAULT_RATE_LIMIT;
    private int runningTaskExpiryHours = DEFAULT_RUNNING_TASK_EXPIRY_HOURS;
    private List<UpstreamConfig> upstreams = new ArrayList<>();
    private List<DownstreamConfig> downstreams = new ArrayList<>();
    
    public boolean hasUpstreams() {
        return upstreams != null && !upstreams.isEmpty();
    }
    
    public boolean hasDownstreams() {
        return downstreams != null && !downstreams.isEmpty();
    }
    
    @Data
    public static class UpstreamConfig {
        private String name;
        private String type;
        private String host;
        private int port;
        private String shareName;
        private String username;
        private String password;
        private String path;
        private String filePattern = DEFAULT_FILE_PATTERN;
        
        public boolean isFtp() {
            return TYPE_FTP.equalsIgnoreCase(type);
        }
        
        public boolean isCifs() {
            return TYPE_CIFS.equalsIgnoreCase(type);
        }
        
        public boolean isLocal() {
            return TYPE_LOCAL.equalsIgnoreCase(type);
        }
    }
    
    @Data
    public static class DownstreamConfig {
        private String name;
        private String type;
        private String path;
        private String conflictStrategy = DEFAULT_CONFLICT_STRATEGY;
        
        public boolean isOverwrite() {
            return "OVERWRITE".equalsIgnoreCase(conflictStrategy);
        }
    }
}
