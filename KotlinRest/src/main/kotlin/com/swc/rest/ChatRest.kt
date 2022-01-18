package com.swc.rest

import com.swc.model.MessageRemoteModel
import com.swc.model.MessageUserModel
import com.swc.repository.UserRepository
import com.swc.service.ChatService
import com.swc.service.SequenceGenerateServices
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("chat")
@CrossOrigin(origins = ["*"])
class ChatRest(val chatService: ChatService, val userRepository: UserRepository, val sequenceGenerateServices: SequenceGenerateServices) {

    @GetMapping("/{senderId}/{receiverId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getMessages(@PathVariable senderId: Int, @PathVariable receiverId: Int): Flux<ServerSentEvent<MessageUserModel>> {
        return chatService.getMessages(senderId, receiverId)
    }

    @PostMapping
    fun sendMessage(@RequestBody messageRemoteModel: MessageRemoteModel): ResponseEntity<Any> {
        messageRemoteModel.id = sequenceGenerateServices.generateSequence("messages_sequence").toInt()
        val message = messageRemoteModel.checkIds(userRepository) ?: return ResponseEntity.badRequest().body("Ids are not valid")
        val sendMessage = chatService.sendMessage(message)
        return when {
            sendMessage != null -> ResponseEntity.ok(sendMessage)
            else -> return ResponseEntity.internalServerError().build()
        }
    }
}
