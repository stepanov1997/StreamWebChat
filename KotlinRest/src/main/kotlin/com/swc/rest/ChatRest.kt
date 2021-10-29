package com.swc.rest

import com.swc.service.ChatService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Flux.just
import java.time.Duration

@RestController
class ChatRest(val chatService: ChatService) {

    @GetMapping("/{value}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @CrossOrigin
    fun getMessages(@PathVariable value: Int): Flux<String> {
        return chatService.getMessages()
    }

    @PostMapping
    @CrossOrigin
    fun sendMessage(@RequestBody message : String): ResponseEntity<Unit> {
        return ResponseEntity.ok(chatService.sendMessage(message))
    }
}
