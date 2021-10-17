package io.pleo.antaeus.scheduler

import io.mockk.mockk
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

internal class UtilsTest {


    @Test
    fun `will return the right milliseconds until tomorrow`() {
        val now: LocalDateTime = LocalDateTime.of(2021,
                Month.MARCH, 2, 0, 30, 40)
        assertEquals(84620000, getTomorrow(now))
    }
}