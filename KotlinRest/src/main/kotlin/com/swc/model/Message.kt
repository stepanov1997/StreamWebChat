package com.swc.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.swc.repository.ChatRepository
import com.swc.repository.UserRepository
import org.jetbrains.annotations.NotNull
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.*
import java.util.stream.Stream


@Document(collection = "messages")
class Message(
    @JsonProperty("senderUsername")
    @NotNull val senderUsername: String,

    @JsonProperty("receiverUsername")
    @NotNull val receiverUsername: String,

    @JsonProperty("text")
    @NotNull val text: String,

    @JsonProperty("timestamp")
    @NotNull val timestamp: Long
) {
    fun checkIds(userRepository: UserRepository): Boolean {
        return userRepository.findByUsername(senderUsername) != null
                && userRepository.findByUsername(receiverUsername) != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Message) return false

        if (senderUsername != other.senderUsername) return false
        if (receiverUsername != other.receiverUsername) return false
        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        var result = senderUsername.hashCode()
        result = 31 * result + receiverUsername.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }


}
