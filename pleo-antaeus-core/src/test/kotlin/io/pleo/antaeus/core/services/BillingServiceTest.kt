package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {


    private val invoice = Invoice(1, 1, Money(BigDecimal(10), Currency.EUR), InvoiceStatus.PENDING)
    private val invoice2 = Invoice(2, 1, Money(BigDecimal(10), Currency.GBP), InvoiceStatus.PENDING)
    private val invoice3 = Invoice(3, 2, Money(BigDecimal(10), Currency.EUR), InvoiceStatus.PENDING)
    private val invoice4= Invoice(4, 3, Money(BigDecimal(10), Currency.EUR), InvoiceStatus.PENDING)
    private val invoice5 = Invoice(5, 4, Money(BigDecimal(10), Currency.EUR), InvoiceStatus.PENDING)

    private val allGoodInvoices = listOf(invoice, invoice2, invoice3, invoice4, invoice5)

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(invoice) } returns true
        every { charge(invoice2)} throws CurrencyMismatchException(invoice2.id, invoice2.customerId)
        every { charge(invoice3)} returns false
        every { charge(invoice4)} throws CustomerNotFoundException(invoice4.id)
    }

    private val invoiceService = mockk<InvoiceService>(relaxed = true){
        every { getPendingInvoices()} returns allGoodInvoices
    }

    private val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

    @Test
    fun `will charge a pending invoice`() {
        assertEquals(billingService.chargeInvoice(invoice), true)
    }

    @Test
    fun `will not charge an invoice with wrong currency`() {
        assertEquals(billingService.chargeInvoice(invoice2), false)
    }

    @Test
    fun `will delay charging a pending invoice with not balance`() {
        assertEquals(billingService.chargeInvoice(invoice3), false)
    }

    @Test
    fun `will delay charging a pending invoice with missing customer`() {
        assertEquals(billingService.chargeInvoice(invoice4), false)
    }
    // chargeAllPendingInvoices
    @Test
    fun `will charge all pending invoices`(){
        // there are only two invoices that are going to be charged
        assertEquals(billingService.chargeAllPendingInvoices(), 2)
    }



}