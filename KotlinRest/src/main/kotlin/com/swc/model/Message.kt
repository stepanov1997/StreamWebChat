package com.swc.model

import com.swc.repository.UserRepository
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.Repository

@Document(collection = "messages")
class Message(
    @Id val id: Int,
    @DBRef val sender: User,
    @DBRef val receiver: User,
    private val text: String,
    @Transient val sequenceName: String = "message_sequence",
){
    fun toUserModel() = MessageUserModel(sender.id, receiver.id, text)
}

class MessageUserModel(@NotNull val senderId: Int,
                       @NotNull val receiverId: Int,
                       @NotNull val text: String){
    fun toMessage(@NotNull userRepository: MongoRepository<User, Int>): Message {
        val sender = userRepository.findById(senderId).orElse(null);
        val receiver = userRepository.findById(receiverId).orElse(null);
        if(sender == null || receiver == null)
            throw IllegalArgumentException("User not found")
        return Message(-1, sender, receiver, text)
    }
}
