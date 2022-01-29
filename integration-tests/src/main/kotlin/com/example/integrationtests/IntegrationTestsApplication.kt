package com.example.integrationtests

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class
])
class IntegrationTestsApplication : CommandLineRunner {
    override fun run(vararg args: String?) {
        TODO("Not yet implemented")
    }
}

fun main(args: Array<String>) {
    runApplication<IntegrationTestsApplication>(*args)
}
