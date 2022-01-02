package com.swc.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
class User(@Id var id : Int,
           val username: String,
           val password: String,
           val isOnline: Boolean,
           @Transient val sequenceName: String = "users_sequence")

class UserUserModel(val id: Int, val username: String, val isOnline: Boolean){
    constructor(user: User) : this(user.id, user.username, user.isOnline)
}
