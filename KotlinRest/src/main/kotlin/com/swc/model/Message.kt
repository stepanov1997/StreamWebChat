package com.swc.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*
import java.util.stream.Stream

@Document(collection = "messages")
class Message(
    @Id val id: Int,
    @DBRef val senderId: Int,
    @DBRef val receiverId: Int,
    val text: String,
    val timestamp: Long,
    @Transient val sequenceName: String = "messages_sequence",
) {
    fun toUserModel() = MessageRemoteModel(id, senderId, receiverId, text, timestamp)
}

class MessageRemoteModel(
    @NotNull var id: Int,
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
            ?.let { MessageRemoteModel(id, it[0], it[1], text, timestamp) }
}

class MessageUserModel{
    @JsonProperty("id") var id: Int? = null
    @JsonProperty("senderUsername") var senderUsername: String? = null
    @JsonProperty("receiverUsername") var receiverUsername: String? = null
    @JsonProperty("text") var text: String? = null
    @JsonProperty("timestamp") val timestamp: Long? = null

    constructor()

    constructor(@NotNull userRepository: MongoRepository<User, Int>, message: Message) {
        Stream.of(userRepository.findById(message.senderId), userRepository.findById(message.receiverId))
            .filter(Optional<User>::isPresent)
            .map(Optional<User>::get)
            .map { it.username }
            .toList()
            .takeIf { it.count() == 2 }
            ?.let { id=message.id; senderUsername = it[0]; receiverUsername = it[1]; text = message.text }
            ?: throw IllegalArgumentException("Message has no sender or receiver")
    }
}
