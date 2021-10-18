package com.swc.rest

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
class ChatService {

    @GetMapping("/{value}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getMessages(@PathVariable value: Int) = Flux.interval(Duration.ofSeconds(1)).map { it.inc()}.repeat(2)

}