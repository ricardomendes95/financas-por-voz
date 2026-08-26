package br.com.financas.core.database

import androidx.room.TypeConverter
import br.com.financas.core.model.AccountKind
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.TransactionType

/** Enums do :core:model persistidos como texto — legível direto no `adb shell sqlite3`. */
class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromTransactionTypeOrNull(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionTypeOrNull(value: String?): TransactionType? = value?.let(TransactionType::valueOf)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let(PaymentMethod::valueOf)

    @TypeConverter
    fun fromEntrySource(value: EntrySource): String = value.name

    @TypeConverter
    fun toEntrySource(value: String): EntrySource = EntrySource.valueOf(value)

    @TypeConverter
    fun fromAccountKind(value: AccountKind): String = value.name

    @TypeConverter
    fun toAccountKind(value: String): AccountKind = AccountKind.valueOf(value)
}
