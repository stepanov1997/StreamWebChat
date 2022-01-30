package com.swc.runner;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("runner")
public class RunnerProperties {

    /**
     * Directory where individual pod logs should be copied to
     */
    protected String logsArchiveDirectory;
    protected AfterAll afterAll;
    protected Kubernetes kubernetes;

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
        private String dockerconfigjson = "something";
        /**
         * Kubernetes namespace
         */
        private String namespace;
    }
}
