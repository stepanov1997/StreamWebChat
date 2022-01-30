package com.swc.runner;

import java.util.stream.Stream;

public abstract class KubernetesDeployment {

    private static final long DEFAULT_HEALTHCHECK_FAILURE_THRESHOLD = 60L;
    private static final long DEFAULT_HEALTHCHECK_PERIOD_SECONDS = 1L;

    public static String getName(Class<? extends KubernetesDeployment> clazz) {
        return clazz.getAnnotation(RunnerComponent.class).value();
    }

    public final String getName() {
        return getName(getClass());
    }

    public static Stream<Class<? extends KubernetesDeployment>> getDependencies(Class<? extends KubernetesDeployment> clazz) {
        return Stream.of(clazz.getAnnotation(RunnerComponent.class).dependsOn());
    }

    public final Stream<Class<? extends KubernetesDeployment>> getDependencies() {
        return getDependencies(getClass());
    }

    public void beforeDeploy() throws Exception {
    }

    public abstract String getDeploymentYaml();

    public void afterDeploy() throws Exception {
    }

    public long getHealthcheckFailureThreshold() {
        return DEFAULT_HEALTHCHECK_FAILURE_THRESHOLD;
    }

    public long getHealthcheckPeriodSeconds() {
        return DEFAULT_HEALTHCHECK_PERIOD_SECONDS;
    }

    public boolean healthcheck() throws Exception {
        return true;
    }
}
