package com.swc.runner;

import com.swc.it.ITLauncherProperties;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @author Kristijan Stepanov <kristijan.stepanov@deepintent.com>
 * @version 1.0
 * Service for manipulating with Kubernetes cluster.
 */
@Service
@EnableConfigurationProperties(RunnerProperties.class)
@Slf4j
public class KubernetesService {

    private final RunnerProperties properties;
    /**
     * Client of Kubernetes cluster
     */
    private final KubernetesClient client;
    /**
     * Started ITF components and corresponding Thread object for each ITF component
     */
    private final List<FutureService> futureServices;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public KubernetesService(RunnerProperties properties) {
        this.properties = properties;
        Config config = Config.fromKubeconfig(properties.getKubernetes().getConfig());
        config.setNamespace(properties.getKubernetes().getNamespace());
        client = new DefaultKubernetesClient(config);
        futureServices = new ArrayList<>();
    }

    /**
     * Method for loading Kubernetes yaml file as deployment, service or job.
     *
     * @param macros      Map with entries on the basis of which key will be replaced by a value from yaml content.
     * @param yamlContent InputStream of yaml file
     * @return list of metadata (deployment, service or job) created by loading data from yaml file
     */
    private List<HasMetadata> load(Map<String, String> macros, String yamlContent) {
        Dotenv dotenv = Dotenv.load();

        if (macros != null) {
            for (Map.Entry<String, String> entry : macros.entrySet()) {
                yamlContent = yamlContent.replace("{{ " + entry.getKey() + " }}", entry.getValue());
            }
        }
        if(dotenv != null) {
            yamlContent = replace(dotenv.entries(), yamlContent);
        }
        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        return client.load(inputStream).createOrReplace();
    }

    /**
     * Method for deleting Kubernetes namespace selected from configuration.
     */
    public void deleteNamespace() {
        client.namespaces().withName(client.getNamespace()).delete();
    }

    /**
     * Method for creating Kubernetes namespace if it doesn't exist.
     */
    public void ensureNamespaceExists() {
        io.fabric8.kubernetes.client.dsl.Resource<Namespace> namespace = client.namespaces().withName(client.getNamespace());
        if (namespace.get() == null) {

            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(client.getNamespace()).endMetadata().build();
            client.namespaces().create(ns);

            Secret secret = new SecretBuilder()
                    .addToData(".dockerconfigjson", properties.getKubernetes().getDockerconfigjson())
                    .withType("kubernetes.io/dockerconfigjson")
                    .withNewMetadata()
                    .withName("platform-registry")
                    .withNamespace(client.getNamespace())
                    .endMetadata()
                    .build();
            client.secrets().create(secret);
        }
    }

    /**
     * Method to get all pods resources.
     */
    public Stream<PodResource<Pod>> getPods() {
        return client.pods().list().getItems().stream()
                .map(pod -> client.pods().withName(pod.getMetadata().getName()));
    }

    public Stream<String> getDeployments() {
        return client.apps().deployments().list().getItems().stream()
                .map(deployment -> deployment.getMetadata().getName());
    }

    /**
     * Method to get a Harbor docker image tag of ITF component
     * WARNING:
     * Environment variable should be in form NAME_OF_SERVICE_DOCKER_TAG=tag_name
     * NAME_OF_SERVICE should be changed with name of service, and tag_name with tag name.
     *
     * @param serviceValue Name of service
     * @return Map of one entry with key "tag" and appropriate value found in environment variables.
     */
    private static Map<String, String> findEnvironmentVariablesForDockerTag(String serviceValue) {
        UnaryOperator<String> cleanName = name -> name.toUpperCase().replace("-", "_").replace(".", "_");
        Optional<String> tag = System.getenv().entrySet().stream()
                .filter(e -> cleanName.apply(e.getKey()).equals(cleanName.apply(serviceValue) + "_DOCKER_TAG"))
                .map(Map.Entry::getValue)
                .findFirst();
        return Map.of("tag", tag.orElse("master"));
    }

