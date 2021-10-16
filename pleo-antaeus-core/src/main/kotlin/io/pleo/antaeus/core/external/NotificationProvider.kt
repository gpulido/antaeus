package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Invoice
import java.time.LocalDateTime

interface NotificationProvider {

    /**
     * Very basic interface to inform of messages for the billing service
     * An enum could be added to model for the result
     */
    fun notifyChargeResult(time: LocalDateTime, invoice: Invoice, result:String)

}