package com.swc.services

import com.swc.runner.KubernetesDeployment
import com.swc.runner.RunnerComponent
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.util.*
import javax.annotation.PostConstruct


@Service
@RunnerComponent(value = "kafka", dependsOn = [ZookeeperService::class])
class KafkaService : KubernetesDeployment() {

    @Value("\${kubernetes.kafka.host}:\${kubernetes.kafka.port}")
    var kafkaUrl: String? = null

    private var yaml: String? = null

    @PostConstruct
    @Throws(IOException::class)
    private fun init() {
        val deploymentYamlPath =
//            "k8s/client" + (if (deletingNamespaceAfterIts) "" else "-debug") + ".yaml"
            "k8s/kafka.yaml"
        val deploymentYamlResource = ClassPathResource(deploymentYamlPath)
        yaml = String(deploymentYamlResource.inputStream.readAllBytes())
    }

    override fun getDeploymentYaml(): String {
        return yaml ?: throw IllegalStateException("Kafka yaml is null.")
    }

    override fun healthcheck(): Boolean {
        val props = Properties()
        props["bootstrap.servers"] = kafkaUrl
        props["group.id"] = "test"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        try {
            KafkaConsumer<String, String>(props).listTopics()
        }catch (_: Exception){
            return false
        }
        return true
    }

    override fun getHealthcheckFailureThreshold(): Long {
        return 60L;
    }

    override fun getHealthcheckPeriodSeconds(): Long {
        return 3L;
    }
}
