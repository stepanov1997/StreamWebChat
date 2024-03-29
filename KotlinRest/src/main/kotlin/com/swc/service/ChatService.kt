package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import com.swc.repository.ChatRepository
import com.swc.repository.UserRepository
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.data.domain.Pageable
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.internals.ConsumerFactory
import reactor.kafka.receiver.internals.DefaultKafkaReceiver
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.internals.DefaultKafkaSender
import reactor.kafka.sender.internals.ProducerFactory
import java.io.Serializable
import java.util.*


@Service
class ChatService(
    val receiverOptions : ReceiverOptions<String, String>,
    val senderOptions: SenderOptions<String, String>,
    val gson: Gson,
    val userRepository: UserRepository,
    val chatRepository: ChatRepository
) {

    fun retrieveOldMessages(pageable: Pageable, senderUsername: String, receiverUsername: String) =
        Flux.fromStream {
            chatRepository
                .findAllBySenderUsernameAndReceiverUsername(senderUsername, receiverUsername, pageable)
                .reversed()
                .stream()
        }.checkpoint("Messages from database are started being consumed")

    fun getMessages(pageable: Pageable, senderUsername: String, receiverUsername: String): Flux<ServerSentEvent<Message>> =
        Flux.concat(
            retrieveOldMessages(pageable, senderUsername, receiverUsername)
                .map { ServerSentEvent.builder(it).build() }
                .doOnError(Throwable::printStackTrace),
            retrieveNewMessages(senderUsername, receiverUsername)
                .map { ServerSentEvent.builder(it).build() }
                .doOnError(Throwable::printStackTrace)
        )

    fun retrieveNewMessages(senderUsername: String, receiverUsername: String) =
        createKafkaReceiver("rest-consumer-${UUID.randomUUID()}", receiverOptions)
            .receive()
            .checkpoint("Messages are started being consumed")
            .log()
            .map { gson.fromJson(it.value(), Message::class.java) }
            .checkpoint("Messages are done consumed")
            .filter {
                it.senderUsername == senderUsername && it.receiverUsername == receiverUsername ||
                it.senderUsername == receiverUsername && it.receiverUsername == senderUsername
            }

    fun sendMessage(message: Message?): Message? =
        try {
            val json = gson.toJson(message);
            createKafkaSender(senderOptions)
                .send(
                    Mono.just(SenderRecord.create(ProducerRecord<String, String>("messages", json), null))
                        .doOnError { it.printStackTrace() }
                )
                .subscribe()
            message
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    fun getConversationsForUser(username: String): List<Map<String, Serializable>> {
        return userRepository
            .findAll()
            .asSequence()
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
                    "name" to it.first.name,
                    "surname" to it.first.surname,
                    "username" to it.first.username,
                    "isOnline" to it.first.isOnline,
                    "exists" to (it.second != null),
                    "timestamp" to (it.second?.timestamp ?: 0),
                    "lastMessage" to (it.second?.text ?: "")
                )
            }
            .sortedByDescending { it["timestamp"] as Long }
            .toList()
    }

    fun createKafkaReceiver(groupId: String, receiverOptions: ReceiverOptions<String, String>): KafkaReceiver<String, String> {
        return DefaultKafkaReceiver(ConsumerFactory.INSTANCE, receiverOptions
            .consumerProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId))
    }

    fun createKafkaSender(senderOptions: SenderOptions<String, String>): KafkaSender<String, String> {
        return DefaultKafkaSender(ProducerFactory.INSTANCE, senderOptions)
    }
}
