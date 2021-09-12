package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import org.jetbrains.annotations.NotNull

@InitiatedBy(AssignToDoInitiator::class)
class AssignToDoResponder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        println("responder called")
        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(@NotNull stx: SignedTransaction) {
                println("check!!")
            }
        }
        val stx = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, stx.id))
    }
}