package com.swc.launcher

import com.google.gson.Gson
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebConfiguration : WebMvcConfigurer {
    override fun addCorsMappings(registry: org.springframework.web.servlet.config.annotation.CorsRegistry) {
        registry.addMapping("/**").allowedMethods("*").allowedOrigins("*")
    }

    @Bean
    fun gson() = Gson()

    @Bean
    fun topicExample(): NewTopic? {
        return TopicBuilder.name("messages")
            .partitions(1)
            .replicas(1)
            .build()
    }

    @Bean
    fun kafkaConsumer(kafkaProperties: KafkaProperties): KafkaConsumer<String, String> {
        return KafkaConsumer<String, String>(kafkaProperties.buildConsumerProperties())
    }
}
