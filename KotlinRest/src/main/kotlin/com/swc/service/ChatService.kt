package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import com.swc.repository.ChatRepository
import com.swc.repository.UserRepository
import org.springframework.http.codec.ServerSentEvent
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.SenderOptions
import java.io.Serializable
import java.util.*


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
            retrieveNewMessages(groupId, senderUsername, receiverUsername)
                .map { ServerSentEvent.builder(it).build() }
                .doOnError(Throwable::printStackTrace)
        )

    fun retrieveNewMessages(groupId: String, senderUsername: String, receiverUsername: String) =
        this.createReactiveKafkaConsumerTemplate(groupId)
            .receiveAutoAck()
            .map { gson.fromJson(it.value(), Message::class.java) }
            .filter {
                it.senderUsername == senderUsername && it.receiverUsername == receiverUsername ||
                it.senderUsername == receiverUsername && it.receiverUsername == senderUsername
            }

    fun sendMessage(message: Message?): Message? {
        var returnMessage = message
        val json = gson.toJson(message)
        val producer = createReactiveKafkaProducerTemplate()
        producer.send("messages", Objects.hash(json).toString(), json)
            .doOnSuccess {
                println("Message sent to Kafka ${message?.text} at ${message?.timestamp}")
            }.doOnError {
                println("Error sending message to Kafka ${message?.text} at ${message?.timestamp}")
                returnMessage = null
            }
            .subscribe()
        return returnMessage
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
                        .filter { it.senderUsername == otherUser.username && it.receiverUsername == username ||
                                it.senderUsername == username && it.receiverUsername == otherUser.username }
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

    fun createReactiveKafkaConsumerTemplate(groupId: String): ReactiveKafkaConsumerTemplate<String, String> =
       ReactiveKafkaConsumerTemplate(receiverOptions.consumerProperty("group.id", groupId))

    fun createReactiveKafkaProducerTemplate() = ReactiveKafkaProducerTemplate(senderOptions)
}
