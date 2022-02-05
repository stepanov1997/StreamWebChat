package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import com.swc.repository.ChatRepository
import com.swc.repository.UserRepository
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.http.codec.ServerSentEvent
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.SenderOptions
import java.io.Serializable
import java.time.Duration
import java.util.*
import kotlin.random.Random


@Service
class ChatService(
    val receiverOptions: ReceiverOptions<String, String>,
    val senderOptions: SenderOptions<String, String>,
    val gson: Gson,
    val userRepository: UserRepository,
    val chatRepository: ChatRepository
) {

    fun retrieveOldMessages(senderUsername: String, receiverUsername: String): Flux<Message> =
        Flux.fromStream {
            chatRepository
                .findAll()
                .filter {
                    it.senderUsername == senderUsername && it.receiverUsername == receiverUsername ||
                    it.senderUsername == receiverUsername && it.receiverUsername == senderUsername
                }.stream()
        }

    fun getMessages(senderUsername: String, receiverUsername: String, groupId: String): Flux<ServerSentEvent<Message>> =
        Flux.concat(
            retrieveOldMessages(senderUsername, receiverUsername)
                .map { ServerSentEvent.builder(it).build() }
                .doOnError(Throwable::printStackTrace),
            ReactiveKafkaConsumerTemplate(receiverOptions.consumerProperty("group.id", groupId))
                .receiveAutoAck()
                .map { gson.fromJson(it.value(), Message::class.java) }
                .filter { it.senderUsername == senderUsername && it.receiverUsername == receiverUsername ||
                        it.senderUsername == receiverUsername && it.receiverUsername == senderUsername }
                .map { ServerSentEvent.builder(it).build() }
                .doOnError(Throwable::printStackTrace)
        )

    fun sendMessage(message: Message?): Message? {
        return try {
            val json = gson.toJson(message)
            val producer = ReactiveKafkaProducerTemplate(senderOptions)
            producer.send("messages", Objects.hash(json).toString(), json)
                .doOnSuccess {
                    println("Message sent to Kafka ${message?.text} at ${message?.timestamp}")
                }
                .subscribe()
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
