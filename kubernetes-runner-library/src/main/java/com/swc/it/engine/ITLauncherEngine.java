package com.swc.it.engine;

import com.swc.runner.*;
import com.swc.it.ITLauncherTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.engine.*;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom IT test engine to define beforeAll and afterAll events for IT integration tests.
 * This test engine is wrapped with {@link JupiterTestEngine}.
 */
@RequiredArgsConstructor
@Component
@EnableConfigurationProperties(RunnerProperties.class)
public class ITLauncherEngine implements TestEngine, ApplicationContextAware {

    private final RunnerProperties runnerProperties;
    private final KubernetesService kubernetesService;
    private final LogsService logsService;
    private final JupiterTestEngine testEngine = new JupiterTestEngine();
    private ApplicationContext applicationContext;

    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Method to return test engine ID.
     *
     * @return IT Test engine ID created by class name.
     */
    @Override
    public String getId() {
        return ITLauncherEngine.class.getSimpleName();
    }

    /**
     * @see JupiterTestEngine discover method
     */
    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        return testEngine.discover(discoveryRequest, uniqueId);
    }

    /**
     * Method to execute IT integration tests.
     *
     * @param request Provides a single {@link TestEngine} access to the information necessary to
     *                execute its tests.
     */
    @Override
    public void execute(ExecutionRequest request) {
        deployServices(request.getRootTestDescriptor().getDescendants());
        testEngine.execute(request);
        cleanup();
    }

    /**
     * Method to define event which will be triggered before all IT tests.
     *
     * @param testDescriptors testDescriptors which describe tests.
     */
    private void deployServices(Set<? extends TestDescriptor> testDescriptors) {
        kubernetesService.ensureNamespaceExists();
        List<String> deployments = kubernetesService.getDeployments().toList();

        Set<Class<? extends KubernetesDeployment>> components = testDescriptors.stream()
                .map(testDescriptor -> testDescriptor.getSource().orElseThrow(() ->
                        new RunnerException("Can't determine test source for " + testDescriptor.getDisplayName())))
                .map(testSource -> {
                    if (testSource instanceof ClassSource classSource) {
                        return classSource.getJavaClass();
                    }
                    return null;
                }).filter(Objects::nonNull)
                .flatMap(testClass -> Arrays.stream(
                        AnnotationUtils.findAnnotation(testClass, ITLauncherTest.class).map(ITLauncherTest::components).orElseThrow(() ->
                                new RunnerException("Test class " + testClass + " must be annotated with " + ITLauncherTest.class.getName()))))
                .filter(component -> !deployments.contains(KubernetesDeployment.getName(component)))
                .collect(Collectors.toSet());

        List<KubernetesDeployment> services = applicationContext.getBeansOfType(KubernetesDeployment.class).values().stream()
                .filter(service -> components.stream().anyMatch(item -> service.getClass().equals(item)))
                .toList();

        kubernetesService.startAndWaitToComplete(services);
    }

    /**
     * Method to define event which will be triggered after all IT tests.
     */
    private void cleanup() {
        logsService.copyLogsFromPodsToArchiveFolder(kubernetesService.getPods());
        if (runnerProperties.getAfterAll().isDeleteNamespace()) {
            kubernetesService.deleteNamespace();
        }
    }
}
