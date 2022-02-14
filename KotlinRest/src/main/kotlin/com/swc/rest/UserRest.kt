package com.swc.rest

import com.swc.model.User
import com.swc.model.UserUserModel
import com.swc.service.SequenceGenerateServices
import com.swc.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    fun register(@RequestBody requestBody: HashMap<String, String>): ResponseEntity<User> {
        val name = requestBody["name"] ?: return ResponseEntity.badRequest().build()
        val surname = requestBody["surname"] ?: return ResponseEntity.badRequest().build()
        val username = requestBody["username"] ?: return ResponseEntity.badRequest().build()
        val password = requestBody["password"] ?: return ResponseEntity.badRequest().build()
        val user = User(
            sequenceGenerateServices.generateSequence("users_sequence").toInt(),
            name,
            surname,
            username,
            password,
            false,
            LocalDateTime.now().minus(1, ChronoUnit.DAYS).format(DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm:ss")),
            "users_sequence"
        );
        val userResponse = userService.addUser(user) ?: return ResponseEntity.status(HttpStatus.CONFLICT).build()
        return ResponseEntity.created(URI.create("/user/get?userId=${userResponse.id}")).body(userResponse)
    }

    @PostMapping("login")
    fun login(@RequestBody requestBody: HashMap<String, String>): ResponseEntity<UserUserModel?> {
        val username = requestBody["username"] ?: return ResponseEntity.badRequest().build()
        val password = requestBody["password"] ?: return ResponseEntity.badRequest().build()

        val user = userService.login(username, password) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user);
    }
}


