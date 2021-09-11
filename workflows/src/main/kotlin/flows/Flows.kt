package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.DummyToDoCommand
import com.template.states.ToDoState
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.jetbrains.annotations.NotNull
import java.util.*


// flow start CreateToDoFlow task: "Get some cheese"
// flow start AssignToDoInitiator linearId: 3d3d3a7b-35bf-4b1d-83d7-9f10a9c98657 , assignedTo: PartyA
// ******************
// * Initiator flow *
// ******************
@StartableByRPC
class CreateToDoFlow(private val taskDescription: String) : FlowLogic<Void?>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Void? {
        val serviceHub = serviceHub
        val me = ourIdentity
        val ts = ToDoState(me, me, taskDescription)
        println("Linear ID of state is " + ts.linearId)
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        var tb = TransactionBuilder(notary)
        tb = tb.addCommand(DummyToDoCommand(), me.owningKey) // at least one is required
        tb = tb.addOutputState(ts)
        val stx = serviceHub.signInitialTransaction(tb)
        subFlow(FinalityFlow(stx, emptySet<FlowSession>()))
        println("oi!!")
        return null
    }
}

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