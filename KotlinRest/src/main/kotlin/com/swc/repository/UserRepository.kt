package com.swc.repository

import com.swc.model.User
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, Int>{
    fun findUserByUsername(username: String) : User?
}

