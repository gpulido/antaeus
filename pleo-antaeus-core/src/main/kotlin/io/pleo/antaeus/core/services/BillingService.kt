package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.NotificationProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.time.LocalDateTime
import java.util.stream.Collectors

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val notificationProvider: NotificationProvider
) {

    fun chargeAllPendingInvoices(): Int {
        // We just returns the number of errors
        // We use parallelStream to avoid adding kotlinx
        return invoiceService
                .fetchAllPendingInvoices()
                .parallelStream()
                .map { chargeInvoice(it)}
                .map { if(it) 0 else 1 }
                .collect(Collectors.summingInt { it })
    }

    fun chargeInvoice(invoice : Invoice, numRetries: Int = 3, sleepTime: Long = 1000): Boolean {
        try {
            if (paymentProvider.charge(invoice)) {
                // update the status after charging. If this fails and the invoice doesn't
                // get updated, it will stay as Pending, although it already has been charge.
                // So it eventually could be tried to be charged again, but we are assuming
                // that the paymentProvider handles it.
                invoiceService.updateInvoiceStatus(invoice.id, status = InvoiceStatus.PAID)
                notificationProvider.notifyChargeResult(LocalDateTime.now(),invoice, "Success")
                return true
            }
            notificationProvider.notifyChargeResult(LocalDateTime.now(),invoice, "NotEnoughFounds")
        }
        catch (e: CustomerNotFoundException)
        {
            notificationProvider.notifyChargeResult(LocalDateTime.now(),invoice, "CustomerNotFound")
            return false
        }
        catch (e: CurrencyMismatchException)
        {
            notificationProvider.notifyChargeResult(LocalDateTime.now(),invoice, "CurrencyMismatch")
            return false
        }
        catch (e: NetworkException)
        {
            // not need to do anything. If needed it will be retried by the calling api
            if (numRetries == 0) {
                notificationProvider.notifyChargeResult(LocalDateTime.now(), invoice, "NetworkError")
                return false
            }
            // Sleep to try again
            Thread.sleep(sleepTime)
            return chargeInvoice(invoice, numRetries-1,  sleepTime * 2 )
        }
        return false
    }
}
