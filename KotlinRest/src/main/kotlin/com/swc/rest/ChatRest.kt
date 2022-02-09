package com.swc.rest

import com.swc.model.Message
import com.swc.repository.UserRepository
import com.swc.service.ChatService
import com.swc.service.SequenceGenerateServices
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.io.Serializable

@RestController
@RequestMapping("chat")
@CrossOrigin(origins = ["*"])
class ChatRest(
    val chatService: ChatService,
    val userRepository: UserRepository,
    val sequenceGenerateServices: SequenceGenerateServices
) {

    @GetMapping("/{senderUsername}/{receiverUsername}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getMessages(
        @PathVariable senderUsername: String,
        @PathVariable receiverUsername: String
    ): Flux<ServerSentEvent<Message>> {
        return chatService.getMessages(senderUsername, receiverUsername)
    }

    @PostMapping
    fun sendMessage(@RequestBody messageModel: Message): ResponseEntity<Any> {
        if (!messageModel.checkIds(userRepository)) {
            return ResponseEntity.badRequest().body("Ids are not valid")
        }
        val sendMessage = chatService.sendMessage(messageModel)
        return when {
            sendMessage != null -> ResponseEntity.ok(sendMessage)
            else -> ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("conversations/{username}")
    fun getAllConversations(@PathVariable username: String): ResponseEntity<List<Map<String, Serializable>>> {
        return ResponseEntity.ok(chatService.getConversationsForUser(username));
    }
}
