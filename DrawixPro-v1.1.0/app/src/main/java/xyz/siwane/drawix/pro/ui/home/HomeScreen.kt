package xyz.siwane.drawix.pro.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.ui.DrawViewModel
import xyz.siwane.drawix.pro.ui.components.*
import xyz.siwane.drawix.pro.ui.layout.LocalLayoutSpec
import xyz.siwane.drawix.pro.ui.theme.LocalAccents

@Composable fun HomeScreen(vm: DrawViewModel, onToggle: () -> Unit) {
    val running by vm.running.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun open(uri: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
            .onFailure { vm.showMessage(R.string.ui_no_handler) }
    }
    LazyColumn(contentPadding = PaddingValues(start = LocalLayoutSpec.current.padding.dp, end = LocalLayoutSpec.current.padding.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(CardCorners.spacing)) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusPill(text(R.string.app_name), true)
                        Text(text(R.string.ui_draw_headline), style = MaterialTheme.typography.displaySmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                    }
                    DrawixBloom(R.drawable.ic_brush_2, Modifier.size(72.dp))
                }
                Text(text(R.string.ui_home_subtitle), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            GlassSurface(shape = CardCorners.single, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary) {
                        DrawIcon(if (running) R.drawable.ic_brush_2 else R.drawable.ic_stop, modifier = Modifier.padding(10.dp).size(24.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text(R.string.service_title), style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(text(if (running) R.string.service_running else R.string.service_stopped), style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(running, { onToggle() })
                }
            }
        }
        item { SectionTitle(text(R.string.guide_title)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(CardCorners.spacing)) {
                Row(horizontalArrangement = Arrangement.spacedBy(CardCorners.spacing), modifier = Modifier.height(IntrinsicSize.Min)) {
                    ToolFeature(text(R.string.guide_laser), R.drawable.ic_pointer, LocalAccents.current.sky,
                        Modifier.weight(1f).fillMaxHeight(), CardCorners.topRow(0, 2))
                    ToolFeature(text(R.string.guide_censor), R.drawable.ic_grid_eraser, LocalAccents.current.peach,
                        Modifier.weight(1f).fillMaxHeight(), CardCorners.topRow(1, 2))
                }
                InfoCard(text(R.string.tip_title), text(R.string.tip_desc), R.drawable.ic_lamp_on, shape = CardCorners.trailing)
            }
        }
        item { SectionTitle(text(R.string.faq_title)) }
        val questions = listOf(R.string.faq_q1 to R.string.faq_a1, R.string.faq_q2 to R.string.faq_a2, R.string.faq_q3 to R.string.faq_a3)
        item {
            var expanded by rememberSaveable { mutableStateOf(-1) }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CardCorners.spacing)) {
                questions.forEachIndexed { index, (question, answer) ->
                    val open = expanded == index
                    GlassSurface(onClick = { expanded = if (open) -1 else index },
                        shape = CardCorners.grouped(index, questions.size),
                        color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text(question), modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleSmall)
                                Text(if (open) "−" else "+", color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleLarge)
                            }
                            AnimatedVisibility(open) {
                                Text(text(answer), modifier = Modifier.padding(top = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle(text(R.string.about_section_title)) }
        listOf(
            Triple(R.string.about_privacy, R.drawable.ic_shield_tick, "https://drawix.siwane.xyz/privacy"),
            Triple(R.string.about_terms, R.drawable.ic_document_text, "https://drawix.siwane.xyz/terms"),
            Triple(R.string.about_dev_email, R.drawable.ic_sms, "mailto:jamal@elhizazi.me"),
            Triple(R.string.about_dev_web, R.drawable.ic_global, "https://jamal.elhizazi.me/"),
            Triple(R.string.about_support, R.drawable.ic_lifebuoy, "https://www.siwane.xyz/")
        ).forEachIndexed { index, (label, icon, uri) -> item { ActionRow(text(label), icon = icon, tone = LocalAccents.current.at(index), shape = CardCorners.grouped(index, 5)) { open(uri) } } }
        item { SectionTitle(text(R.string.support_section_title)) }
        item { DonationButtons(onPayPal = { open("https://paypal.me/Elhizazi") },
            onCoffee = { open("https://buymeacoffee.com/elhizazi1") }) }

    }
}

@Composable private fun ToolFeature(title: String, icon: Int, tone: androidx.compose.ui.graphics.Color, modifier: Modifier, shape: RoundedCornerShape) {
    GlassSurface(modifier = modifier, shape = shape, color = tone) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DrawIcon(icon, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}
