package com.swc.service

import com.google.gson.Gson
import com.swc.model.Message
import com.swc.model.User
import com.swc.repository.ChatRepository
import com.swc.repository.UserRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.annotation.DirtiesContext
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kotlin.core.publisher.toFlux
import java.io.Serializable
import java.util.stream.Stream


@Suppress("ReactiveStreamsUnusedPublisher")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode= DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(MockitoExtension::class)
class TestChatService(
    @Mock val userRepository: UserRepository,
    @Mock val chatRepository: ChatRepository,
    @Mock val kafkaSender: KafkaSender<String, String>,
    @Mock val kafkaReceiver: KafkaReceiver<String, String>
) {

    private var gson: Gson = Gson()
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setUp() {
        chatService = spy(ChatService(ReceiverOptions.create(), SenderOptions.create(), Gson(), userRepository, chatRepository))
        Mockito.reset(userRepository);
        Mockito.reset(chatRepository);
        Mockito.reset(kafkaSender);
        Mockito.reset(kafkaReceiver);
    }

    @ParameterizedTest
    @MethodSource("provideMessages")
    fun `Test retrieveOldMessages method`(messages: List<Message>,
                                          sender: String,
                                          receiver: String,
                                          expected: List<Message>) {

        whenever(chatRepository.findAll()).thenReturn(messages.toList())
        val oldMessages = chatService.retrieveOldMessages(sender, receiver)
        val array = oldMessages.collectList().block()?.toTypedArray()

        verify(chatRepository,  Mockito.times(1)).findAll()
        Assertions.assertArrayEquals(expected.toTypedArray(), array)
    }

    @ParameterizedTest
    @MethodSource("provideMessages")
    fun `Test retrieveNewMessages method`(messages: List<Message>,
                                          sender: String,
                                          receiver: String,
                                          expected: List<Message>) {

        whenever(kafkaReceiver.receive()).thenReturn(Flux.fromIterable(messages.map {
            ReceiverRecord(ConsumerRecord("test", 0, 0, null, gson.toJson(it)), null)
        }))
        lenient().doReturn(kafkaReceiver)
            .`when`(chatService)
            .createKafkaReceiver(anyOrNull(), anyOrNull())
        val newMessages = chatService.retrieveNewMessages(sender, receiver)
        val array = newMessages.collectList().block()?.toTypedArray()

        verify(kafkaReceiver,  Mockito.times(1)).receive()
        verify(chatService, Mockito.times(1)).createKafkaReceiver(anyOrNull(), anyOrNull())
        Assertions.assertArrayEquals(expected.toTypedArray(), array)
    }

    @ParameterizedTest
    @MethodSource("provideMessages")
    fun `Test getMessages method`(messages: List<Message>,
                                  sender: String,
                                  receiver: String) {

        lenient().doReturn(messages.toFlux())
            .whenever(chatService)
            .retrieveOldMessages(anyOrNull(), anyOrNull())

        lenient().doReturn(messages.toFlux())
            .whenever(chatService)
            .retrieveNewMessages(anyOrNull(), anyOrNull())

        val allServerSendEvent = chatService.getMessages(sender, receiver)
        val array = allServerSendEvent.collectList().block()?.toTypedArray()

        verify(chatService, Mockito.times(1)).retrieveOldMessages(anyOrNull(), anyOrNull())
        verify(chatService, Mockito.times(1)).retrieveNewMessages(anyOrNull(), anyOrNull())
        Assertions.assertArrayEquals((messages+messages).toTypedArray(), array?.map { it.data() }?.toTypedArray())
    }

    @Test
    fun `Test sendMessage method - successful`() {
        lenient().doReturn(kafkaSender)
            .whenever(chatService)
            .createKafkaSender(anyOrNull())

        whenever(kafkaSender.send<String>(anyOrNull())).thenReturn(
            Flux.empty()
        )
        val message = Message("any()", "any()", "any()", System.currentTimeMillis())
        val actual = chatService.sendMessage(message)

        verify(kafkaSender, Mockito.times(1)).send<String>(anyOrNull())
        Assertions.assertEquals(message, actual)
    }

    @Test
    fun `Test sendMessage method - throws exception`() {
        lenient().doReturn(kafkaSender)
            .whenever(chatService)
            .createKafkaSender(anyOrNull())

        whenever(kafkaSender.send<String>(anyOrNull())).thenReturn(
            Flux.error(RuntimeException())
        )

        val message = Message("any()", "any()", "any()", System.currentTimeMillis())
        val actual = chatService.sendMessage(message)

        verify(kafkaSender,  Mockito.times(1)).send<String>(anyOrNull())
        Assertions.assertNotNull(actual)
    }

    @ParameterizedTest
    @MethodSource("provideUsers")
    fun `Test getting conversations for user`(username: String, expected: List<Map<String, Serializable>>) {
        whenever(userRepository.findAll()).thenReturn(listOf(
            User(1, "user", "", true, "", "user_sequence"),
            User(2, "user2", "", true, "", "user_sequence"),
            User(3, "user3", "", true, "", "user_sequence")
        ))
        whenever(chatRepository.findAll()).thenReturn(listOf(
            Message("user", "user2", "text1", System.currentTimeMillis()),
            Message("user2", "user", "text2",  System.currentTimeMillis()+100),
            Message("user2", "user3", "text3",  System.currentTimeMillis()+200)
        ))
        val conversationList = chatService.getConversationsForUser(username)

        verify(userRepository, Mockito.times(1)).findAll()
        verify(chatRepository, Mockito.times(2)).findAll()
        conversationList.forEachIndexed { index, map ->
            Assertions.assertEquals(expected[index]["userId"], map["userId"])
            Assertions.assertEquals(expected[index]["username"], map["username"])
            Assertions.assertEquals(expected[index]["isOnline"], map["isOnline"])
            Assertions.assertEquals(expected[index]["exists"], map["exists"])
            Assertions.assertEquals(expected[index]["lastMessage"], map["lastMessage"])
        }
    }

    @Test
    fun `Test creation of Kafka Consumer and Producer template`() {
        Assertions.assertNotNull(chatService.createKafkaSender(SenderOptions.create()))
        Assertions.assertNotNull(chatService.createKafkaReceiver("any()", ReceiverOptions.create()))
    }

    private fun provideMessages(): Stream<Arguments> =
        Stream.of(
            Arguments.of(
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                    Message("sender2", "receiver2", "any2()", System.currentTimeMillis())
                ),
                "sender1",
                "receiver1",
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                ),
            ),
            Arguments.of(
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                    Message("receiver1", "sender1", "any2()", System.currentTimeMillis())
                ),
                "sender1",
                "receiver1",
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                    Message("receiver1", "sender1", "any2()", System.currentTimeMillis()),
                ),
            ),
            Arguments.of(
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                    Message("sender2", "receiver1", "any2()", System.currentTimeMillis())
                ),
                "sender1",
                "receiver1",
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                ),
            ),
            Arguments.of(
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                    Message("sender1", "receiver2", "any2()", System.currentTimeMillis())
                ),
                "sender1",
                "receiver1",
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                ),
            ),
            Arguments.of(
                listOf(
                    Message("sender2", "receiver2", "any()", System.currentTimeMillis()),
                    Message("sender2", "receiver2", "any2()", System.currentTimeMillis())
                ),
                "sender1",
                "receiver1",
               listOf<Message>(),
            ))

    private fun provideUsers(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                "user", listOf(
                    mapOf(
                        "userId" to 2,
                        "username" to "user2",
                        "isOnline" to true,
                        "exists" to true,
                        "lastMessage" to "text2"
                    ),
                    mapOf(
                        "userId" to 3,
                        "username" to "user3",
                        "isOnline" to true,
                        "exists" to false,
                        "lastMessage" to ""
                    )
                )
            ),
            Arguments.of(
                "user2", listOf(
                    mapOf(
                        "userId" to 1,
                        "username" to "user",
                        "isOnline" to true,
                        "exists" to true,
                        "lastMessage" to "text2"
                    ),
                    mapOf(
                        "userId" to 3,
                        "username" to "user3",
                        "isOnline" to true,
                        "exists" to true,
                        "lastMessage" to "text3"
                    )
                )
            ),
            Arguments.of(
                "user3", listOf(
                    mapOf(
                        "userId" to 1,
                        "username" to "user",
                        "isOnline" to true,
                        "exists" to false,
                        "lastMessage" to ""
                    ),
                    mapOf(
                        "userId" to 2,
                        "username" to "user2",
                        "isOnline" to true,
                        "exists" to true,
                        "lastMessage" to "text3"
                    )
                )
            )
        )
    }
}

