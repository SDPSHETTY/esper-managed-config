package com.example.myapplication.utils

import org.junit.Test
import org.junit.Assert.*

class ResultTest {

    @Test
    fun `success result holds data correctly`() {
        val data = "test data"
        val result = Result.success(data)

        assertTrue(result is Result.Success)
        assertEquals(data, result.getOrNull())
        assertEquals(data, result.getOrThrow())
    }

    @Test
    fun `error result holds exception correctly`() {
        val exception = RuntimeException("test exception")
        val result = Result.error<String>(exception)

        assertTrue(result is Result.Error)
        assertNull(result.getOrNull())
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test(expected = RuntimeException::class)
    fun `error result throws exception on getOrThrow`() {
        val exception = RuntimeException("test exception")
        val result = Result.error<String>(exception)

        result.getOrThrow()
    }

    @Test
    fun `map transforms success value`() {
        val result = Result.success(5)
        val mapped = result.map { it * 2 }

        assertTrue(mapped is Result.Success)
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `map preserves error`() {
        val exception = RuntimeException("test exception")
        val result = Result.error<Int>(exception)
        val mapped = result.map { it * 2 }

        assertTrue(mapped is Result.Error)
        assertEquals(exception, (mapped as Result.Error).exception)
    }

    @Test
    fun `onSuccess executes action for success`() {
        var executed = false
        val result = Result.success("test")

        result.onSuccess { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `onSuccess does not execute action for error`() {
        var executed = false
        val result = Result.error<String>(RuntimeException())

        result.onSuccess { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onError executes action for error`() {
        var executed = false
        val result = Result.error<String>(RuntimeException("test"))

        result.onError { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `onError does not execute action for success`() {
        var executed = false
        val result = Result.success("test")

        result.onError { executed = true }

        assertFalse(executed)
    }
}