package com.swc.service

import com.swc.model.DatabaseSequence
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.MongoOperations

@ExtendWith(MockitoExtension::class)
class TestSequenceGenerateServices(
    @Mock val mongoOperations: MongoOperations
){
    lateinit var sequenceGenerateServices: SequenceGenerateServices

    @BeforeEach
    fun setUp(){
        sequenceGenerateServices = SequenceGenerateServices(mongoOperations)
    }

    @Test
    fun `Test when function generateSequence return regular number`() {
        whenever(mongoOperations.findAndModify(anyOrNull(), anyOrNull(), anyOrNull(), eq(DatabaseSequence::class.java))).thenReturn(DatabaseSequence("3",4L))
        val sequenceGenerateServices = SequenceGenerateServices(mongoOperations)
        val generateSequence = sequenceGenerateServices.generateSequence("12")

        verify(mongoOperations, Mockito.times(1)).findAndModify(anyOrNull(), anyOrNull(), anyOrNull(), eq(DatabaseSequence::class.java))
        Assertions.assertEquals(4L, generateSequence)
    }

    @Test
    fun `Test when function generateSequence returns 1 for null value of database sequence`() {
        whenever(mongoOperations.findAndModify(anyOrNull(), anyOrNull(), anyOrNull(), eq(DatabaseSequence::class.java))).thenReturn(null)
        val sequenceGenerateServices = SequenceGenerateServices(mongoOperations)
        val generateSequence = sequenceGenerateServices.generateSequence("12")

        verify(mongoOperations, Mockito.times(1)).findAndModify(anyOrNull(), anyOrNull(), anyOrNull(), eq(DatabaseSequence::class.java))
        Assertions.assertEquals(1L, generateSequence)
    }

    @Test
    fun `Test when function generateSequence throws exception`() {
        whenever(mongoOperations.findAndModify(anyOrNull(), anyOrNull(), anyOrNull(), eq(DatabaseSequence::class.java))).thenThrow(RuntimeException())
        val sequenceGenerateServices = SequenceGenerateServices(mongoOperations)

        Assertions.assertThrows(RuntimeException::class.java) {sequenceGenerateServices.generateSequence("12")}
    }
}
