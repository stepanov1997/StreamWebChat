package com.swc.services

import com.swc.runner.KubernetesDeployment
import com.swc.runner.RunnerComponent
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


@Service
@RunnerComponent(value = "zookeeper", dependsOn = [])
class ZookeeperService : KubernetesDeployment() {

    @Value("\${kubernetes.zookeeper.host}:\${kubernetes.zookeeper.port}")
    var zookeeperUrl: String? = null

    private var yaml: String? = null

    @PostConstruct
    @Throws(IOException::class)
    private fun init() {
        val deploymentYamlPath =
//            "k8s/client" + (if (deletingNamespaceAfterIts) "" else "-debug") + ".yaml"
            "k8s/zookeeper.yaml"
        val deploymentYamlResource = ClassPathResource(deploymentYamlPath)
        yaml = String(deploymentYamlResource.inputStream.readAllBytes())
    }

    override fun getDeploymentYaml(): String {
        return yaml ?: throw IllegalStateException("Zookeeper yaml is null.")
    }

    override fun healthcheck(): Boolean {
        try {
            val curatorFramework = CuratorFrameworkFactory.newClient(
                zookeeperUrl,
                ExponentialBackoffRetry(100, 6)
            )
            curatorFramework.start()
            try {
                curatorFramework.blockUntilConnected(2, TimeUnit.SECONDS)
                if (curatorFramework.zookeeperClient.isConnected) {
                    return true
                }
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            curatorFramework.close()
            return false
        } catch (_: Exception) {
            return false
        }
    }

    override fun getHealthcheckFailureThreshold(): Long {
        return 60L;
    }

    override fun getHealthcheckPeriodSeconds(): Long {
        return 3L;
    }
}
