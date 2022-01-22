package com.swc.repository

import com.swc.model.Message
import org.springframework.data.mongodb.repository.MongoRepository

interface ChatRepository : MongoRepository<Message, Int> {
}

