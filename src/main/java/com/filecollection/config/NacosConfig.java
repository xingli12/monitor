package com.filecollection.config;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class NacosConfig {
    // Nacos 配置通过 @ConfigurationProperties 自动加载
    // @RefreshScope 支持配置热更新
}
