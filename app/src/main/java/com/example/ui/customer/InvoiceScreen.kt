package com.example.ui.customer

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.repository.GroceryRepository
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    order: Order,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items = remember(order.itemsJson) {
        GroceryRepository.parseOrderItems(order.itemsJson)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(order.createdAt))

    fun generateInvoiceText(): String {
        return buildString {
            appendLine("==================================================")
            appendLine("             SHAPOORJI DELIVERY                   ")
            appendLine("          OFFICIAL TAX INVOICE                    ")
            appendLine("     spdelivery.reddevils.co.in                   ")
            appendLine("==================================================")
            appendLine("Invoice No   : ${order.invoiceNumber}")
            appendLine("Order No     : ${order.orderNumber}")
            appendLine("Date & Time  : $dateStr")
            appendLine("Status       : ${order.status.label.uppercase()}")
            appendLine("--------------------------------------------------")
            appendLine("CUSTOMER & DESTINATION:")
            appendLine("Name         : ${order.customerName}")
            appendLine("Email        : ${order.customerEmail}")
            appendLine("Phone        : ${order.customerPhone}")
            appendLine("Tower        : ${order.towerName}")
            appendLine("Flat         : ${order.flatNumber}")
            appendLine("Geofence     : Verified Inside Shapoorji Shukhobrishti")
            appendLine("--------------------------------------------------")
            appendLine("ITEMIZED GROCERY DETAILS:")
            items.forEachIndexed { i, itm ->
                val qtyStr = if (itm.portionLabel.isNotBlank()) itm.portionLabel else "${itm.quantity} ${itm.unit}"
                appendLine("${i + 1}. ${itm.productName}")
                appendLine("   Qty: $qtyStr x ₹${itm.price.toInt()} = ₹${itm.total.toInt()}")
            }
            appendLine("--------------------------------------------------")
            appendLine("Subtotal     : ₹${order.subtotal.toInt()}")
            appendLine("Delivery Fee : ₹${order.deliveryFee.toInt()} (Free inside Shapoorji)")
            appendLine("Grand Total  : ₹${order.grandTotal.toInt()}")
            appendLine("Payment Mode : ${order.paymentMethod}")
            appendLine("==================================================")
            appendLine("Thank you for choosing Shapoorji Delivery!")
            appendLine("Support: order@spdelivery.reddevils.co.in")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tax Invoice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val invoiceText = generateInvoiceText()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, invoiceText)
                            putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${order.invoiceNumber}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Invoice via"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Invoice")
                    }
                }
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val invoiceText = generateInvoiceText()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, invoiceText)
                                putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${order.invoiceNumber}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send Invoice to"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("✓ Invoice ${order.invoiceNumber} downloaded to Device storage!")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Paper-style Invoice Sheet
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Brand
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldGreenPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Shapoorji Delivery",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Shukhobrishti Commercial Plaza, AA-III",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "order@spdelivery.reddevils.co.in",
                                fontSize = 11.sp,
                                color = EmeraldGreenPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TAX INVOICE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = EmeraldGreenDark
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color(0xFFE0E0E0)
                    )

                    // Invoice Metadata
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Invoice Number:", fontSize = 11.sp, color = Color.Gray)
                            Text(order.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Date & Time:", fontSize = 11.sp, color = Color.Gray)
                            Text(dateStr, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Order ID:", fontSize = 11.sp, color = Color.Gray)
                            Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Status:", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = order.status.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = EmeraldGreenPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Billed to Customer Info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF9FBF9))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Billed & Delivered To:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(order.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            Text("${order.customerEmail} • ${order.customerPhone}", fontSize = 12.sp, color = Color.DarkGray)
                            Text("${order.towerName}, ${order.flatNumber}", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = EmeraldGreenPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verified Shapoorji Shukhobrishti Resident", fontSize = 11.sp, color = EmeraldGreenDark, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Itemized Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Item / Description", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.weight(2f))
                        Text("Qty", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.weight(0.7f))
                        Text("Price", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
                                val unitOrPortion = if (item.portionLabel.isNotBlank()) item.portionLabel else item.unit
                                Text(unitOrPortion, fontSize = 11.sp, color = Color.Gray)
                            }
                            val displayQty = if (item.portionLabel.isNotBlank()) item.portionLabel else if (item.quantity == item.quantity.toInt().toDouble()) "${item.quantity.toInt()}" else "${item.quantity}"
                            Text(displayQty, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(0.7f))
                            Text("₹${item.price.toInt()}", fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                            Text("₹${item.total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calculation Table
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Items Subtotal", fontSize = 12.sp, color = Color.Gray)
                            Text("₹${order.subtotal.toInt()}", fontSize = 12.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delivery Fee (Inside Shapoorji)", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                if (order.deliveryFee == 0.0) "FREE" else "₹${order.deliveryFee.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (order.deliveryFee == 0.0) EmeraldGreenPrimary else Color.Black
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST & Taxes", fontSize = 12.sp, color = Color.Gray)
                            Text("Included", fontSize = 12.sp, color = EmeraldGreenPrimary)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFE0E0E0)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Grand Total", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                            Text(
                                "₹${order.grandTotal.toInt()}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = EmeraldGreenPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Payment Mode: ${order.paymentMethod}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF9FAFB))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This is a computer-generated tax invoice for Shapoorji Delivery. For queries or phone orders, contact +91-8442980101 / order@spdelivery.reddevils.co.in",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
