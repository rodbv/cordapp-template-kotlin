package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.DummyToDoCommand
import com.template.states.ToDoState
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
class MarkToDoAsDone(private val linearId: String): FlowLogic<Void?>() {
    @Suspendable
    override fun call(): Void? {
        val sb = serviceHub
        val queryCriteria: QueryCriteria = QueryCriteria.LinearStateQueryCriteria(
            null,
            listOf(UUID.fromString(linearId))
        )
        val taskStatePage: Vault.Page<ToDoState> = sb.vaultService.queryBy(ToDoState::class.java, queryCriteria)
        val states = taskStatePage.states
        val sar = states[0]
        val currentToDoState = sar.state.data

        val doneToDoState = currentToDoState.markAsDone()

        println("ToDo ${doneToDoState.taskDescription} is now done! 🤘🏻")
        
        val notary = sb.networkMapCache.notaryIdentities[0]

        val myKey = ourIdentity.owningKey
        val tb: TransactionBuilder = TransactionBuilder(notary).addInputState(sar)
            .addOutputState(doneToDoState)
            .addCommand(DummyToDoCommand(), myKey)

        val stx = serviceHub.signInitialTransaction(tb)
        subFlow(FinalityFlow(stx, emptySet<FlowSession>()))

        return null;
    }
}

