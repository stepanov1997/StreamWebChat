package com.it;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("IT")
public class ITProperties {
    /**
     * Specifies root package which will be scanned for test classes
     */
    private String testsPackage;
    /**
     * Directory where individual pod logs should be copied to
     */
    private String logsArchiveDirectory;
    private AfterAll afterAll;
    private Kubernetes kubernetes;

    @Data
    public static class AfterAll {
        /**
         * Should kubernetes test namespace be deleted after all tests are complete
         */
        private boolean deleteNamespace = true;
    }

    @Data
    public static class Kubernetes {
        /**
         * Kubernetes cluster config
         */
        private String config;
        /**
         * Docker credentials
         *
         * @see <a href="https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/#registry-secret-existing-credentials">kubernetes documentation</a>
         */
        private String dockerconfigjson;
        /**
         * Kubernetes namespace
         */
        private String namespace;
    }
}
