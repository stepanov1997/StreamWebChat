package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import com.swc.repository.ChatRepository
import com.swc.repository.UserRepository
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.springframework.http.codec.ServerSentEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.io.Serializable
import java.time.Duration
import javax.annotation.PostConstruct


@Service
class ChatService(
    val kafkaTemplate: KafkaTemplate<String, String>,
    val gson: Gson,
    val kafkaConsumer: KafkaConsumer<String, String>,
    val userRepository: UserRepository,
    val chatRepository: ChatRepository
) {

    @PostConstruct
    fun init() {
        kafkaConsumer.subscribe(listOf("messages"))
    }

    fun retrieveOldMessages(senderUsername: String, receiverUsername: String): Flux<Message> =
        Flux.fromStream {
            chatRepository
                .findAll()
                .filter {
                    it.senderUsername == senderUsername && it.receiverUsername == receiverUsername ||
                    it.senderUsername == receiverUsername && it.receiverUsername == senderUsername
                }.stream()
        }

    fun retrieveNewMessages(senderUsername: String, receiverUsername: String): Flux<Message> =
        Flux.create {
            while (true) {
                val records: ConsumerRecords<String, String> = kafkaConsumer.poll(Duration.ofMillis(100))
                for (record in records) {
                    val message: Message = gson.fromJson(record.value(), Message::class.java)
                    if (message.senderUsername == senderUsername && message.receiverUsername == receiverUsername ||
                        message.senderUsername == receiverUsername && message.receiverUsername == senderUsername
                    ) {
                        it.next(message)
                    }
                }
            }
        }

    fun getMessages(senderUsername: String, receiverUsername: String): Flux<ServerSentEvent<Message>> =
        Flux.concat(
            retrieveOldMessages(senderUsername, receiverUsername)
                .map { ServerSentEvent.builder(it).build() }
                .doOnError(Throwable::printStackTrace),
            retrieveNewMessages(senderUsername, receiverUsername)
                .map { ServerSentEvent.builder(it).build() }
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
