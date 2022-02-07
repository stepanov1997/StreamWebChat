package com.swc.runner;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
@EnableConfigurationProperties(RunnerProperties.class)
@Slf4j
public record LogsService(RunnerProperties runnerProperties) {

    public void copyLogsFromPodsToArchiveFolder(Stream<PodResource<Pod>> pod) {
        File logsDir = new File(runnerProperties.getLogsArchiveDirectory()
                + File.separator
                + System.currentTimeMillis()
        );
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            logsDir.mkdirs();
        }

        log.info("Archiving logs from pods:");
        pod.forEach(pod1 -> copyLogsFromPodToArchiveFolder(pod1, logsDir));
    }

    private void copyLogsFromPodToArchiveFolder(PodResource<Pod> pod, File logsDir) {
        String podName = pod.get().getMetadata().getName();
        String logFile = logsDir.getPath() + File.separator + podName + ".log";
        try {
            Files.writeString(Path.of(logFile), pod.getLog(true));
            log.info("\tPod {} logs are copied to archive directory.", podName);
        } catch (IOException e) {
            log.warn("\tError copying logs from pod {}.", podName, e);
        }
    }
}
