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
@RunnerComponent(value = "react-client", dependsOn = [])
class ReactClientService : KubernetesDeployment() {

    @Value("http://\${kubernetes.react-client.host}:\${kubernetes.react-client.port}")
    var reactUrl: String? = null

    @Value("\${beechbone.afterall.deletenamespace:true}")
    var deletingNamespaceAfterIts = false

    private var yaml: String? = null

    @PostConstruct
    @Throws(IOException::class)
    private fun init() {
        val bidderBeaconDeploymentYamlPath =
//            "k8s/client" + (if (deletingNamespaceAfterIts) "" else "-debug") + ".yaml"
            "k8s/client.yaml"
        val bidderBeaconDeploymentYamlResource = ClassPathResource(bidderBeaconDeploymentYamlPath)
        yaml = String(bidderBeaconDeploymentYamlResource.inputStream.readAllBytes())
    }

    override fun getDeploymentYaml(): String {
        return yaml ?: throw IllegalStateException("React yaml is null.")
    }

    override fun healthcheck(): Boolean {
        return RestTemplate()
            .getForEntity(reactUrl ?: throw IllegalStateException("React Url is null."), String::class.java)
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
