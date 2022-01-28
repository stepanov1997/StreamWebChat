package com.swc.service

import com.swc.repository.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Component
class ScheduledTasks(private val userRepository: UserRepository) {

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    fun reportCurrentTime() {
        try {
            userRepository.findAll()
                .filter { LocalDateTime.now().isBefore(LocalDateTime.parse(it.lastOnline).plusSeconds(12)) }
                .onEach { it.isOnline = true }
                .forEach(userRepository::save)
        } catch (e: Exception) {
            println(e.message)
        }
    }
}
