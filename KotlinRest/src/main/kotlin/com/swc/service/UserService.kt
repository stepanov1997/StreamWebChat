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

    var currentUser: User? = null

    fun addUser(u: User): User? {
        if (userRepository.containsByUsername(u.username)) {
            return null
        }
        return userRepository.insert(u)
    }

    fun getUser(userId: Int): User? = userRepository.findById(userId).orElse(null)
    fun getUsers(): MutableList<User> = userRepository.findAll()

    fun login(username: String, password: String): UserUserModel? {
        if (!userRepository.containsByUsername(username)) {
            return null
        }
        val user = userRepository.findByUsername(username)!!.first()
        return if (user.password == password) {
            user.lastOnline = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")
            );
            user.isOnline = true;
            userRepository.save(user)
            currentUser = user
            UserUserModel(user)
        } else null
    }

//    // TODO: Obrisati kad se implementira logout
//    fun logout(): Void? {
//        currentUser = null;
//        return null
//    }
//
//    // TODO: Obrisati kad se implementira imAliveSignal
//    fun imAliveSignal(): Any {
//        currentUser?.lastOnline = LocalDateTime.now().format(
//            DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")
//        );
//        currentUser?.isOnline = true;
//        userRepository.save(currentUser!!)
//        return UserUserModel(currentUser!!)
//    }

    fun getOnlineUsers(): List<User> {
        return userRepository.findAll().stream().filter { it.isOnline }.toList()
    }
}
