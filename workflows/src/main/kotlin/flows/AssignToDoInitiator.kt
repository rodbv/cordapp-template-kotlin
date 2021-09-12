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
class AssignToDoInitiator(private val linearId: String, private val assignedTo: String): FlowLogic<Void?>() {
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
        println("ToDoState => ${currentToDoState.taskDescription}")

        val parties = sb.identityService.partiesFromName(assignedTo, true)
        val assignedToParty = parties.iterator().next()

        val newToDoState = currentToDoState.assign(assignedToParty)
        val notary = sb.networkMapCache.notaryIdentities[0]

        val myKey = ourIdentity.owningKey
        val tb: TransactionBuilder = TransactionBuilder(notary).addInputState(sar)
            .addOutputState(newToDoState)
            .addCommand(
                DummyToDoCommand(), myKey,
                assignedToParty.owningKey
            )

        val assignedToSession = initiateFlow(assignedToParty)
        val ptx = serviceHub.signInitialTransaction(tb)
        val stx = subFlow(
            CollectSignaturesFlow(
                ptx,
                setOf(assignedToSession)
            )
        )
        subFlow(FinalityFlow(stx, listOf(assignedToSession)))

        return null;
    }
}

