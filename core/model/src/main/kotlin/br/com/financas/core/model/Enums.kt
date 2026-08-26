package br.com.financas.core.model

/** Saída ou entrada de dinheiro. */
enum class TransactionType { EXPENSE, INCOME }

/** Forma de pagamento, quando informada. */
enum class PaymentMethod { PIX, CREDIT, DEBIT, CASH, BOLETO, TRANSFER }

/** De onde veio o lançamento — usado para auditoria e para decidir se precisa de revisão. */
enum class EntrySource { VOICE, MANUAL, NOTIFICATION, WIDGET, IMPORT, RECURRING }

/** Tipo de conta/carteira. */
enum class AccountKind { CHECKING, CREDIT_CARD, CASH, SAVINGS }
