package com.swc.launcher

import com.swc.repository.UserRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.kafka.annotation.EnableKafka
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.cert.Certificate

@SpringBootApplication(scanBasePackages = ["com.swc"])
@EnableMongoRepositories(basePackages = ["com.swc"])
@EnableKafka
class KotlinRestApplication

fun main(args: Array<String>) {
    runApplication<KotlinRestApplication>(*args)
}
