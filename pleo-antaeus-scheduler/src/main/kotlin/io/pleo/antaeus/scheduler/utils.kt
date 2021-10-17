package io.pleo.antaeus.scheduler

import mu.KotlinLogging
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

val logger = KotlinLogging.logger {}
/**
 * Makes a post call to the charge_invoice endpoint of the localhost.
 * NOTE: The end point is hardcoded as an example. In a real deployment environment variables should
 * be used. For avoid adding more dependencies,  the java.net.http implementation is used
 *
 * returns: An integer with the number of failing invoices charged. -1 if there was any exception
 */
internal fun chargePendingInvoices(): Int {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:7000/rest/v1/invoices/charge_pending"))
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build()
    return try {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        response.body().toInt()
    } catch (e: IOException) {
        // there was an error
        -1
    }
}

internal fun getTomorrow(date: LocalDateTime): Long {
    // get the number of milliseconds between now and 00:01 of tomorrow
    val tomorrow = date.plusDays(1).truncatedTo(ChronoUnit.DAYS).plusMinutes(1)
    return ChronoUnit.MILLIS.between(date, tomorrow)
}

fun chargeIfFirstOfMonth(force: Boolean = false): LocalDateTime
{
    val date = LocalDateTime.now()
    logger.info {"Checking date at $date"}
    if (date.dayOfMonth == 1 || force)
    {
        // more logic could be added here
        val failedInvoices = chargePendingInvoices()
        if (failedInvoices != -1) {
            logger.info { "$failedInvoices could not be charged" }
        }
        else {
            logger.info {"There was an error trying to charge invoices"}
        }

    }
    return date
}