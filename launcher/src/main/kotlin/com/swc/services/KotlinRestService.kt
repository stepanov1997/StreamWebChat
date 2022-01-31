package com.swc.services

import com.swc.runner.KubernetesDeployment
import com.swc.runner.RunnerComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.InetAddress
import javax.annotation.PostConstruct


@Service
@RunnerComponent(value = "kotlin-rest", dependsOn = [TransferAppService::class])
class KotlinRestService : KubernetesDeployment() {

    @Value("http://\${kubernetes.kotlin-rest.host}:\${kubernetes.kotlin-rest.port}")
    var kotlinRestUrl: String? = null

    private var yaml: String? = null

    @PostConstruct
    @Throws(IOException::class)
    private fun init() {
        val deploymentYamlPath =
//            "k8s/client" + (if (deletingNamespaceAfterIts) "" else "-debug") + ".yaml"
            "k8s/rest.yaml"
        val deploymentYamlResource = ClassPathResource(deploymentYamlPath)
        yaml = String(deploymentYamlResource.inputStream.readAllBytes())
    }

    override fun getDeploymentYaml(): String {
        return yaml ?: throw IllegalStateException("Kotlin rest yaml is null.")
    }

    override fun healthcheck(): Boolean {
        return RestTemplate()
            .getForEntity(
                ("$kotlinRestUrl/chat/conversations/admin"), String::class.java)
            .statusCode
            .is2xxSuccessful
    }

    override fun getHealthcheckFailureThreshold(): Long {
        return 60L;
    }

    override fun getHealthcheckPeriodSeconds(): Long {
        return 3L;
    }
}
