package com.swc.repository

import com.swc.model.Message
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ChatRepository : MongoRepository<Message, Int> {
    @Query("{ \$or: [{ \"senderUsername\" : ?0, \"receiverUsername\" : ?1 }, { \"senderUsername\" : ?1, \"receiverUsername\" : ?0 }]}")
    fun findAllBySenderUsernameAndReceiverUsername(senderUsername: String, receiverUsername: String, pageable: Pageable): Page<Message>
}

