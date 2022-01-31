package com.swc.services

import com.swc.runner.KubernetesDeployment
import com.swc.runner.RunnerComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import javax.annotation.PostConstruct


@Service
@RunnerComponent(value = "mongo-rs0-1", dependsOn = [])
class MongoFirstService : KubernetesDeployment() {

    @Value("http://\${kubernetes.mongo-rs0-1.host}:\${kubernetes.mongo-rs0-1.port}")
    var mongodbUrl: String? = null

    private var yaml: String? = null

    @PostConstruct
    @Throws(IOException::class)
    private fun init() {
        val deploymentYamlPath =
//            "k8s/client" + (if (deletingNamespaceAfterIts) "" else "-debug") + ".yaml"
            "k8s/mongo-rs0-1.yaml"
        val deploymentYamlResource = ClassPathResource(deploymentYamlPath)
        yaml = String(deploymentYamlResource.inputStream.readAllBytes())
    }

    override fun getDeploymentYaml(): String {
        return yaml ?: throw IllegalStateException("The first Mongodb yaml is null.")
    }

    override fun healthcheck(): Boolean {
        return RestTemplate()
            .getForEntity(mongodbUrl ?: throw IllegalStateException("The first Mongodb url is null."), String::class.java)
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
