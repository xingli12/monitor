package com.filecollection.config;

import com.filecollection.core.FileCopier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileCopierConfig {
    
    @Bean
    public FileCopier fileCopier(@Value("${file-collection.rate-limit:10485760}") long rateLimit) {
        return new FileCopier(rateLimit);
    }
}
