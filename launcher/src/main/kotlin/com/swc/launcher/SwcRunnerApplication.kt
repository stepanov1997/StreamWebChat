package com.swc.launcher

import com.swc.it.ITLauncher
import com.swc.runner.Runner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import kotlin.system.exitProcess

@SpringBootApplication(
    scanBasePackages = ["com.swc.services"],
    exclude = [
        DataSourceAutoConfiguration::class,
        MongoAutoConfiguration::class,
    ]
)
class SwcRunnerApplication(val runner: Runner, val itLauncher: ITLauncher) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val isIts = args.asList().any { "it".equals(it, true) }
        val success = if(isIts) itLauncher.executeIntegrationTests() else runner.run()
        exitProcess(if(success) 0 else 1)
    }
}

fun main(args: Array<String>) {
    val application = SpringApplication(SwcRunnerApplication::class.java)
    application.webApplicationType = WebApplicationType.NONE
    application.run(*args)
}
