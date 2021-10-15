package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
// TODO - Add code e.g. here

    fun chargeInvoice(invoice : Invoice): Invoice {
        try {
            if (paymentProvider.charge(invoice)) {
                invoiceService.updateInvoiceStatus(invoice.id, status = InvoiceStatus.PAID)
            }
            // TODO:
        }
        catch (e: CustomerNotFoundException)
        {
            // TODO:
        }
        catch (e: CurrencyMismatchException)
        {
            // TODO:
        }
        catch (e: InvoiceNotFoundException)
        {
            // TODO:
        }
        return invoice
    }
}
