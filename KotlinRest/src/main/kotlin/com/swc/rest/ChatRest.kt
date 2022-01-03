package com.swc.rest

import com.swc.model.Message
import com.swc.model.MessageUserModel
import com.swc.repository.UserRepository
import com.swc.service.ChatService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.kafka.support.SendResult
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.just
import java.time.Duration

@RestController
@RequestMapping("chat")
@CrossOrigin(origins = ["*"])
class ChatRest(val chatService: ChatService, val userRepository: UserRepository) {

    @GetMapping("/{value}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getMessages(@PathVariable value: Int): Flux<Message> {
        return chatService.getMessages()
    }

    @PostMapping
    fun sendMessage(@RequestBody messageUserModel: MessageUserModel): ResponseEntity<String> {
        val message = messageUserModel.toMessage(userRepository)
        return ResponseEntity.ok(chatService.sendMessage(message))
    }
}