    /**
     * Method for waiting for dependent service (component) of forwarded service.
     *
     * @param service service
     */
    private void wait(KubernetesDeployment service) {
        lock.readLock().lock();
        List<FutureService> dependencies = futureServices.stream()
                .filter(proc -> service.getDependencies().anyMatch(dep -> dep.isInstance(proc.service)))
                .toList();
        lock.readLock().unlock();
        if (dependencies.isEmpty()) {
            return;
        }

        log.info("{} scheduled for deployment after {} are deployed", service.getName(), dependencies.stream().map(s -> s.service.getName()).toList());
        for (FutureService dependency : dependencies) {
            try {
                dependency.future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RunnerException(service.getName() + " was interrupted while waiting for " + dependency.service.getName() + " to finish deployment", e);
            } catch (ExecutionException e) {
                throw new RunnerException(service.getName() + " was waiting for " + dependency.service.getName() + " to finish deployment, but it threw an exception", e);
            }
        }
    }

    private void deploy(KubernetesDeployment service) {
        wait(service);
        Map<String, String> macros = new HashMap<>();
        macros.put("namespace", client.getNamespace());
        macros.putAll(findEnvironmentVariablesForDockerTag(service.getName()));
        log.info("{} is deploying", service.getName());
        try {
            service.beforeDeploy();
        } catch (Exception e) {
            throw new RunnerException("Error in 'beforeDeploy' of " + service.getName(), e);
        }
        load(macros, service.getDeploymentYaml());
        log.info("{} is waiting to become healthy", service.getName());
        healthcheck(service);
        try {
            service.afterDeploy();
        } catch (Exception e) {
            throw new RunnerException("Error in 'afterDeploy' of " + service.getName(), e);
        }
        log.info("{} finished deploying", service.getName());
    }

    private void healthcheck(KubernetesDeployment service) {
        Duration healthcheckDuration = Duration.ofSeconds(service.getHealthcheckFailureThreshold() * service.getHealthcheckPeriodSeconds());
        LocalDateTime expiration = LocalDateTime.now().plus(healthcheckDuration);
        while (LocalDateTime.now().isBefore(expiration)) {
            try {
                if (service.healthcheck()) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RunnerException(service.getName() + " healthcheck was interrupted", e);
            } catch (Exception e) {
                log.debug("{} healthcheck thrown an exception: {}", service.getName(), e);
            }
            try {
                TimeUnit.SECONDS.sleep(service.getHealthcheckPeriodSeconds());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RunnerException(service.getName() + " healthcheck was interrupted while sleeping", e);
            }
        }
        throw new RunnerException(service.getName() + " failed to become healthy after " + healthcheckDuration.getSeconds() + " seconds");
    }

    /**
     * Method for starting every service (ITF component) which was added in scheduler.
     */
    public void startAndWaitToComplete(List<KubernetesDeployment> services) {
        if (services.isEmpty()) {
            return;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(services.size(), new ThreadFactory() {
            private final AtomicInteger c = new AtomicInteger(0);

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "kube-deploy-thread-" + c.incrementAndGet());
            }
        });

        lock.writeLock().lock();
        for (KubernetesDeployment service : services) {
            futureServices.add(new FutureService(executorService.submit(() -> deploy(service)), service));
        }
        lock.writeLock().unlock();

        try {
            for (FutureService futureService : futureServices) {
                futureService.future.get();
            }
            executorService.shutdown();
            while (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.debug("Waiting for all services to finish deployment");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            throw new RunnerException("Scheduler was interrupted", e);
        } catch (ExecutionException e) {
            executorService.shutdownNow();
            throw new RunnerException("One of the services failed to deploy", e);
        }
    }

    private record FutureService(Future<?> future, KubernetesDeployment service) {
    }

    public String replace(Set<DotenvEntry> entries, String value) {
        while (value.contains("${")) {
            for (DotenvEntry entry : entries) {
                value = value.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }
        return value;
    }
}
