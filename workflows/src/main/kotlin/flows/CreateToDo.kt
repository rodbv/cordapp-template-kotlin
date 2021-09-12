package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.DummyToDoCommand
import com.template.states.ToDoState
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class CreateToDoFlow(private val taskDescription: String) : FlowLogic<Void?>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {
        val serviceHub = serviceHub
        val me = ourIdentity
        val toDoState = ToDoState(me, me, taskDescription)

        println("Linear ID of state is ${toDoState.linearId}")

        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val tb = TransactionBuilder(notary).addCommand(DummyToDoCommand(), me.owningKey).addOutputState(toDoState)
        val stx = serviceHub.signInitialTransaction(tb)
        subFlow(FinalityFlow(stx, emptySet<FlowSession>()))

        return null
    }
}

