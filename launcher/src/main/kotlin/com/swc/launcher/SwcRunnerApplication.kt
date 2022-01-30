package com.swc.launcher

import com.swc.it.ITLauncher
import com.swc.runner.Runner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(
    scanBasePackages = ["com.swc.services"],
    exclude = [
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class
    ]
)
class SwcRunnerApplication(val runner: Runner, val itLauncher: ITLauncher) : CommandLineRunner {
    override fun run(vararg args: String?) {
       args.asList()
           .any{ "it".equals(it, true) }
           .let { if(it) itLauncher.executeIntegrationTests() else runner.run() }
    }
}

fun main(args: Array<String>) {
    val application = SpringApplication(SwcRunnerApplication::class.java)
    application.webApplicationType = WebApplicationType.NONE
    application.run(*args)
}
