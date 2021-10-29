package com.swc.service

import jdk.jfr.consumer.EventStream
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux
import reactor.kotlin.extra.retry.retryRandomBackoff
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.time.Duration
import java.util.*

@Service
class ChatService(val kafkaTemplate: KafkaTemplate<String?, String?>, val webClient: WebClient) {

    fun sendMessage(message: String) {
        val listenableFuture = kafkaTemplate.send("messages", message)
        val get = listenableFuture.get()
        println(get.producerRecord.key())
    }

    fun getMessages(): Flux<String> {
        val exchangeToFlux = webClient.get()
            .uri("/stream")
            .exchangeToFlux { it.bodyToFlux(String::class.java) }

        return exchangeToFlux
    }
}
