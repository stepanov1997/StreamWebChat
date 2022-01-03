package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class ChatService(val kafkaTemplate: KafkaTemplate<String?, String?>, val webClient: WebClient, val gson: Gson) {

    fun sendMessage(message: Message): String? {
        val listenableFuture = kafkaTemplate.send("messages", gson.toJson(message))
        val get = listenableFuture.get()
        println(get.producerRecord.key())
        return get.producerRecord.value()
    }

    fun getMessages(): Flux<Message> {
        return webClient.get()
            .uri("/stream")
            .exchangeToFlux { it.bodyToFlux(Message::class.java) }
    }
}
