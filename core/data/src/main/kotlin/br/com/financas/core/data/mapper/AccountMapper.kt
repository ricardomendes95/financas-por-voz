package br.com.financas.core.data.mapper

import br.com.financas.core.database.entity.AccountEntity
import br.com.financas.core.model.Account

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    kind = kind,
    openingBalanceCents = openingBalanceCents,
    closingDay = closingDay,
    dueDay = dueDay,
    colorArgb = colorArgb
)
