package com.swc.service

import com.swc.model.User
import com.swc.model.UserUserModel
import com.swc.repository.UserRepository
import com.swc.repository.containsByUsername
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Stream

@Service
class UserService(@Autowired val userRepository : UserRepository) {

    var currentUser : User? = null

    fun addUser(u: User): User? {
        if(userRepository.containsByUsername(u.username)) {
            return null
        }
        return userRepository.insert(u)
    }
    fun getUser(userId: Int): User? = userRepository.findById(userId).orElseGet(null)
    fun getUsers(): MutableList<User> = userRepository.findAll()

    fun login(u: User): UserUserModel? {
        if(!userRepository.containsByUsername(u.username)) {
            return null
        }
        val user = userRepository.findByUsername(u.username).firstOrNull()
        if(user != null && user.password == u.password) {
            currentUser = user
            return UserUserModel(user)
        }
        return null;
    }

    fun logout(): Void? {
        currentUser = null;
        return null
    }

    fun imAliveSignal(): Any {
        return "I'm alive!";
    }

    fun getOnlineUsers(): Any {
        return userRepository.findAll().stream().filter { it.isOnline }
    }
}
