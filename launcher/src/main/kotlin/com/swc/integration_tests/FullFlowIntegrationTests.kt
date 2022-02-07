package com.swc.integration_tests

import SwcProperties
import com.deepintent.it.test.util.KafkaTools
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.swc.launcher.SwcIntegrationTest
import io.restassured.RestAssured.given
import org.bson.Document
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.ConsumerFactory


@SwcIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class FullFlowIntegrationTests @Autowired constructor(
    swcProperties: SwcProperties,
    private val mongoClient: MongoClient,
    private val consumerFactory: ConsumerFactory<String, String>
) {

    var restUrl = "${swcProperties.`kotlin-rest`.host}:${swcProperties.`kotlin-rest`.port}"
    var kafkaTools: KafkaTools? = null

    @BeforeAll
    fun setup() {
        kafkaTools = KafkaTools(consumerFactory);

        println("Running integration tests")
    }

    @ParameterizedTest
    @ValueSource(strings = ["test1", "test2"])
    @Order(1)
    fun `Test userRegister method`(username: String) {
        val response =
            given()
                .contentType("application/json")
                .body(
                    mapOf(
                        "username" to username,
                        "password" to "test"
                    )
                )
                .post("http://$restUrl/user/register")
                .then()
                .statusCode(201)
                .extract()
                .response()

        val jsonPath = response.body().jsonPath()
        val usernameResponse = jsonPath.getString("username")
        val passwordResponse = jsonPath.getString("password")
        if (usernameResponse != username || passwordResponse != "test") {
            throw AssertionError("Username or password is not correct")
        }

        Assertions.assertEquals(username, usernameResponse)
    }

    @ParameterizedTest
    @ValueSource(strings = ["test1", "test2"])
    @Order(2)
    fun `Test userLogin method`(username: String) {
        val loginResponse =
            given()
                .contentType("application/json")
                .body(
                    mapOf(
                        "username" to username,
                        "password" to "test"
                    )
                )
                .post("http://$restUrl/user/login")
                .then()
                .statusCode(200)
                .extract()
                .response()

        val jsonPath = loginResponse.body().jsonPath()
        val usernameResponse = jsonPath.getString("username")
        if (usernameResponse != username) {
            throw AssertionError("Username or password is not correct")
        }

        Assertions.assertEquals(username, usernameResponse)
    }

    @ParameterizedTest
    @CsvSource(
        "test1, test2, Hello World",
        "test2, test1, Hello World"
    )
    @Order(3)
    fun `Test sendMessage method`(sender: String, receiver: String, text: String) {
        val message = mapOf(
            "senderUsername" to sender,
            "receiverUsername" to receiver,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        val response =
            given()
                .contentType("application/json")
                .body(message)
                .post("http://$restUrl/chat")
                .then()
                .statusCode(200)
                .extract()
                .response()

        val responseMessage = response.body().jsonPath().getMap<String, String>("")
        Assertions.assertEquals(message["senderUsername"], responseMessage["senderUsername"])
        Assertions.assertEquals(message["receiverUsername"], responseMessage["receiverUsername"])
        Assertions.assertEquals(message["text"], responseMessage["text"])
        Assertions.assertEquals(message["timestamp"], responseMessage["timestamp"])
    }

    @ParameterizedTest
    @Order(4)
    @CsvSource(
        "test1, test2, Hello World",
        "test2, test1, Hello World"
    )
    fun `Test are messages written in Kafka`(sender: String, receiver: String, text: String) {
        var exists = false
        var counter = 0
        while(counter < 2) {
            val record = kafkaTools?.readRecord("messages")
            val jsonObject = Gson().fromJson(record, JsonObject::class.java)
            if (jsonObject.get("senderUsername").asString == sender &&
                jsonObject.get("receiverUsername").asString == receiver &&
                jsonObject.get("text").asString == text) {
                exists = true
                break
            }
            counter++
        }
        Assertions.assertTrue(exists)
    }


    @ParameterizedTest
    @Order(5)
    @CsvSource(
        "test1, test2",
        "test2, test1"
    )
    fun `Test are messages written in database`(sender: String, receiver: String) {
        val database: MongoDatabase = mongoClient.getDatabase("test")
        val collection: MongoCollection<Document> = database.getCollection("messages")
        val messages = collection.find(
            and(
                eq("senderUsername", sender),
                eq("receiverUsername", receiver)
            )
        ).toList()
        Assertions.assertEquals(1, messages.size)
    }

    @AfterAll
    fun `Close mongo client`() {
        mongoClient.close()
        kafkaTools?.close();
    }
}

