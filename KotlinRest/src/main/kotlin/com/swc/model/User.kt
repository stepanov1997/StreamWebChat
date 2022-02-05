package com.swc.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class User(
    @Id var id: Int,
    val username: String,
    val password: String,
    var isOnline: Boolean,
    var lastOnline: String,
    @Transient val sequenceName: String = "users_sequence"
)

data class UserUserModel(val id: Int, val username: String, val isOnline: Boolean, val lastOnline: String) {
    constructor(user: User) : this(user.id, user.username, user.isOnline, user.lastOnline)
}
