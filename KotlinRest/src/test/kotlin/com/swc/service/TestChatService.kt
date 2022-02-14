package com.swc.service

import com.google.gson.Gson
import com.mongodb.MongoClientException
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
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.test.annotation.DirtiesContext
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kotlin.core.publisher.toFlux
import java.io.Serializable
import java.lang.NullPointerException
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
    @MethodSource("provideMongoMessages")
    fun `Test retrieveOldMessages method - successful`(messages: List<Message>,
                                          sender: String,
                                          receiver: String,
                                          expected: List<Message>) {

        whenever(chatRepository.findAllBySenderUsernameAndReceiverUsername(any(), any(), any())).thenReturn(
            PageImpl(messages.toList())
        )
        val oldMessages = chatService.retrieveOldMessages(Pageable.unpaged(), sender, receiver)
        val array = oldMessages.collectList().block()?.toTypedArray()

        verify(chatRepository,  Mockito.times(1)).findAllBySenderUsernameAndReceiverUsername(any(), any(), any())
        Assertions.assertArrayEquals(expected.toTypedArray(), array)
    }

    @ParameterizedTest
    @MethodSource("provideMongoMessages")
    fun `Test retrieveOldMessages method - unsuccessful`(messages: List<Message>,
                                                         sender: String,
                                                         receiver: String,
                                                         expected: List<Message>) {

        whenever(chatRepository.findAllBySenderUsernameAndReceiverUsername(any(), any(), any())).thenThrow(RuntimeException::class.java)

        Assertions.assertThrows(RuntimeException::class.java) { chatService.retrieveOldMessages(Pageable.unpaged(), sender, receiver).collectList().block() }
        verify(chatRepository,  Mockito.times(1)).findAllBySenderUsernameAndReceiverUsername(any(), any(), any())
    }

    @ParameterizedTest
    @MethodSource("provideMongoMessages")
    fun `Test retrieveOldMessages method - null`(messages: List<Message>,
                                                         sender: String,
                                                         receiver: String,
                                                         expected: List<Message>) {

        whenever(chatRepository.findAllBySenderUsernameAndReceiverUsername(any(), any(), any())).thenReturn(null)

        Assertions.assertThrows(NullPointerException::class.java) { chatService.retrieveOldMessages(Pageable.unpaged(), sender, receiver).collectList().block() }
        verify(chatRepository,  Mockito.times(1)).findAllBySenderUsernameAndReceiverUsername(any(), any(), any())
    }

    @ParameterizedTest
    @MethodSource("provideMongoMessages")
    fun `Test retrieveOldMessages method - empty`(messages: List<Message>,
                                                 sender: String,
                                                 receiver: String,
                                                 expected: List<Message>) {

        whenever(chatRepository.findAllBySenderUsernameAndReceiverUsername(any(), any(), any())).thenReturn(PageImpl(emptyList()))

        Assertions.assertEquals(0, chatService.retrieveOldMessages(Pageable.unpaged(), sender, receiver).collectList().block()?.size)
        verify(chatRepository,  Mockito.times(1)).findAllBySenderUsernameAndReceiverUsername(any(), any(), any())
    }

    @ParameterizedTest
    @MethodSource("provideKafkaMessages")
    fun `Test retrieveNewMessages method - successful`(messages: List<Message>,
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
    @MethodSource("provideKafkaMessages")
    fun `Test retrieveNewMessages method - unsuccessful`(messages: List<Message>,
                                          sender: String,
                                          receiver: String,
                                          expected: List<Message>) {

        whenever(kafkaReceiver.receive()).thenReturn(Flux.error(RuntimeException()))
        lenient().doReturn(kafkaReceiver)
            .`when`(chatService)
            .createKafkaReceiver(anyOrNull(), anyOrNull())

        Assertions.assertThrows(RuntimeException::class.java) { chatService.retrieveNewMessages(sender, receiver).collectList().block() }
        verify(kafkaReceiver,  Mockito.times(1)).receive()
        verify(chatService, Mockito.times(1)).createKafkaReceiver(anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @MethodSource("provideKafkaMessages")
    fun `Test getMessages method - successful`(messages: List<Message>,
                                  sender: String,
                                  receiver: String,
                                  expected: List<Message>) {

        lenient().doReturn(messages.toFlux())
            .whenever(chatService)
            .retrieveOldMessages(any(), anyString(), anyString())

        lenient().doReturn(messages.toFlux())
            .whenever(chatService)
            .retrieveNewMessages(anyString(), anyString())

        val allServerSendEvent = chatService.getMessages(Pageable.unpaged(), sender, receiver)
        val array = allServerSendEvent.collectList().block()?.toTypedArray()

        verify(chatService, Mockito.times(1)).retrieveOldMessages(any(), anyString(), anyString())
        verify(chatService, Mockito.times(1)).retrieveNewMessages(anyString(), anyString())
        Assertions.assertArrayEquals((messages+messages).toTypedArray(), array?.map { it.data() }?.toTypedArray())
    }

    @ParameterizedTest
    @MethodSource("provideKafkaMessages")
    fun `Test getMessages method - old messages runtime exception`(messages: List<Message>,
                                  sender: String,
                                  receiver: String,
                                  expected: List<Message>) {

        lenient().doReturn(Flux.error<Message>(RuntimeException()))
            .whenever(chatService)
            .retrieveOldMessages(any(), anyString(), anyString())

        lenient().doReturn(messages.toFlux())
            .whenever(chatService)
            .retrieveNewMessages(anyString(), anyString())

        Assertions.assertThrows(RuntimeException::class.java) {chatService.getMessages(Pageable.unpaged(), sender, receiver).collectList().block()}
        verify(chatService, Mockito.times(1)).retrieveOldMessages(any(), anyString(), anyString())
        verify(chatService, Mockito.times(1)).retrieveNewMessages(anyString(), anyString())
    }

    @ParameterizedTest
    @MethodSource("provideKafkaMessages")
    fun `Test getMessages method - new messages null pointer`(messages: List<Message>,
                                  sender: String,
                                  receiver: String,
                                  expected: List<Message>) {

        lenient().doReturn(Flux.empty<Message>())
            .whenever(chatService)
            .retrieveOldMessages(any(), anyString(), anyString())

        lenient().doReturn(Flux.empty<Message>())
            .whenever(chatService)
            .retrieveNewMessages(anyString(), anyString())

        Assertions.assertEquals(0,
            chatService.getMessages(Pageable.unpaged(), sender, receiver).collectList().block()?.size
        )
        verify(chatService, Mockito.times(1)).retrieveOldMessages(any(), anyString(), anyString())
        verify(chatService, Mockito.times(1)).retrieveNewMessages(anyString(), anyString())
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

    @Test
    fun `Test sendMessage method - createKafkaSender throws exception`() {
        lenient().doReturn(null)
            .whenever(chatService)
            .createKafkaSender(anyOrNull())

        val message = Message("any()", "any()", "any()", System.currentTimeMillis())
        val actual = chatService.sendMessage(message)

        verify(kafkaSender,  Mockito.times(0)).send<String>(anyOrNull())
        Assertions.assertNull(actual)
    }

    @Test
    fun `Test sendMessage method - send null`() {
        lenient().doReturn(null)
            .whenever(chatService)
            .createKafkaSender(anyOrNull())

        val actual = chatService.sendMessage(null)

        verify(kafkaSender,  Mockito.times(0)).send<String>(anyOrNull())
        Assertions.assertNull(actual)
    }

    @Test
    fun `Test sendMessage method - throws exception create Kafka sender`() {
        lenient().doThrow(RuntimeException())
            .whenever(chatService)
            .createKafkaSender(anyOrNull())

        val message = Message("any()", "any()", "any()", System.currentTimeMillis())
        val actual = chatService.sendMessage(message)

        verify(kafkaSender,  Mockito.times(0)).send<String>(anyOrNull())
        Assertions.assertNull(actual)
    }

    @ParameterizedTest
    @MethodSource("provideUsers")
    fun `Test getting conversations for user`(username: String, expected: List<Map<String, Serializable>>) {
        whenever(userRepository.findAll()).thenReturn(listOf(
            User(1, "", "", "user", "", true, "", "user_sequence"),
            User(2, "", "","user2", "", false, "", "user_sequence"),
            User(3, "", "","user3", "", true, "", "user_sequence")
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

    @ParameterizedTest
    @MethodSource("provideUsers")
    fun `Test getting conversations for user - userRepository findAll throws exception`(username: String, expected: List<Map<String, Serializable>>) {
        whenever(userRepository.findAll()).thenThrow(MongoClientException("mongo exception"))
        whenever(chatRepository.findAll()).thenReturn(listOf(
            Message("user", "user2", "text1", System.currentTimeMillis()),
            Message("user2", "user", "text2",  System.currentTimeMillis()+100),
            Message("user2", "user3", "text3",  System.currentTimeMillis()+200)
        ))
        Assertions.assertThrows(MongoClientException::class.java) {chatService.getConversationsForUser(username)}
        verify(userRepository, Mockito.times(1)).findAll()
        verify(chatRepository, Mockito.times(0)).findAll()
    }

    @ParameterizedTest
    @MethodSource("provideUsers")
    fun `Test getting conversations for user - chatRepository findAll throws exception`(username: String, expected: List<Map<String, Serializable>>) {
        whenever(userRepository.findAll()).thenReturn(listOf(
            User(1, "", "", "user", "", true, "", "user_sequence"),
            User(2, "", "","user2", "", false, "", "user_sequence"),
            User(3, "", "","user3", "", true, "", "user_sequence")
        ))
        whenever(chatRepository.findAll()).thenThrow(MongoClientException("mongo exception"))
        Assertions.assertThrows(MongoClientException::class.java) {chatService.getConversationsForUser(username)}

        verify(userRepository, Mockito.times(1)).findAll()
        verify(chatRepository, Mockito.times(1)).findAll()
    }

    @ParameterizedTest
    @MethodSource("provideUsers")
    fun `Test getting conversations for user - chatRepository and userRepository findAll throws exception`(username: String, expected: List<Map<String, Serializable>>) {
        whenever(userRepository.findAll()).thenThrow(MongoClientException("mongo exception"))
        whenever(chatRepository.findAll()).thenThrow(RuntimeException())
        Assertions.assertThrows(MongoClientException::class.java) {chatService.getConversationsForUser(username)}

        verify(userRepository, Mockito.times(1)).findAll()
        verify(chatRepository, Mockito.times(0)).findAll()
    }

    @Test
    fun `Test creation of Kafka Consumer and Producer template - successful`() {
        Assertions.assertNotNull(chatService.createKafkaSender(SenderOptions.create()))
        Assertions.assertNotNull(chatService.createKafkaReceiver("any()", ReceiverOptions.create()))
    }

    @Test
    fun `Test creation of Kafka Consumer and Producer template - throws exception`() {
        lenient().doThrow(RuntimeException())
            .whenever(chatService)
            .createKafkaSender(anyOrNull())
        Assertions.assertThrows(RuntimeException::class.java) {chatService.createKafkaSender(SenderOptions.create())}

        lenient().doThrow(RuntimeException())
            .whenever(chatService)
            .createKafkaReceiver(anyOrNull(), anyOrNull())
        Assertions.assertThrows(RuntimeException::class.java) {chatService.createKafkaReceiver("any()", ReceiverOptions.create())}
    }

    private fun provideMongoMessages(): Stream<Arguments> =
        Stream.of(
            Arguments.of(
                listOf(
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
                    Message("sender2", "receiver2", "any2()", System.currentTimeMillis())
                ),
                "sender1",
                "receiver1",
                listOf(
                    Message("sender2", "receiver2", "any2()", System.currentTimeMillis()),
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
                    Message("receiver1", "sender1", "any2()", System.currentTimeMillis()),
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis()),
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
                    Message("sender2", "receiver1", "any2()", System.currentTimeMillis()),
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis())
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
                    Message("sender1", "receiver2", "any2()", System.currentTimeMillis()),
                    Message("sender1", "receiver1", "any()", System.currentTimeMillis())
                ),
            ),
            Arguments.of(
                listOf(
                    Message("sender2", "receiver2", "any()", System.currentTimeMillis()),
                    Message("sender2", "receiver2", "any2()", System.currentTimeMillis())
                ),
                "sender1",
                "receiver1",
                listOf(

                    Message("sender2", "receiver2", "any2()", System.currentTimeMillis()),
                    Message("sender2", "receiver2", "any()", System.currentTimeMillis())
                )
            ))

    private fun provideKafkaMessages(): Stream<Arguments> =
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
                        "isOnline" to false,
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
                        "userId" to 3,
                        "username" to "user3",
                        "isOnline" to true,
                        "exists" to true,
                        "lastMessage" to "text3"
                    ),
                    mapOf(
                        "userId" to 1,
                        "username" to "user",
                        "isOnline" to true,
                        "exists" to true,
                        "lastMessage" to "text2"
                    )
                )
            ),
            Arguments.of(
                "user3", listOf(
                    mapOf(
                        "userId" to 2,
                        "username" to "user2",
                        "isOnline" to false,
                        "exists" to true,
                        "lastMessage" to "text3"
                    ),
                    mapOf(
                        "userId" to 1,
                        "username" to "user",
                        "isOnline" to true,
                        "exists" to false,
                        "lastMessage" to ""
                    )
                )
            )
        )
    }
}

