package com.swc.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "messages")
class Message(@Id var id: Int,
           val sender: User,
           val receiver: User,
           val text: String,
           @Transient val sequenceName: String = "message_sequence")
