package com.swc.service

import com.mongodb.MongoClientException
import com.swc.model.User
import com.swc.model.UserUserModel
import com.swc.repository.UserRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*
import java.util.stream.IntStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class TestUserService(
    @Mock val userRepository: UserRepository
) {

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository)
    }

    @Test
    fun `Test addUser method - username exists`() {
        val u = User(1, "", "", "test", "test", true, "test", "test")
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(listOf(u))
        val addUser = userService.addUser(u)

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        verify(userRepository, Mockito.times(0)).insert(eq(u))
        assertNull(addUser)
    }

    @Test
    fun `Test addUser method - userRepository returns null`() {
        val u = User(1, "", "", "test", "test", true, "test", "test")
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(null)
        Assertions.assertThrows(NullPointerException::class.java) {userService.addUser(u)}

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        verify(userRepository, Mockito.times(0)).insert(eq(u))
    }

    @Test
    fun `Test addUser method - insert successful`() {
        val u = User(1, "", "", "test", "test", true, "test", "test")
        whenever(userRepository.findByUsername(any())).thenReturn(listOf())
        whenever(userRepository.insert(eq(u))).thenReturn(u)
        val addUser = userService.addUser(u)

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        verify(userRepository, Mockito.times(1)).insert(eq(u))
        assertEquals(u, addUser)
    }

    @Test
    fun `Test addUser method - insert unsuccessful`() {
        val u = User(1, "", "", "test", "test", true, "test", "test")
        whenever(userRepository.findByUsername(any())).thenReturn(listOf(u))
        whenever(userRepository.insert(eq(u))).thenReturn(null)
        val addUser = userService.addUser(u)

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        verify(userRepository, Mockito.times(0)).insert(eq(u))
        assertNull(addUser)
    }

    @Test
    fun `Test getUser method - user doesn't exists`() {
        whenever(userRepository.findById(any())).thenReturn(Optional.empty())
        val getUser = userService.getUser(1)

        verify(userRepository, Mockito.times(1)).findById(any())
        assertNull(getUser)
    }

    @Test
    fun `Test getUser method - user exists`() {
        val u = User(1, "", "", "test", "test", true, "test", "test")
        whenever(userRepository.findById(any())).thenReturn(Optional.of(u))
        val getUser = userService.getUser(1)

        verify(userRepository, Mockito.times(1)).findById(any())
        assertNotNull(getUser)
        assertEquals(u, getUser)
    }

    @Test
    fun `Test getUsers method - user exists`() {
        val u = User(1, "", "", "test", "test", true, "test", "test")
        val users = IntStream.range(1, 5).mapToObj { u }.toList()
        whenever(userRepository.findAll()).thenReturn(users)
        val getUsers = userService.getUsers()

        verify(userRepository, Mockito.times(1)).findAll()
        assertNotNull(getUsers)
        assertArrayEquals(users.toTypedArray(), getUsers.toTypedArray())
    }

    @Test
    fun `Test login method - username doesn't exists`() {
        val u = User(1, "", "", "test", "test", true, "test", "test")
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(listOf())
        val userModel = userService.login("test", "test")

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        assertNull(userModel)
    }

    @Test
    fun `Test login method - wrong password`() {
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(
            listOf(
                User(1, "", "", "test", "test2", true, "test", "test"),
                User(2, "", "", "test2", "test2", true, "test2", "test2")
            )
        )
        val userModel = userService.login("test", "test")

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        assertNull(userModel)
    }

    @Test
    fun `Test login method - isOnline false`() {
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(
            listOf(
                User(1, "", "", "test", "test2", true, "test", "test"),
                User(2, "", "", "test2", "test2", false, "test2", "test2")
            )
        )
        val userModel = userService.login("test", "test2")

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        assertNotNull(userModel)
        assertEquals(userModel.username, "test")
    }

    @Test
    fun `Test login method - saving with error`() {
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(
            listOf(
                User(1, "", "", "test", "test", true, "test", "test"),
                User(2, "", "", "test2", "test2", true, "test2", "test2")
            )
        )
        whenever(userRepository.save(ArgumentMatchers.any(User::class.java))).thenThrow(RuntimeException())

        Assertions.assertThrows(RuntimeException::class.java) { userService.login("test", "test") }
        verify(userRepository, Mockito.times(1)).findByUsername(any())
        verify(userRepository, Mockito.times(1)).save(any())
    }

    @Test
    fun `Test login method - throws exception`() {
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenThrow(MongoClientException(""))
        whenever(userRepository.save(ArgumentMatchers.any(User::class.java))).thenThrow(RuntimeException())

        Assertions.assertThrows(MongoClientException::class.java) { userService.login("test", "test") }
        verify(userRepository, Mockito.times(1)).findByUsername(any())
        verify(userRepository, Mockito.times(0)).save(any())
    }

    @Test
    fun `Test login method - saving successful`() {
        whenever(userRepository.findByUsername(ArgumentMatchers.anyString())).thenReturn(
            listOf(
                User(1, "", "", "test", "test", true, "test", "test"),
                User(2, "", "", "test2", "test2", true, "test2", "test2")
            )
        )
        whenever(userRepository.save(ArgumentMatchers.any(User::class.java))).thenReturn(null)

        val user = userService.login("test", "test")

        verify(userRepository, Mockito.times(1)).findByUsername(any())
        verify(userRepository, Mockito.times(1)).save(any())
        assertNotNull(user)
        assertEquals(
            user, UserUserModel(
                User(1, "", "", "test", "test", true, user.lastOnline, "test")
            )
        )
    }

    @Test
    fun `Test getOnlineUsers method`() {
        whenever(userRepository.findAll()).thenReturn(
            listOf(
                User(1, "", "", "test", "test", true, "test", "test"),
                User(2, "", "", "test2", "test2", false, "test2", "test2"),
                User(3, "", "", "test3", "test3", true, "test3", "test3")
            )
        )
        val users = userService.getOnlineUsers()

        verify(userRepository, Mockito.times(1)).findAll()
        assertArrayEquals(
            users.toTypedArray(),
            arrayOf(
                User(1, "", "", "test", "test", true, "test", "test"),
                User(3, "", "", "test3", "test3", true, "test3", "test3")
            )
        )
    }

    @Test
    fun `Test getOnlineUsers method - throws exception`() {
        whenever(userRepository.findAll()).thenThrow(RuntimeException())

        Assertions.assertThrows(RuntimeException::class.java) { userService.getOnlineUsers() }
        verify(userRepository, Mockito.times(1)).findAll()
    }
}
