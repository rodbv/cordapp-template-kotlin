package com.template.states

import com.learncorda.tododist.contracts.ToDoContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party


// *************
// * ToDoState *
// *************
@BelongsToContract(ToDoContract::class)
data class ToDoState(
    val assignedBy: Party,
    val assignedTo: Party,
    val taskDescription: String,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : ContractState, LinearState {
    fun assign(assignedTo: Party): ToDoState {
        return ToDoState(
            assignedBy,
            assignedTo,
            taskDescription,
            linearId
        )
    }

    override val participants: List<AbstractParty>
        get() = listOf(assignedBy, assignedTo)
}
