package com.swc.service

import com.swc.model.User
import com.swc.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Stream

@Service
class UserService(@Autowired val userRepository : UserRepository) {

    var currentUser : User? = null

    fun addUser(u: User): User? {
        if(userRepository.findUserByUsername(u.username) != null) {
            return null
        }
        return userRepository.insert(u)
    }
    fun getUser(userId: Int): User? = userRepository.findById(userId).orElseGet(null)
    fun getUsers(): MutableList<User> = userRepository.findAll()

    fun login(u: User): User? {
        val user = userRepository.findUserByUsername(u.username)
        if (user != null) {
            if (user.password == u.password) {
                currentUser = user
                return user
            }
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
}
