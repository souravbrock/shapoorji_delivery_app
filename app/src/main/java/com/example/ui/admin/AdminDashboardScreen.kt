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
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationLog
import com.example.data.model.NotificationType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
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
    val tabs = listOf("Orders", "Daily Prices", "Products", "Notifications")

    val orders by viewModel.allOrders.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val logs by viewModel.notificationLogs.collectAsState()

    var showAddProductDialog by remember { mutableStateOf(false) }

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
                            text = "Shapoorji Sukhobristi Operations",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
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
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
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
                    onDeleteProduct = { viewModel.deleteProduct(it) },
                    onAddClick = { showAddProductDialog = true }
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
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onSave = { name, category, unit, price, mrp, stock, desc, url, isDaily ->
                viewModel.addProduct(name, category, unit, price, mrp, stock, desc, url, isDaily)
                showAddProductDialog = false
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
    onDeleteProduct: (Product) -> Unit,
    onAddClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "${product.category} • ${product.unit} • ₹${product.price.toInt()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Stock: ${product.stockQty} units", fontSize = 11.sp, color = EmeraldGreenPrimary)
                    }

                    IconButton(onClick = { onDeleteProduct(product) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CoralRed
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: NOTIFICATIONS & TELEGRAM SETTINGS
// -------------------------------------------------------------
@Composable
fun AdminNotificationLogsTab(
    logs: List<NotificationLog>,
    viewModel: GroceryViewModel,
    onTestAlert: (Boolean) -> Unit
) {
    var botTokenInput by remember { mutableStateOf("") }
    var adminChatInput by remember { mutableStateOf("") }
    var managerChatInput by remember { mutableStateOf("") }
    var staffChatInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notification Strategy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = EmeraldGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email & Telegram Dispatch System",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = EmeraldGreenDark
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Official Email: order@spdelivery.reddevils.co.in\n• Auto-dispatches to: Customer Email & souravbrock@gmail.com\n• Triggered on: Order Placed + All Status Updates\n• Telegram Bot: Broadcasts to Admin, Store Managers & Store Staff",
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Telegram", tint = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Telegram Bot Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = botTokenInput,
                        onValueChange = { botTokenInput = it },
                        label = { Text("Telegram Bot API Token") },
                        placeholder = { Text("e.g. 123456789:ABCdef-...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adminChatInput,
                        onValueChange = { adminChatInput = it },
                        label = { Text("Admin Telegram Chat ID (souravbrock)") },
                        placeholder = { Text("e.g. 987654321") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = managerChatInput,
                        onValueChange = { managerChatInput = it },
                        label = { Text("Store Manager Chat ID") },
                        placeholder = { Text("e.g. 876543210") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.updateTelegramSettings(
                                    botTokenInput,
                                    adminChatInput,
                                    managerChatInput,
                                    staffChatInput
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Config", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.sendTestTelegramAlert { success ->
                                    onTestAlert(success)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                        ) {
                            Text("Test Bot Alert", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Live Log Feed
        item {
            Text(
                text = "Dispatch Audit Trail (${logs.size} notifications sent):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        items(logs) { log ->
            NotificationLogCard(log = log)
        }
    }
}

@Composable
fun NotificationLogCard(log: NotificationLog) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
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

                Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        isDaily: Boolean
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
                    onValueChange = { name = it },
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
                            isDailyEssential
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
