package com.swc.scheduled_job

import com.swc.repository.UserRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter

@EnableAsync
@Component
class OnlineUsersTracker(val userRepository: UserRepository) {

    @Async
    @Scheduled(fixedRate = 5000)
    fun scheduleFixedRateTaskAsync() {
        userRepository
            .findAll()
            .onEach {
                val oldState = it.isOnline
                it.isOnline = LocalDateTime.parse(it.lastOnline, DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")).plusSeconds(5).isAfter(now())
                if(oldState != it.isOnline) {
                    println("${it.username} is ${if(it.isOnline) "online" else "offline"}")
                }
            }
            .forEach(userRepository::save)
    }
}
