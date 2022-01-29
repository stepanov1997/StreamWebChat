package com.it;

import com.it.engine.ITEngine;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.logging.log4j.Level.INFO;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

@RequiredArgsConstructor
@Component
@EnableConfigurationProperties(ITProperties.class)
public class IT {

    private final ITProperties properties;
    private final ITEngine testEngine;

    public boolean execute() {
        LoggingListener loggingListener = LoggingListener.forJavaUtilLogging(java.util.logging.Level.FINE);
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();

        LauncherConfig config = LauncherConfig.builder()
                .enableTestEngineAutoRegistration(false)
                .enableTestExecutionListenerAutoRegistration(false)
                .addTestEngines(testEngine)
                .addTestExecutionListeners(loggingListener, summaryListener)
                .build();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage(properties.getTestsPackage()))
                .build();

        LauncherFactory.create(config).execute(request);

        TestExecutionSummary summary = summaryListener.getSummary();
        summary.printTo(IoBuilder.forLogger(IT.class).setLevel(INFO).buildPrintWriter());
        summary.printFailuresTo(IoBuilder.forLogger(IT.class).setLevel(Level.WARN).buildPrintWriter(), 25);

        return summary.getTotalFailureCount() == 0;
    }
}

