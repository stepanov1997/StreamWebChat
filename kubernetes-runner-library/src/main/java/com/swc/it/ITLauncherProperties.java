package com.swc.it;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("it")
public class ITLauncherProperties {
    /**
     * Specifies root package which will be scanned for test classes
     */
    private String testsPackage;
}
