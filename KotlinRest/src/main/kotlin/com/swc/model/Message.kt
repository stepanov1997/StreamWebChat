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
}
