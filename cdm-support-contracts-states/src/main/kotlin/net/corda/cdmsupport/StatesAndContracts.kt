package net.corda.cdmsupport

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


import org.isda.cdm.Event
import org.isda.cdm.ExecutionState
import org.isda.cdm.PartyRoleEnum
import java.math.BigDecimal
import java.time.LocalDate

class CDMEvent : Contract {


    companion object {
        val ID = "net.corda.cdmsupport.CDMEvent"
    }

    interface Commands : CommandData {
        class Affirmation() : Commands
        class Confirmation() : Commands
        class Portfolio() : Commands
        class Execution(val outputIndex: Int) : Commands
        class Transfer(val outputIndex: Int) : Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commandsOfType(Commands::class.java).single()

        if(command.value is Commands.Execution){
            val outputState = (tx.outputStates[(command.value as Commands.Execution).outputIndex] as net.corda.cdmsupport.states.ExecutionState)
            requireThat {
                "Three Parties in the Execution Event" using (outputState.execution().party.size == 3)
                "One Client " using (getNumberOfPartiesPlayingARole(PartyRoleEnum.CLIENT,outputState) ==1)
                "One Executing Entity" using (getNumberOfPartiesPlayingARole(PartyRoleEnum.EXECUTING_ENTITY,outputState) == 1)
                "One Counterparty " using (getNumberOfPartiesPlayingARole(PartyRoleEnum.COUNTERPARTY,outputState) ==1)
                "Executing PArty is on the same side as the Client" using ( checkClientExecutingPartySide(outputState))
            }
        }


    }

  private fun getPartyRoles(globalReference : String, outputState: net.corda.cdmsupport.states.ExecutionState) : List<PartyRoleEnum> {
    return outputState.execution().partyRole.filter{it.partyReference.globalReference == globalReference}.map{it -> it.role}.toList()
  }

    private fun getNumberOfPartiesPlayingARole(role : PartyRoleEnum, outputState: net.corda.cdmsupport.states.ExecutionState) : Int {
        return outputState.execution().partyRole.filter { it.role == role }.size
    }

    private fun checkClientExecutingPartySide (outputState: net.corda.cdmsupport.states.ExecutionState) : Boolean {
        val clientReference = outputState.execution().partyRole.single { it.role == PartyRoleEnum.CLIENT }.partyReference.globalReference
        val executingEntityReference = outputState.execution().partyRole.single { it.role == PartyRoleEnum.EXECUTING_ENTITY }.partyReference.globalReference

        val clientRoles = getPartyRoles(clientReference, outputState).filter{ it != PartyRoleEnum.CLIENT }
        val execEntityRole = getPartyRoles(executingEntityReference, outputState).filter{it != PartyRoleEnum.EXECUTING_ENTITY}

        return clientRoles.equals(executingEntityReference)

    }
}











