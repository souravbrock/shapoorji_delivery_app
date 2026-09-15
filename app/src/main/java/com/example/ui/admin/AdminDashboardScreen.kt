package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import com.example.BuildConfig
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.NotificationLog
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.notification.SmtpResult
import com.example.data.notification.TelegramDispatchReport
import com.example.data.repository.GroceryRepository
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BlueReceived
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.GreenDelivered
import com.example.ui.theme.PurpleTransit
import com.example.ui.viewmodel.GroceryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: GroceryViewModel,
    onNavigateBackToCustomer: () -> Unit,
    onViewOrderInvoice: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Orders", "Daily Prices", "Products", "Notifications", "System & Updates")

    val orders by viewModel.allOrders.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val logs by viewModel.notificationLogs.collectAsState()

    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Store Admin Console", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Text(
                            text = "Admin: souravbrock@gmail.com • +91-8442980101",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkForAppUpdates() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check for Updates",
                            tint = EmeraldGreenDark
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateBackToCustomer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Customer View", fontSize = 12.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 2) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = EmeraldGreenPrimary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Metrics Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminMetricCard(
                    title = "Orders",
                    value = "${orders.size}",
                    sub = "${orders.count { it.status != OrderStatus.DELIVERED }} active",
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Catalog",
                    value = "${products.size}",
                    sub = "Products",
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Alerts Sent",
                    value = "${logs.size}",
                    sub = "Email + Bot",
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> AdminOrdersTab(
                    orders = orders,
                    onAdvanceStatus = { order -> viewModel.advanceOrderStatus(order) },
                    onViewInvoice = onViewOrderInvoice
                )
                1 -> AdminDailyPricesTab(
                    products = products,
                    onUpdatePrice = { productId, newPrice ->
                        viewModel.updateProductDailyPrice(productId, newPrice)
                    }
                )
                2 -> AdminProductsCatalogTab(
                    products = products,
                    onEditProduct = { editingProduct = it },
                    onDeleteProduct = { viewModel.deleteProduct(it) },
                    onAddClick = { showAddProductDialog = true },
                    onResetOfficial = { viewModel.resetToOfficialCatalog() }
                )
                3 -> AdminNotificationLogsTab(
                    logs = logs,
                    viewModel = viewModel,
                    onTestAlert = { success ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "Telegram Alert Dispatched Successfully!" else "Test Alert Logged in Database Audit Trail!"
                            )
                        }
                    }
                )
                4 -> AdminSystemUpdatesTab(
                    viewModel = viewModel
                )
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { name, category, unit, price, mrp, stock, desc, url, isDaily, allowFrac, fracStep ->
                viewModel.addProduct(
                    name = name,
                    category = category,
                    unit = unit,
                    price = price,
                    mrp = mrp,
                    stockQty = stock,
                    description = desc,
                    imageUrl = url,
                    isDailyEssential = isDaily,
                    allowFractional = allowFrac,
                    fractionStepGrams = fracStep
                )
                showAddProductDialog = false
            }
        )
    }

    editingProduct?.let { product ->
        EditProductDialog(
            product = product,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                viewModel.updateProduct(updated)
                editingProduct = null
            }
        )
    }
}

