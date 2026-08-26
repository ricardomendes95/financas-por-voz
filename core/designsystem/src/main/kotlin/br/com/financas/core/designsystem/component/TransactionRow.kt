package br.com.financas.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.financas.core.common.RelativeDateFormatter
import br.com.financas.core.designsystem.theme.FinanceTheme
import br.com.financas.core.model.TransactionListItem

/** Linha de lançamento reutilizada pelo Dashboard e pela lista de Lançamentos — toque abre a edição. */
@Composable
fun TransactionRow(item: TransactionListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = CategoryIcons.resolve(item.categoryIcon),
                    contentDescription = item.categoryName
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${item.categoryName} · ${RelativeDateFormatter.format(item.occurredAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MoneyText(
            cents = item.amountCents,
            isExpense = item.isExpense,
            color = if (item.isExpense) FinanceTheme.colors.expense else FinanceTheme.colors.income,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
