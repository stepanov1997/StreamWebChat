package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.time.Duration
import java.util.*

@Service
class ChatService(val kafkaTemplate: KafkaTemplate<String?, String?>, val webClient: WebClient, val gson: Gson) {

    fun sendMessage(message: Message) {
        val listenableFuture = kafkaTemplate.send("messages", gson.toJson(message))
        val get = listenableFuture.get()
        println(get.producerRecord.key())
    }

    fun getMessages(): Flux<Message> {

        return webClient.get()
            .uri("/stream")
            .exchangeToFlux { it.bodyToFlux(Message::class.java) }
    }
}
