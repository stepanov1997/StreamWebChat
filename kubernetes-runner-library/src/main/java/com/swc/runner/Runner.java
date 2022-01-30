package com.swc.runner;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public record Runner(KubernetesService kubernetesService) implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        Runner.applicationContext = applicationContext;
    }

    public boolean run() {
        kubernetesService.ensureNamespaceExists();
        List<KubernetesDeployment> services = applicationContext.getBeansOfType(KubernetesDeployment.class).values().stream()
                .filter(service -> service.getClass().isAnnotationPresent(RunnerComponent.class))
                .toList();
        System.out.println("Found " + services.size() + " services to run");
        kubernetesService.startAndWaitToComplete(services);
        return true;
    }
}
