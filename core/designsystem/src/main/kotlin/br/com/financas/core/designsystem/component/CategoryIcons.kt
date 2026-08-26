package br.com.financas.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Traduz o nome de ícone salvo em `CategoryEntity.icon` (§3.3 da spec) para o
 * `ImageVector` do Material — mesmo vocabulário usado no `CategorySeeder`.
 */
object CategoryIcons {

    private val BY_NAME: Map<String, ImageVector> = mapOf(
        "restaurant" to Icons.Filled.Restaurant,
        "directions_car" to Icons.Filled.DirectionsCar,
        "home" to Icons.Filled.Home,
        "favorite" to Icons.Filled.Favorite,
        "movie" to Icons.Filled.Movie,
        "school" to Icons.Filled.School,
        "shopping_bag" to Icons.Filled.ShoppingBag,
        "build" to Icons.Filled.Build,
        "pets" to Icons.Filled.Pets,
        "receipt_long" to Icons.AutoMirrored.Filled.ReceiptLong,
        "more_horiz" to Icons.Filled.MoreHoriz,
        "payments" to Icons.Filled.Payments,
        "work" to Icons.Filled.Work,
        "undo" to Icons.AutoMirrored.Filled.Undo,
        "add_circle" to Icons.Filled.AddCircle
    )

    fun resolve(name: String): ImageVector = BY_NAME[name] ?: Icons.Filled.MoreHoriz
}
