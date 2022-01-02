package com.swc.rest

import com.swc.model.User
import com.swc.model.UserUserModel
import com.swc.service.SequenceGenerateServices
import com.swc.service.UserService
import org.springframework.boot.context.properties.bind.Bindable.mapOf
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("user")
@CrossOrigin(origins = ["*"])
class UserRest(val userService: UserService, val sequenceGenerateServices: SequenceGenerateServices) {

    @GetMapping("get")
    fun getUser(@RequestParam userId: Int) = ResponseEntity.ok(userService.getUser(userId))

    @GetMapping("getAll")
    fun getUsers() = ResponseEntity.ok(userService.getUsers())

    @GetMapping("online")
    fun getOnlineUsers() = ResponseEntity.ok(userService.getOnlineUsers())

    @PostMapping("register")
    fun register(@RequestBody u: User): ResponseEntity<User> {
        u.id = sequenceGenerateServices.generateSequence("users_sequence").toInt()
        val user = userService.addUser(u) ?: return ResponseEntity.status(HttpStatus.CONFLICT).build()
        return ResponseEntity.created(URI.create("/user/get?userId=${u.id}")).body(user)
    }

    @PostMapping("login")
    fun login(@RequestBody u: User): ResponseEntity<UserUserModel?> {
        val user = userService.login(u) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user);
    }

    @PostMapping("logout")
    fun logout() = ResponseEntity.ok(userService.logout())

    @GetMapping("current")
    fun currentUser() = if (userService.currentUser == null) ResponseEntity.notFound().build() else ResponseEntity.ok(userService.currentUser?.username)

    @PostMapping("imAlive")
    fun imAliveSignal() = ResponseEntity.ok(userService.imAliveSignal())

}


