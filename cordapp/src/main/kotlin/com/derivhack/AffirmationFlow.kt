package com.derivhack

import co.paralleluniverse.fibers.Suspendable
import net.corda.cdmsupport.eventparsing.parseEventFromJson
import net.corda.cdmsupport.transactionbuilding.CdmTransactionBuilder
import net.corda.cdmsupport.vaultquerying.DefaultCdmVaultQuery
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class AffirmationFlow(val executionRef: String) : FlowLogic<SignedTransaction>() {

    //TODO
    /**
     *  You're expected to generate relevant CDM objects and link them to associated allocated
     *  trades created with UC2 as well as validate them against CDM data rules by
     *  creating validations similar to those for the previous use cases
     *
     *  For building your affirmation CDM Object see net.corda.cdmsupport.builders
     *  package in the project
     *
     *  Add an Observery mode to the transaction
     */


    @Suspendable
    override fun call(): SignedTransaction {
        val event = parseEventFromJson("{allocationJson}")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cdmTransactionBuilder = CdmTransactionBuilder(notary, event, DefaultCdmVaultQuery(serviceHub))
        cdmTransactionBuilder.verify(serviceHub)
        val signedByMe = serviceHub.signInitialTransaction(cdmTransactionBuilder)

        val counterPartySessions = cdmTransactionBuilder.getPartiesToSign().minus(ourIdentity).map { initiateFlow(it) }


        val fullySignedTx = subFlow(CollectSignaturesFlow(signedByMe, counterPartySessions, CollectSignaturesFlow.tracker()))
        val finalityTx = subFlow(FinalityFlow(fullySignedTx, counterPartySessions))


        return finalityTx

    }
}

@InitiatedBy(AllocationFlow::class)
class AffirmationFlowResponder(val flowSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        }

        val signedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = signedId.id))
    }
}

