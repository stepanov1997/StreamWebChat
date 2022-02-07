package com.swc.launcher

import SwcProperties
import com.swc.it.ITLauncherTest
import com.swc.services.*
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration


@ITLauncherTest(components = [
    ZookeeperService::class,
    KafkaService::class,
    MongoService::class,
    TransferAppService::class,
    KotlinRestService::class
])
@SpringBootTest
@Component
@EnableConfigurationProperties(SwcProperties::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = [ITFConfiguration::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SwcIntegrationTest
