package it.scado.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.scado.data.Product
import it.scado.ui.FridgeViewModel
import it.scado.ui.components.ProductForm
import it.scado.ui.theme.urgencyColor
import it.scado.util.Dates
import it.scado.util.Emoji
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class Section(val title: String, val items: List<Product>)

private fun sections(products: List<Product>): List<Section> {
    val groups = products.groupBy {
        val d = Dates.daysLeft(it.expiryEpochDay)
        when {
            d < 0 -> 0
            d <= 1 -> 1
            d <= 7 -> 2
            else -> 3
        }
    }
    val titles = listOf("Scaduti", "Oggi e domani", "Entro una settimana", "Più avanti")
    return (0..3).mapNotNull { i -> groups[i]?.let { Section(titles[i], it) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: FridgeViewModel, onShopping: () -> Unit, onSettings: () -> Unit) {
    val products by vm.products.collectAsStateWithLifecycle()
    val household by vm.household.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    NotificationPermissionRequest()

    fun removeWithUndo(p: Product) {
        vm.deleteProduct(p)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(
                "${p.name} tolto dal frigo", actionLabel = "Annulla", duration = SnackbarDuration.Short
            )
            if (r == SnackbarResult.ActionPerformed) vm.restoreProduct(p)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(household?.name ?: "Il nostro frigo", fontWeight = FontWeight.SemiBold)
                        products?.let {
                            Text(
                                when (it.size) {
                                    0 -> "vuoto"
                                    1 -> "1 prodotto"
                                    else -> "${it.size} prodotti"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Impostazioni") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onShopping,
                icon = { Icon(Icons.Default.ShoppingCart, null) },
                text = { Text("Ho fatto la spesa") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = products
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            list.isEmpty() -> EmptyFridge(Modifier.padding(padding), onShopping)
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sections(list).forEach { section ->
                    item(key = "h_${section.title}") {
                        Text(
                            "${section.title} · ${section.items.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp).animateItem(),
                        )
                    }
                    items(section.items, key = { it.id }) { p ->
                        SwipeableProduct(
                            product = p,
                            onClick = { editingId = p.id },
                            onRemove = { removeWithUndo(p) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    val editing = editingId?.let { id -> products?.firstOrNull { it.id == id } }
    if (editing != null) {
        EditProductSheet(
            product = editing,
            defaultReminders = vm.settings.collectAsStateWithLifecycle().value.defaultReminderDays,
            onDismiss = { editingId = null },
            onSave = { name, date, reminders ->
                vm.updateProduct(editing, name, date, reminders)
                editingId = null
            },
            onRemove = {
                editingId = null
                removeWithUndo(editing)
            },
        )
    }
}

@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun EmptyFridge(modifier: Modifier, onShopping: () -> Unit) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🧊", fontSize = 80.sp)
        Spacer(Modifier.height(12.dp))
        Text("Il frigo è vuoto", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Quando torni dalla spesa tocca «Ho fatto la spesa»: ti chiederò un prodotto alla volta.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onShopping) { Text("Inizia") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableProduct(
    product: Product,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remove by rememberUpdatedState(onRemove)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) {
                remove()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val alignment =
                if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart
                else Alignment.CenterEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Finito", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
    ) {
        ProductCard(product, onClick)
    }
}

@Composable
private fun ProductCard(p: Product, onClick: () -> Unit) {
    val daysLeft = Dates.daysLeft(p.expiryEpochDay)
    val color = urgencyColor(daysLeft, isSystemInDarkTheme())
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text(Emoji.forName(p.name), fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    Dates.format(p.expiryEpochDay) + (p.addedByName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(CircleShape).background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    Dates.shortRelative(p.expiryEpochDay),
                    color = color, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProductSheet(
    product: Product,
    defaultReminders: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, Set<Int>) -> Unit,
    onRemove: () -> Unit,
) {
    var name by rememberSaveable(product.id) { mutableStateOf(product.name) }
    var epochDay by rememberSaveable(product.id) { mutableStateOf(product.expiryEpochDay) }
    var reminders by rememberSaveable(product.id) { mutableStateOf(product.reminderDays.toList()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Modifica prodotto", style = MaterialTheme.typography.titleLarge)
            ProductForm(
                name = name, onNameChange = { name = it },
                date = LocalDate.ofEpochDay(epochDay), onDateChange = { epochDay = it.toEpochDay() },
                reminders = reminders.toSet(), onRemindersChange = { reminders = it.sorted() },
                showDatePicker = showPicker, onShowDatePickerChange = { showPicker = it },
            )
            if (reminders.isEmpty() && defaultReminders.isNotEmpty()) {
                Text(
                    "Nessun promemoria: non riceverai notifiche per questo prodotto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) { Text("Finito / buttato") }
                Button(
                    onClick = { onSave(name.trim(), LocalDate.ofEpochDay(epochDay), reminders.toSet()) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("Salva") }
            }
        }
    }
}
