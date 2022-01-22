package com.swc.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.swc.repository.UserRepository
import org.jetbrains.annotations.NotNull
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*
import java.util.stream.Stream

@Document(collection = "messages")
class Message(
    @DBRef val senderId: Int,
    @DBRef val receiverId: Int,
    val text: String,
    val timestamp: Long,
    @Transient val sequenceName: String = "messages_sequence",
) {
    fun toUserModel(userRepository: UserRepository) = MessageUserModel(userRepository, this)
}

class MessageRemoteModel(
    @NotNull val senderId: Int,
    @NotNull val receiverId: Int,
    @NotNull val text: String,
    @NotNull val timestamp: Long
) {
    fun checkIds(@NotNull userRepository: MongoRepository<User, Int>): MessageRemoteModel? =
        Stream.of(userRepository.findById(receiverId), userRepository.findById(senderId))
            .filter(Optional<User>::isPresent)
            .map(Optional<User>::get)
            .map { it.id }
            .toList()
            .takeIf { it.count() == 2 }
            ?.let { MessageRemoteModel(it[0], it[1], text, timestamp) }
}

class MessageUserModel{
    @JsonProperty("senderUsername")
    var senderUsername: String = ""
    @JsonProperty("receiverUsername")
    var receiverUsername: String = ""
    @JsonProperty("text")
    var text: String = ""
    @JsonProperty("timestamp")
    var timestamp: Long = 0

    constructor()

    constructor(userRepository: MongoRepository<User, Int>, message: Message) {
            Stream.of(userRepository.findById(message.senderId), userRepository.findById(message.receiverId))
                .filter(Optional<User>::isPresent)
                .map(Optional<User>::get)
                .map { it.username }
                .toList()
                .takeIf { it.count() == 2 }
                ?.let { senderUsername = it[0]; receiverUsername = it[1]; text = message.text; timestamp = message.timestamp }
                ?: throw IllegalArgumentException("Message has no sender or receiver")
    }
}
