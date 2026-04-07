package com.letta.mobile.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResultTest {

    @Test
    fun apiException_storesCodeAndMessage() {
        val exception = ApiException(404, "Not Found")

        assertEquals(404, exception.code)
        assertEquals("Not Found", exception.message)
    }

    @Test
    fun apiResultSuccess_wrapsDataCorrectly() {
        val data = "test data"
        val result = ApiResult.Success(data)

        assertEquals(data, result.data)
    }

    @Test
    fun apiResultSuccess_wrapsComplexDataCorrectly() {
        data class TestData(val id: String, val value: Int)
        val testData = TestData("abc", 123)
        val result = ApiResult.Success(testData)

        assertEquals(testData, result.data)
        assertEquals("abc", result.data.id)
        assertEquals(123, result.data.value)
    }

    @Test
    fun apiResultApiError_holdsCodeAndMessage() {
        val result = ApiResult.ApiError(500, "Internal Server Error")

        assertEquals(500, result.code)
        assertEquals("Internal Server Error", result.message)
    }

    @Test
    fun apiResultNetworkError_holdsException() {
        val exception = RuntimeException("Connection failed")
        val result = ApiResult.NetworkError(exception)

        assertEquals(exception, result.exception)
        assertEquals("Connection failed", result.exception.message)
    }

    @Test
    fun apiResult_successIsOfCorrectType() {
        val result: ApiResult<String> = ApiResult.Success("test")

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun apiResult_apiErrorIsOfCorrectType() {
        val result: ApiResult<Nothing> = ApiResult.ApiError(400, "Bad Request")

        assertTrue(result is ApiResult.ApiError)
    }

    @Test
    fun apiResult_networkErrorIsOfCorrectType() {
        val exception = java.io.IOException("Network timeout")
        val result: ApiResult<Nothing> = ApiResult.NetworkError(exception)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun apiException_extendsException() {
        val exception = ApiException(403, "Forbidden")

        assertTrue(exception is Exception)
    }

    @Test
    fun apiResultApiError_withEmptyMessage() {
        val result = ApiResult.ApiError(204, "")

        assertEquals(204, result.code)
        assertEquals("", result.message)
    }

    @Test
    fun apiResultNetworkError_withDifferentExceptionTypes() {
        val ioException = java.io.IOException("IO failed")
        val result1 = ApiResult.NetworkError(ioException)

        val runtimeException = RuntimeException("Runtime error")
        val result2 = ApiResult.NetworkError(runtimeException)

        assertTrue(result1.exception is java.io.IOException)
        assertTrue(result2.exception is RuntimeException)
    }
}
