package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    fun chargeAllPendingInvoices(): Int {
        // We just returns the number of sucessfully done
        return invoiceService
                .fetchAllPendingInvoices()
                .map { if (chargeInvoice(it)) 1 else 0 }
                .sum()
    }

    fun chargeInvoice(invoice : Invoice): Boolean {
        try {
            if (paymentProvider.charge(invoice)) {
                // update the status after charging. If this fails and the invoice doesn't
                // get updated, it will stay as Pending although it already has been charge.
                // So it eventually could be tried to be charged again but we are assumming
                // that the paymentProvider handles it.
                invoiceService.updateInvoiceStatus(invoice.id, status = InvoiceStatus.PAID)
                //TODO: Maybe notify to a "LoggerService"
                return true
            }
        }
        catch (e: CustomerNotFoundException)
        {
            // TODO: Notify to a "LoggerService"
            return false
        }
        catch (e: CurrencyMismatchException)
        {
            // TODO: Notify and do nothing
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
