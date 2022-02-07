package com.deepintent.it.test.util

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

class KafkaTools(consumerFactory: ConsumerFactory<String, String>) :
    AutoCloseable {

    private val consumer: Consumer<String, String>
    private val records: MutableMap<String, BlockingQueue<String>> = ConcurrentHashMap()
    private var active = true

    init {
        val config: MutableMap<String, Any> = HashMap(consumerFactory.configurationProperties)
        config[ConsumerConfig.GROUP_ID_CONFIG] = "kafka-tools-" + TIMESTAMP + "-" + ID.incrementAndGet()
        consumer = DefaultKafkaConsumerFactory<String, String>(config).createConsumer()
        consumer.subscribe(Pattern.compile(".*"))
        val consumerThread = Thread({
            while (active) {
                val kafkaRecords =
                    consumer.poll(Duration.ofSeconds(1))
                if (kafkaRecords.isEmpty) {
                    continue
                }
                for (kafkaRecord in kafkaRecords) {
                    records.computeIfAbsent(
                        kafkaRecord.topic()
                    ) { t: String? -> LinkedBlockingQueue() }
                        .add(kafkaRecord.value())
                }
                consumer.commitSync()
            }
            consumer.unsubscribe()
        }, "" + config[ConsumerConfig.GROUP_ID_CONFIG])
        consumerThread.isDaemon = true
        consumerThread.start()
    }

    @Throws(InterruptedException::class)
    fun readRecord(topic: String): String? {
        val expiration = LocalDateTime.now().plus(Duration.ofSeconds(3))
        while (LocalDateTime.now().isBefore(expiration)) {
            val topicData = records[topic]
            if (topicData != null) {
                return topicData.poll(2, TimeUnit.SECONDS)
            }
            Thread.sleep(100)
        }
        return null
    }
    
    fun clear() {
        records.clear()
    }

    override fun close() {
        active = false
    }

    companion object {
        private val ID = AtomicInteger(0)
        private val TIMESTAMP = System.currentTimeMillis()
    }
}
