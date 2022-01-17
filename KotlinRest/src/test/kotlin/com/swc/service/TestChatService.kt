package com.swc.service

import com.mongodb.assertions.Assertions
import com.swc.model.MessageUserModel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.ServerSentEvent
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.time.LocalTime


@ExtendWith(MockitoExtension::class)
@EnableKafka
class TestChatService {

    val webClient: WebClient = WebClient.create("http://localhost:8080");

    @Test
    fun test() {
        val list = ArrayList<MessageUserModel?>();
        val type: ParameterizedTypeReference<ServerSentEvent<MessageUserModel>> =
            object : ParameterizedTypeReference<ServerSentEvent<MessageUserModel>>() {}
        webClient.get()
            .uri("/chat/1/1")
            .retrieve()
            .bodyToFlux(type)
            .doOnError(Throwable::printStackTrace)
            .subscribe(
            { content: ServerSentEvent<MessageUserModel> ->
                list.add(content.data())
                println(content.data()?.text)
            },
            { error: Throwable? -> System.out.printf("Error receiving SSE: %s", error) }
        ) { println("Completed!!!") }

        while (true) {
            Thread.sleep(1000)
        }
        Assertions.assertTrue(true);
    }
}
