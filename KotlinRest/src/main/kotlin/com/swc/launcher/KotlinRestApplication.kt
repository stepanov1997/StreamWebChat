package com.swc.launcher

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication(scanBasePackages = ["com.swc"])
@EnableKafka
class KotlinRestApplication

fun main(args: Array<String>) {
    runApplication<KotlinRestApplication>(*args)
}