@Composable
fun AdminMetricCard(title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, fontSize = 10.sp, color = EmeraldGreenPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

// -------------------------------------------------------------
// TAB 1: ORDERS & 4-STAGE STATUS ADVANCEMENT
// -------------------------------------------------------------
@Composable
fun AdminOrdersTab(
    orders: List<Order>,
    onAdvanceStatus: (Order) -> Unit,
    onViewInvoice: (Order) -> Unit
) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No customer orders received yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                AdminOrderCard(
                    order = order,
                    onAdvanceStatus = { onAdvanceStatus(order) },
                    onViewInvoice = { onViewInvoice(order) }
                )
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onAdvanceStatus: () -> Unit,
    onViewInvoice: () -> Unit
) {
    val items = remember(order.itemsJson) {
        GroceryRepository.parseOrderItems(order.itemsJson)
    }
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(order.createdAt))

    val statusColor = when (order.status) {
        OrderStatus.ORDER_RECEIVED -> BlueReceived
        OrderStatus.ORDER_PACKED -> AmberAccent
        OrderStatus.OUT_FOR_DELIVERY -> PurpleTransit
        OrderStatus.DELIVERED -> GreenDelivered
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.orderNumber}",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.status.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer and Tower
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.customerName} (${order.customerPhone})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "📍 ${order.towerName}, ${order.flatNumber}",
                        fontSize = 11.sp,
                        color = EmeraldGreenDark,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "₹${order.grandTotal.toInt()}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = EmeraldGreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items Preview
            Text(
                text = "${order.itemCount} items: " + items.joinToString { "${it.quantity}x ${it.productName}" },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewInvoice,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Invoice",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invoice", fontSize = 11.sp)
                }

                val next = order.status.nextStatus()
                if (next != null) {
                    Button(
                        onClick = onAdvanceStatus,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Advance: ${next.label}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Text(
                        text = "✓ Order Fully Delivered",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GreenDelivered
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: DAILY PRICES UPDATER
// -------------------------------------------------------------
@Composable
fun AdminDailyPricesTab(
    products: List<Product>,
    onUpdatePrice: (Long, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CurrencyRupee, contentDescription = "Price", tint = EmeraldGreenDark)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Daily Price Manager",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EmeraldGreenDark
                    )
                    Text(
                        text = "Update daily morning mandi prices. Changes reflect instantly to all customers.",
                        fontSize = 11.sp,
                        color = EmeraldGreenDark.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { it.id }) { product ->
                DailyPriceItemRow(product = product, onSavePrice = { newPrice ->
                    onUpdatePrice(product.id, newPrice)
                })
            }
        }
    }
}

