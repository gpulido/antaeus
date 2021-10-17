package io.pleo.antaeus.scheduler
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.concurrent.fixedRateTimer


fun initScheduler() {
    logger.info {"Initializing scheduler"}
    // first we check if we are at first of the month and have to charge
    chargeIfFirstOfMonth()
    logger.info{"will try again tomorrow"}
    // Very basic implementation of a daily scheduler
    val date = LocalDateTime.now()
    val oneDay = Duration.of(1, ChronoUnit.DAYS).toMillis()
    val tomorrow = getTomorrow(date)
    fixedRateTimer("scheduler", initialDelay = tomorrow, period = oneDay) {
        // if we want to do more things each day (like try to charge again
        // the failing invoices, logic could be added here)
        chargeIfFirstOfMonth()
        logger.info{"will try again tomorrow"}
    }
}


