package br.com.financas.app.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Dashboard

@Serializable
data object Transactions

@Serializable
data class AddTransaction(val type: String? = null)

@Serializable
data class EditTransaction(val transactionId: String)

@Serializable
data object Settings

@Serializable
data object Reports

@Serializable
data object Budgets

@Serializable
data object MonthClosing

@Serializable
data object BankAllowlist

@Serializable
data object ImportStatement
