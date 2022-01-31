package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import com.swc.repository.ChatRepository
import com.swc.repository.UserRepository
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.ServerSentEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.io.Serializable

@Service
class ChatService(
    val kafkaTemplate: KafkaTemplate<String?, String?>,
    val webClient: WebClient,
    val gson: Gson,
    val userRepository: UserRepository,
    val chatRepository: ChatRepository
) {

    fun getMessages(senderUsername: String, receiverUsername: String): Flux<ServerSentEvent<Message>> = Flux.concat(
        webClient.get()
            .uri {
                it.path("/messages")
                    .queryParam("senderUsername", senderUsername)
                    .queryParam("receiverUsername", receiverUsername)
                    .build()
            }
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<Message>>() {})
            .doOnError(Throwable::printStackTrace),
        webClient.get()
            .uri {
                it.path("/stream")
                    .queryParam("senderUsername", senderUsername)
                    .queryParam("receiverUsername", receiverUsername)
                    .build()
            }
            .retrieve()
            .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<Message>>() {})
            .doOnError(Throwable::printStackTrace)
    )

    fun sendMessage(message: Message?): Message? {
        return try {
            kafkaTemplate.send("messages", gson.toJson(message))
            message
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getConversationsForUser(username: String): List<Map<String, Serializable>> {
        return userRepository
            .findAll()
            .filter { username != it.username }
            .map { otherUser ->
                Pair(
                    otherUser,
                    chatRepository
                        .findAll()
                        .filter { it.senderUsername == username || it.receiverUsername == username }
                        .maxByOrNull { it.timestamp }
                )
            }
            .map {
                mapOf(
                    "userId" to it.first.id,
                    "username" to it.first.username,
                    "isOnline" to it.first.isOnline,
                    "exists" to (it.second != null),
                    "timestamp" to (it.second?.timestamp ?: 0),
                    "lastMessage" to (it.second?.text ?: "")
                )
            }
            .toList()
    }
}
