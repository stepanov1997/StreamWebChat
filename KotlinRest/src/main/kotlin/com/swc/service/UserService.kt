package com.swc.service

import com.swc.model.User
import com.swc.model.UserUserModel
import com.swc.repository.UserRepository
import com.swc.repository.containsByUsername
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Stream

@Service
class UserService(@Autowired val userRepository: UserRepository) {

    var currentUser: User? = null

    fun addUser(u: User): User? {
        if (userRepository.containsByUsername(u.username) != false) {
            return null
        }
        return userRepository.insert(u)
    }

    fun getUser(userId: Int): User? = userRepository.findById(userId).orElseGet(null)
    fun getUsers(): MutableList<User> = userRepository.findAll()

    fun login(username: String, password: String): UserUserModel? {
        if (!userRepository.containsByUsername(username)) {
            return null
        }
        val user = userRepository.findByUsername(username)?.firstOrNull() ?: return null
        return if (user.password == password) {
            currentUser = user
            user.lastOnline = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")
            );
            user.isOnline = true;
            userRepository.save(user)
            UserUserModel(user)
        } else null
    }

    fun logout(): Void? {
        currentUser = null;
        return null
    }

    fun imAliveSignal(): Any {
        currentUser?.lastOnline = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")
        );
        currentUser?.isOnline = true;
        userRepository.save(currentUser!!)
        return UserUserModel(currentUser!!)
    }

    fun getOnlineUsers(): Any {
        return userRepository.findAll().stream().filter { it.isOnline }
    }
}
