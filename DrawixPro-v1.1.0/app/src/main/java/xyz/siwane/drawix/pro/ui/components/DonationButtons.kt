package xyz.siwane.drawix.pro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.siwane.drawix.pro.R

/** Tonal support cards retain the existing donation destinations. */
@Composable fun DonationButtons(onPayPal: () -> Unit, onCoffee: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(CardCorners.spacing)) {
        SupportButton("PayPal", R.drawable.ic_support_heart, Modifier.weight(1f).fillMaxHeight(), false, 0, onPayPal)
        SupportButton("Buy Me a Coffee", R.drawable.ic_coffee, Modifier.weight(1f).fillMaxHeight(), true, 1, onCoffee)
    }
}
@Composable private fun SupportButton(label: String, icon: Int, modifier: Modifier, warm: Boolean, index: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    GlassSurface(onClick = onClick, modifier = modifier, shape = CardCorners.horizontal(index, 2),
        color = if (warm) colors.tertiaryContainer else colors.secondaryContainer,
        contentColor = if (warm) colors.onTertiaryContainer else colors.onSecondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            DrawIcon(icon, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
