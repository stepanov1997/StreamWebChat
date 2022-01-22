package com.swc.service

import com.google.gson.Gson
import com.swc.model.MessageRemoteModel
import com.swc.model.MessageUserModel
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
class ChatService(val kafkaTemplate: KafkaTemplate<String?, String?>,
                  val webClient: WebClient,
                  val gson: Gson,
                  val userRepository: UserRepository,
                  val chatRepository: ChatRepository) {

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

    fun getConversationsForUser(username: String): List<Map<String, Serializable>> {
           return userRepository
                .findAll()
                .filter { username != it.username }
                .map { otherUser -> Pair(
                        otherUser,
                        chatRepository
                            .findAll()
                            .map { it.toUserModel(userRepository)}
                            .filter { it.senderUsername == username || it.receiverUsername == username }
                            .maxByOrNull { it.timestamp }
                )}
               .map { mapOf(
                    "userId" to it.first.id,
                    "username" to it.first.username,
                    "isOnline" to it.first.isOnline,
                    "exists" to (it.second != null),
                    "timestamp" to (it.second?.timestamp ?: 0),
                    "lastMessage" to (it.second?.text ?: "")
                ) }
                .toList()
    }
}
