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
@RunnerComponent(value = "setup-rs", dependsOn = [MongoThirdService::class])
class SetupReplicaSetService : KubernetesDeployment() {

    private var yaml: String? = null

    @PostConstruct
    @Throws(IOException::class)
    private fun init() {
        val deploymentYamlPath =
//            "k8s/client" + (if (deletingNamespaceAfterIts) "" else "-debug") + ".yaml"
            "k8s/setup-rs.yaml"
        val deploymentYamlResource = ClassPathResource(deploymentYamlPath)
        yaml = String(deploymentYamlResource.inputStream.readAllBytes())
    }

    override fun getDeploymentYaml(): String {
        return yaml ?: throw IllegalStateException("Setup replica set yaml is null.")
    }

    override fun healthcheck(): Boolean {
        Thread.sleep(5000)
        return true
    }

    override fun getHealthcheckFailureThreshold(): Long {
        return 60L;
    }

    override fun getHealthcheckPeriodSeconds(): Long {
        return 3L;
    }
}
