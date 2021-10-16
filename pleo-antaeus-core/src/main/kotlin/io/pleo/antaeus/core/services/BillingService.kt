package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.NotificationProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.time.LocalDateTime

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val notificationProvider: NotificationProvider
) {

    fun chargeAllPendingInvoices(): Int {
        // We just returns the number of sucessfully done
        return invoiceService
                .fetchAllPendingInvoices()
                .map { chargeInvoice(it)}
                .map { if(it) 0 else 1 }
                .sum()
    }

    fun chargeInvoice(invoice : Invoice): Boolean {
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
            return false
        }
        return false
    }
}