@Composable
fun DailyPriceItemRow(
    product: Product,
    onSavePrice: (Double) -> Unit
) {
    var priceText by remember(product.price) { mutableStateOf("${product.price.toInt()}") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${product.category} • ${product.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("MRP: ₹${product.mrp.toInt()} | Current: ₹${product.price.toInt()}", fontSize = 11.sp, color = EmeraldGreenPrimary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("₹", fontSize = 13.sp) },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val parsed = priceText.toDoubleOrNull() ?: product.price
                        onSavePrice(parsed)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                ) {
                    Text("Save", fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: PRODUCTS CRUD (ADD, EDIT, DELETE)
// -------------------------------------------------------------
@Composable
fun AdminProductsCatalogTab(
    products: List<Product>,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAddClick: () -> Unit,
    onResetOfficial: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Official Store Catalog (${products.size} Items)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldGreenDark
                        )
                        Text(
                            text = "37 produce items (Vegetables & Fruits) with multilingual Bengali & Hindi titles and live rates.",
                            fontSize = 11.sp,
                            color = EmeraldGreenDark.copy(alpha = 0.85f)
                        )
                    }

                    OutlinedButton(
                        onClick = onResetOfficial,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sync Official", fontSize = 11.sp)
                    }
                }
            }
        }

        items(products, key = { it.id }) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = product.imageUrl.ifBlank { "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400" },
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${product.category} • ${product.unit} • ₹${product.price.toInt()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (product.description.isNotBlank()) {
                            Text(
                                text = product.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val isLowStock = product.stockQty <= 20
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isLowStock) CoralRed.copy(alpha = 0.15f) else EmeraldContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (product.stockQty <= 0) "Out of Stock" else "Stock: ${product.stockQty} ${product.unit}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLowStock) CoralRed else EmeraldGreenDark
                            )
                        }

                        if (product.allowFractional && product.isUnitDivisible()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Fractional: from ${if (product.fractionStepGrams <= 100) "100g" else "250g"}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        } else if (!product.isUnitDivisible()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Fixed Per-Piece",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEditProduct(product) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Product",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { onDeleteProduct(product) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Product",
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: NOTIFICATIONS & DISPATCH ENGINE SETTINGS
// -------------------------------------------------------------
@Composable
fun AdminNotificationLogsTab(
    logs: List<NotificationLog>,
    viewModel: GroceryViewModel,
    onTestAlert: ((Boolean) -> Unit)? = null
) {
    val vmBotToken by viewModel.telegramBotToken.collectAsState()
    val vmAdminChats by viewModel.adminChatIds.collectAsState()
    val vmManagerChats by viewModel.managerChatIds.collectAsState()
    val vmStaffChats by viewModel.staffChatIds.collectAsState()

    val vmSmtpHost by viewModel.smtpHost.collectAsState()
    val vmSmtpPort by viewModel.smtpPort.collectAsState()
    val vmSmtpUser by viewModel.smtpUsername.collectAsState()
    val vmSmtpPass by viewModel.smtpPassword.collectAsState()
    val vmSenderEmail by viewModel.senderEmail.collectAsState()
    val vmAdminEmail by viewModel.adminEmail.collectAsState()

    val isTestingTelegram by viewModel.isTestingTelegram.collectAsState()
    val isTestingEmail by viewModel.isTestingEmail.collectAsState()

    var botTokenInput by remember(vmBotToken) { mutableStateOf(vmBotToken) }
    var adminChatInput by remember(vmAdminChats) { mutableStateOf(vmAdminChats) }
    var managerChatInput by remember(vmManagerChats) { mutableStateOf(vmManagerChats) }
    var staffChatInput by remember(vmStaffChats) { mutableStateOf(vmStaffChats) }

    var smtpHostInput by remember(vmSmtpHost) { mutableStateOf(vmSmtpHost) }
    var smtpPortInput by remember(vmSmtpPort) { mutableStateOf(vmSmtpPort) }
    var smtpUserInput by remember(vmSmtpUser) { mutableStateOf(vmSmtpUser) }
    var smtpPassInput by remember(vmSmtpPass) { mutableStateOf(vmSmtpPass) }
    var senderEmailInput by remember(vmSenderEmail) { mutableStateOf(vmSenderEmail) }
    var adminEmailInput by remember(vmAdminEmail) { mutableStateOf(vmAdminEmail) }

    var telegramReportDialog by remember { mutableStateOf<TelegramDispatchReport?>(null) }
    var emailResultDialog by remember { mutableStateOf<SmtpResult?>(null) }
    var selectedLogForDetail by remember { mutableStateOf<NotificationLog?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Architecture Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Alerts", tint = EmeraldGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Multi-Channel Notification Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EmeraldGreenDark
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Email: Real SMTP over SSL (spdelivery.reddevils.co.in:465)\n" +
                                "• Sender: order@spdelivery.reddevils.co.in\n" +
                                "• Recipients: Customer + souravbrock@gmail.com\n" +
                                "• Triggers: Order Placed + All status changes (Packed, Out for Delivery, Delivered)\n" +
                                "• Telegram: Direct Bot API alerts to Admin, Store Managers & Staff with full order itemization",
                        fontSize = 11.sp,
                        color = EmeraldGreenDark.copy(alpha = 0.9f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Telegram Bot Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Telegram", tint = Color(0xFF0288D1))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Telegram Bot Channel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("@ShapoorjiOrdersBot", fontSize = 11.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = { botTokenInput = it },
                        label = { Text("Bot Token") },
                        placeholder = { Text("8906839330:AAEO...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adminChatInput,
                        onValueChange = { adminChatInput = it },
                        label = { Text("Admin Chat IDs (comma separated)") },
                        placeholder = { Text("167694312, 7127777789") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = managerChatInput,
                        onValueChange = { managerChatInput = it },
                        label = { Text("Store Manager Chat IDs") },
                        placeholder = { Text("8924193494, 9083900751") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = staffChatInput,
                        onValueChange = { staffChatInput = it },
                        label = { Text("Store Staff Chat IDs") },
                        placeholder = { Text("58088380") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.sendTestTelegramAlert { report ->
                                telegramReportDialog = report
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isTestingTelegram,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                    ) {
                        if (isTestingTelegram) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Broadcasting to Telegram...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Telegram Dispatch (Broadcast to All IDs)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Email & Outgoing SMTP Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = "SMTP", tint = EmeraldGreenPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SMTP Email Server Channel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("Port 465 (SSL)", fontSize = 11.sp, color = EmeraldGreenDark, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = smtpHostInput,
                            onValueChange = { smtpHostInput = it },
                            label = { Text("Outgoing SMTP Host") },
                            placeholder = { Text("spdelivery.reddevils.co.in") },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = smtpPortInput,
                            onValueChange = { smtpPortInput = it },
                            label = { Text("Port") },
                            placeholder = { Text("465") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = smtpUserInput,
                        onValueChange = { smtpUserInput = it },
                        label = { Text("SMTP Username / Auth Email") },
                        placeholder = { Text("order@spdelivery.reddevils.co.in") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = smtpPassInput,
                        onValueChange = { smtpPassInput = it },
                        label = { Text("SMTP Password") },
                        placeholder = { Text("Enter password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adminEmailInput,
                        onValueChange = { adminEmailInput = it },
                        label = { Text("Admin Notification Email (souravbrock)") },
                        placeholder = { Text("souravbrock@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.sendTestEmailAlert { result ->
                                emailResultDialog = result
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isTestingEmail,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        if (isTestingEmail) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting & Authenticating via SSL...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test SMTP Email Dispatch (To souravbrock@gmail.com)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Save All Configurations Button
        item {
            Button(
                onClick = {
                    viewModel.updateAllNotificationSettings(
                        botToken = botTokenInput,
                        adminChats = adminChatInput,
                        managerChats = managerChatInput,
                        staffChats = staffChatInput,
                        host = smtpHostInput,
                        port = smtpPortInput,
                        username = smtpUserInput,
                        pass = smtpPassInput,
                        sender = senderEmailInput,
                        adminMail = adminEmailInput
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save All Notification & Server Settings", fontWeight = FontWeight.Bold)
            }
        }

        // Live Log Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification Audit Trail (${logs.size} recorded):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Tap any card to view content",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(logs) { log ->
            NotificationLogCard(
                log = log,
                onClick = { selectedLogForDetail = log }
            )
        }
    }

    // Telegram Dispatch Report Dialog
    telegramReportDialog?.let { report ->
        AlertDialog(
            onDismissRequest = { telegramReportDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (report.successCount > 0) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (report.successCount > 0) EmeraldGreenPrimary else CoralRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Telegram Broadcast Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Delivered to ${report.successCount} of ${report.totalCount} recipients.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (report.successCount > 0) EmeraldGreenDark else CoralRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    report.details.forEach { detail ->
                        Text(
                            text = detail,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = if (detail.startsWith("✓")) EmeraldGreenDark else CoralRed
                        )
                    }
                    if (report.details.any { it.contains("tap /start") }) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ℹ️ Note: Telegram requires users to tap 'Start' on the bot (@ShapoorjiOrdersBot) before receiving bot messages.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { telegramReportDialog = null }) {
                    Text("Done")
                }
            }
        )
    }

    // Email SMTP Test Result Dialog
    emailResultDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { emailResultDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (result.success) EmeraldGreenPrimary else CoralRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (result.success) "Email Dispatched" else "SMTP Notice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = result.message,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (result.success) EmeraldGreenDark else CoralRed
                    )
                    if (result.serverLog.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Server Communication Trace:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = result.serverLog,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { emailResultDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Log Detail Inspector Dialog
    selectedLogForDetail?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            title = {
                Text(log.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Column {
                    Text("Type: ${log.type.name} | Status: ${log.status}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Recipient: ${log.recipient}", fontSize = 11.sp, color = EmeraldGreenDark)
                    Text("Sender: ${log.sender}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = log.content,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp),
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedLogForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun NotificationLogCard(
    log: NotificationLog,
    onClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(log.timestamp))
    val isDelivered = log.status.startsWith("DELIVERED")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (log.type == NotificationType.EMAIL) EmeraldContainer else Color(0xFFE1F5FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.type == NotificationType.EMAIL) Icons.Default.Email else Icons.Default.Send,
                            contentDescription = log.type.name,
                            tint = if (log.type == NotificationType.EMAIL) EmeraldGreenPrimary else Color(0xFF0288D1),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.type.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDelivered) EmeraldContainer else Color(0xFFFFEBEE))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDelivered) "DELIVERED" else "NOTICE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDelivered) EmeraldGreenDark else CoralRed
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = log.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "To: ${log.recipient}",
                fontSize = 11.sp,
                color = EmeraldGreenDark,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "From: ${log.sender}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// ADD PRODUCT DIALOG
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: String,
        unit: String,
        price: Double,
        mrp: Double,
        stock: Int,
        description: String,
        imageUrl: String,
        isDaily: Boolean,
        allowFractional: Boolean,
        fractionStepGrams: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vegetables") }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf("1 kg") }
    var priceStr by remember { mutableStateOf("40") }
    var mrpStr by remember { mutableStateOf("50") }
    var stockStr by remember { mutableStateOf("50") }
    var description by remember { mutableStateOf("Fresh daily stock for Shapoorji residents.") }
    var imageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1542838132-92c53300491e?w=400") }
    var isDailyEssential by remember { mutableStateOf(true) }

    val isUnitDivisible = remember(unit) {
        val lower = unit.lowercase()
        val isPiece = lower.contains("pc") || lower.contains("piece") || lower.contains("bunch") || lower.contains("packet") || lower.contains("bottle")
        (lower.contains("kg") || lower.contains("gm") || lower.contains("gram") || lower.contains("l") || lower.contains("liter") || lower.contains("litre") || lower.contains("ltr") || lower.contains("ml")) && !isPiece
    }
    var allowFractional by remember { mutableStateOf(true) }
    var fractionStepGrams by remember { mutableStateOf(250) }

    val categories = listOf("Vegetables", "Fruits", "Dairy & Breakfast", "Staples & Atta")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Grocery Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.lowercase().contains("garlic") || it.lowercase().contains("ginger") || it.lowercase().contains("chilli")) {
                            fractionStepGrams = 100
                        }
                    },
                    label = { Text("Product Name") },
                    placeholder = { Text("e.g. Fresh Green Capsicum") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = isCategoryMenuExpanded,
                    onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryMenuExpanded,
                        onDismissRequest = { isCategoryMenuExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isCategoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mrpStr,
                        onValueChange = { mrpStr = it },
                        label = { Text("MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Product Description") },
                    placeholder = { Text("e.g. Fresh farm-picked produce...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Essential / Fresh Today:", fontSize = 13.sp)
                    Switch(
                        checked = isDailyEssential,
                        onCheckedChange = { isDailyEssential = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary)
                    )
                }

                // Fractional Purchase Admin Configuration
                if (isUnitDivisible) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (allowFractional) EmeraldContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Fractional Purchase", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Allow buying in 100g, 250g, 500g etc.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = allowFractional,
                                    onCheckedChange = { allowFractional = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary)
                                )
                            }
                            if (allowFractional) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Fraction Step:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = fractionStepGrams == 100,
                                        onClick = { fractionStepGrams = 100 },
                                        label = { Text("100g Step (Garlic/Ginger)", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = fractionStepGrams == 250,
                                        onClick = { fractionStepGrams = 250 },
                                        label = { Text("250g Step (Tomato/Veg)", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ℹ️ Fixed Per-Piece: Per-piece items ('$unit') cannot be bought in fractions.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            category,
                            unit.trim(),
                            priceStr.toDoubleOrNull() ?: 40.0,
                            mrpStr.toDoubleOrNull() ?: 50.0,
                            stockStr.toIntOrNull() ?: 50,
                            description.trim(),
                            imageUrl.trim(),
                            isDailyEssential,
                            if (isUnitDivisible) allowFractional else false,
                            fractionStepGrams
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text("Add to Catalog")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// EDIT PRODUCT DIALOG
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(product.unit) }
    var priceStr by remember { mutableStateOf(product.price.toInt().toString()) }
    var mrpStr by remember { mutableStateOf(product.mrp.toInt().toString()) }
    var stockStr by remember { mutableStateOf(product.stockQty.toString()) }
    var description by remember { mutableStateOf(product.description) }
    var imageUrl by remember { mutableStateOf(product.imageUrl) }
    var isDailyEssential by remember { mutableStateOf(product.isDailyEssential) }

    val isUnitDivisible = remember(unit) {
        val lower = unit.lowercase()
        val isPiece = lower.contains("pc") || lower.contains("piece") || lower.contains("bunch") || lower.contains("packet") || lower.contains("bottle")
        (lower.contains("kg") || lower.contains("gm") || lower.contains("gram") || lower.contains("l") || lower.contains("liter") || lower.contains("litre") || lower.contains("ltr") || lower.contains("ml")) && !isPiece
    }
    var allowFractional by remember { mutableStateOf(product.allowFractional && isUnitDivisible) }
    var fractionStepGrams by remember { mutableStateOf(product.fractionStepGrams) }

    val categories = listOf("Vegetables", "Fruits", "Dairy & Breakfast", "Staples & Atta")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product & Inventory", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = isCategoryMenuExpanded,
                    onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryMenuExpanded,
                        onDismissRequest = { isCategoryMenuExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isCategoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mrpStr,
                        onValueChange = { mrpStr = it },
                        label = { Text("MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Essential / Fresh Today:", fontSize = 13.sp)
                    Switch(
                        checked = isDailyEssential,
                        onCheckedChange = { isDailyEssential = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary)
                    )
                }

                // Fractional Purchase Admin Configuration
                if (isUnitDivisible) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (allowFractional) EmeraldContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Fractional Purchase", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Allow buying in 100g, 250g, 500g etc.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = allowFractional,
                                    onCheckedChange = { allowFractional = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreenPrimary)
                                )
                            }
                            if (allowFractional) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Fraction Step:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = fractionStepGrams == 100,
                                        onClick = { fractionStepGrams = 100 },
                                        label = { Text("100g Step (Garlic/Ginger)", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = fractionStepGrams == 250,
                                        onClick = { fractionStepGrams = 250 },
                                        label = { Text("250g Step (Tomato/Veg)", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ℹ️ Fixed Per-Piece: Per-piece items ('$unit') cannot be bought in fractions.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedStock = stockStr.toIntOrNull() ?: product.stockQty
                        onSave(
                            product.copy(
                                name = name.trim(),
                                category = category,
                                unit = unit.trim(),
                                price = priceStr.toDoubleOrNull() ?: product.price,
                                mrp = mrpStr.toDoubleOrNull() ?: product.mrp,
                                stockQty = parsedStock,
                                isAvailable = parsedStock > 0,
                                description = description.trim(),
                                imageUrl = imageUrl.trim(),
                                isDailyEssential = isDailyEssential,
                                allowFractional = if (isUnitDivisible) allowFractional else false,
                                fractionStepGrams = fractionStepGrams,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AdminSystemUpdatesTab(
    viewModel: GroceryViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val canInstall = viewModel.appUpdateManager.canRequestPackageInstalls()
    val updateStatus by viewModel.updateStatus.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Version Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreenDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Shapoorji Delivery v${BuildConfig.VERSION_NAME}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = EmeraldGreenDark
                                )
                                Text(
                                    text = "Internal Build: ${BuildConfig.VERSION_CODE} • Target SDK: 36",
                                    fontSize = 12.sp,
                                    color = EmeraldGreenDark.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldGreenDark)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = EmeraldGreenDark.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "App ID: com.aistudio.shapoorjidelivery.grocer",
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = EmeraldGreenDark
                    )
                }
            }
        }

        // In-Place Upgrade Compatibility Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seamless Upgrade Guarantee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val points = listOf(
                        "✓ In-Place Installation: Direct APK upgrade without uninstalling previous version",
                        "✓ Zero Data Loss: User login sessions, active cart, and delivery addresses are preserved",
                        "✓ Package Identity: com.aistudio.shapoorjidelivery.grocer remains permanent",
                        "✓ Key Signature Match: Consistent signing key maintains Android OS package validation",
                        "✓ Room Database Migration: Retains customer orders and product catalog safely"
                    )

                    points.forEach { point ->
                        Text(
                            text = point,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Action Controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Update Controls & Testing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Button(
                        onClick = { viewModel.checkForAppUpdates() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Official Updates")
                    }

                    OutlinedButton(
                        onClick = { viewModel.simulateUpgradeAvailable() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Test In-Place Upgrade Dialog Flow")
                    }

                    // Package Install permission status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canInstall) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (canInstall) "Direct Package Install Allowed" else "Install Permission Needed",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (canInstall) EmeraldGreenDark else Color(0xFFE65100)
                            )
                            Text(
                                text = if (canInstall) "Android OS allows this app to trigger updates" else "Tap to allow app update installation in Settings",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!canInstall) {
                            OutlinedButton(
                                onClick = { viewModel.appUpdateManager.openInstallPermissionSettings(context) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Settings", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
