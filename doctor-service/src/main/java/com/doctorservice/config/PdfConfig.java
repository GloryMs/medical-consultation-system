package com.doctorservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.pdf")
@Getter
@Setter
public class PdfConfig {
    private Storage storage = new Storage();
    private String baseUrl;
    private Integer maxSizeMb;
    private String pageSize;
    private Boolean compress;
    private Integer fontSize;
    private Integer headerFontSize;
    private Integer titleFontSize;
    private String filenamePattern;
    
    @Getter
    @Setter
    public static class Storage {
        private String basePath;
        private String tempDir;
    }
}