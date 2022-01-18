package com.swc.service

import com.google.gson.Gson
import com.swc.model.MessageRemoteModel
import com.swc.model.MessageUserModel
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.ServerSentEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class ChatService(val kafkaTemplate: KafkaTemplate<String?, String?>, val webClient: WebClient, val gson: Gson) {

    fun getMessages(senderId: Int, receiverId: Int): Flux<ServerSentEvent<MessageUserModel>> = Flux.concat(
        webClient.get()
            .uri {
                it.path("/messages")
                    .queryParam("senderId", senderId)
                    .queryParam("receiverId", receiverId)
                    .build()
            }
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<MessageUserModel>>() {})
            .doOnError(Throwable::printStackTrace),
        webClient.get()
            .uri {
                it.path("/stream")
                    .queryParam("senderId", senderId)
                    .queryParam("receiverId", receiverId)
                    .build()
            }
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<MessageUserModel>>() {})
            .doOnError(Throwable::printStackTrace)
    )

    fun sendMessage(message: MessageRemoteModel?): MessageRemoteModel? {
        try {
            val listenableFuture = kafkaTemplate.send("messages", gson.toJson(message))
            listenableFuture.get()
            return message;
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
