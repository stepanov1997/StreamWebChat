package com.swc.launcher

import com.google.gson.Gson
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.SenderOptions


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
    fun reactiveKafkaProducerOptions(properties: KafkaProperties): SenderOptions<String, String> {
        val props = properties.buildProducerProperties()
        return SenderOptions.create(props)
    }

    @Bean
    fun kafkaReceiverOptions(properties: KafkaProperties): ReceiverOptions<String, String> {
        val basicReceiverOptions: ReceiverOptions<String, String> =
            ReceiverOptions.create(properties.buildConsumerProperties())
        return basicReceiverOptions.subscription(listOf("messages"))
    }
}
