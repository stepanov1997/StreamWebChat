package com.swc.rest

import com.swc.model.Message
import com.swc.service.ChatService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.just
import java.time.Duration

@RestController
@RequestMapping("chat")
@CrossOrigin(origins = ["*"])
class ChatRest(val chatService: ChatService) {

    @GetMapping("/{value}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getMessages(@PathVariable value: Int): Flux<Message> {
        return chatService.getMessages()
    }

    @PostMapping
    fun sendMessage(@RequestBody message: Message): ResponseEntity<Unit> {
        return ResponseEntity.ok(chatService.sendMessage(message))
    }
}
