package com.swc.launcher

import SwcProperties
import com.mongodb.MongoClient
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.checkerframework.checker.units.qual.A
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import java.util.*


@Configuration
@EnableConfigurationProperties(SwcProperties::class)
class ITFConfiguration {

    @Bean
    fun mongoClient(swcProperties: SwcProperties): MongoClient {
        return MongoClient(swcProperties.mongo.host, swcProperties.mongo.port)
    }

    @Bean
    fun kafkaConsumer(swcProperties: SwcProperties): ConsumerFactory<String, String> {
        val props = mutableMapOf<String, Any>()
        props["bootstrap.servers"] = "${swcProperties.kafka.host}:${swcProperties.kafka.port}"
        props["auto.offset.reset"] = "latest"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        return DefaultKafkaConsumerFactory(props)
    }
}
