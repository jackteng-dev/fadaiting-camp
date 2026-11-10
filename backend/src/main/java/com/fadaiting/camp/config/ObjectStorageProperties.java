package com.fadaiting.camp.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 僅使用明確指定的 Camp S3 憑證，不讀 AWS 預設憑證鏈。 */
@Data
@Component
@ConfigurationProperties(prefix = "camp.storage")
public class ObjectStorageProperties {
    private String endpoint = "http://localhost:9000";
    @ToString.Exclude private String accessKey = "";
    @ToString.Exclude private String secretKey = "";
    private String region = "us-east-1";
    private String publicBasePath = "/objects";
}
