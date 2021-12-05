package com.swc.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
class User(@Id val id: Int, val username: String, val password: String) {
    constructor() : this(0, "", "")
}
