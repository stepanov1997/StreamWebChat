package com.swc.rest

import com.swc.model.Message
import com.swc.repository.UserRepository
import com.swc.service.ChatService
import com.swc.service.SequenceGenerateServices
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.io.Serializable
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


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
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "10") size: Int,
        @PathVariable senderUsername: String,
        @PathVariable receiverUsername: String
    ): Flux<ServerSentEvent<Message>> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by("timestamp").descending())
        return chatService.getMessages(pageable, senderUsername, receiverUsername)
    }

    @GetMapping("/old/{senderUsername}/{receiverUsername}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOldMessages(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "10") size: Int,
        @PathVariable senderUsername: String,
        @PathVariable receiverUsername: String
    ): Flux<Message> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by("timestamp").descending())
        return chatService.retrieveOldMessages(pageable, senderUsername, receiverUsername)
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
        userRepository.findByUsername(username)
            ?.first()
            .let {
                it?.lastOnline = now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss"))
                if(it!=null) userRepository.save(it)
            }
        return ResponseEntity.ok(chatService.getConversationsForUser(username));
    }
}
