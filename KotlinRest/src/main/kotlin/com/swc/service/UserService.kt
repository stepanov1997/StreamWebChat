package com.swc.service

import com.swc.model.User
import com.swc.model.UserUserModel
import com.swc.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class UserService(@Autowired val userRepository: UserRepository) {

    fun addUser(u: User): User? {
        val userList = userRepository.findByUsername(u.username)
        if (userList?.isNotEmpty()!!) {
            return null
        }
        return userRepository.insert(u)
    }

    fun getUser(userId: Int): User? = userRepository.findById(userId).orElse(null)
    fun getUsers(): MutableList<User> = userRepository.findAll()

    fun login(username: String, password: String): UserUserModel? {
        val userList = userRepository.findByUsername(username)
        if ((userList != null) && userList.isEmpty()) {
            return null
        }
        val user = userList?.first() ?: return null
        return if (user.password == password) {
            user.lastOnline = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")
            )
            user.isOnline = true;
            userRepository.save(user)
            UserUserModel(user)
        } else null
    }


    fun getOnlineUsers(): List<User> {
        return userRepository.findAll().stream().filter { it.isOnline }.toList()
    }
}
