package com.swc.launcher

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebConfiguration : WebMvcConfigurer {
    override fun addCorsMappings(registry: org.springframework.web.servlet.config.annotation.CorsRegistry) {
        registry.addMapping("/**").allowedMethods("*").allowedOrigins("*")
    }

    @Bean
    fun topicExample(): NewTopic? {
        return TopicBuilder.name("messages")
            .partitions(1)
            .build()
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.create("http://localhost:5000")
    }
}