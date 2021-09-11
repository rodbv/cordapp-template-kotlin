package com.learncorda.tododist.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class ToDoContract : Contract {
    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {}

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
        class Assign : Commands
    }

    companion object {
        // This is used to identify our contract when building a transaction.
        const val ID = "TemplateContract"
    }
}