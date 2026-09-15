package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ShapoorjiGeo
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenPrimary
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapoorjiMapLocationPicker(
    selectedTower: String,
    onTowerChange: (String) -> Unit,
    flatInput: String,
    onFlatChange: (String) -> Unit,
    notesInput: String,
    onNotesChange: (String) -> Unit,
    latitude: Double,
    longitude: Double,
    isInsideShapoorji: Boolean,
    onCoordinatesChange: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isTowerDropdownExpanded by remember { mutableStateOf(false) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        onCoordinatesChange(loc.latitude, loc.longitude)
                    } else {
                        // Default to center if hardware emulator has no fix
                        onCoordinatesChange(ShapoorjiGeo.CENTER_LATITUDE, ShapoorjiGeo.CENTER_LONGITUDE)
                    }
                }
            } catch (e: SecurityException) {
                // permission revoked
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Delivery Address & GPS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Strictly serving inside Shapoorji",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // GPS detect button
                OutlinedButton(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasFine || hasCoarse) {
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        onCoordinatesChange(loc.latitude, loc.longitude)
                                    } else {
                                        onCoordinatesChange(ShapoorjiGeo.CENTER_LATITUDE, ShapoorjiGeo.CENTER_LONGITUDE)
                                    }
                                }
                            } catch (e: SecurityException) {
                                // Handled
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "GPS",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Get GPS", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Geofence Map Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE8F5E9))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val geofenceRadius = size.height * 0.42f

                    // Draw geofence boundary (Shapoorji Sukhobristi Boundary)
                    drawCircle(
                        color = Color(0x334CAF50),
                        radius = geofenceRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF2E7D32),
                        radius = geofenceRadius,
                        center = center,
                        style = Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    )

                    // Draw center marker (Shapoorji Complex Club House)
                    drawCircle(
                        color = Color(0xFF1B5E20),
                        radius = 6f,
                        center = center
                    )

                    // Draw user marker based on whether inside or outside
                    val userMarkerOffset = if (isInsideShapoorji) {
                        Offset(center.x + 20f, center.y - 15f)
                    } else {
                        Offset(center.x + geofenceRadius + 35f, center.y - 25f)
                    }

                    drawCircle(
                        color = if (isInsideShapoorji) Color(0xFF1565C0) else Color(0xFFD32F2F),
                        radius = 10f,
                        center = userMarkerOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = userMarkerOffset
                    )
                }

                // Map Overlay Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shapoorji Sukhobristi Township (AA-III)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldGreenDark
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "GPS: %.4f, %.4f", latitude, longitude),
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Verification Status Banner
            val containerColor = if (isInsideShapoorji) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            val contentColor = if (isInsideShapoorji) Color(0xFF1B5E20) else Color(0xFFB71C1C)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(containerColor)
                    .border(1.dp, contentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isInsideShapoorji) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = "Status",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isInsideShapoorji) "Location Verified: Inside Shapoorji" else "Location Outside Shapoorji!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = contentColor
                        )
                        Text(
                            text = if (isInsideShapoorji)
                                "Your pin is verified within Sukhobristi geofence. Doorstep delivery is active."
                            else
                                "Shapoorji Delivery operates exclusively inside Shapoorji Sukhobristi. Orders outside cannot be placed.",
                            fontSize = 11.sp,
                            color = contentColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Location Simulation / Quick Pin Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onCoordinatesChange(22.5695, 88.5195) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EmeraldGreenPrimary
                    )
                ) {
                    Text("Pin: Inside Shapoorji", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onCoordinatesChange(22.5868, 88.4355) }, // Sector V Kolkata
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CoralRed
                    )
                ) {
                    Text("Simulate: Outside Zone", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tower Dropdown
            ExposedDropdownMenuBox(
                expanded = isTowerDropdownExpanded,
                onExpandedChange = { isTowerDropdownExpanded = !isTowerDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedTower,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Shapoorji Tower / Building") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTowerDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = isTowerDropdownExpanded,
                    onDismissRequest = { isTowerDropdownExpanded = false }
                ) {
                    ShapoorjiGeo.TOWERS.forEach { tower ->
                        DropdownMenuItem(
                            text = { Text(tower) },
                            onClick = {
                                onTowerChange(tower)
                                isTowerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Flat / Unit Input
            OutlinedTextField(
                value = flatInput,
                onValueChange = onFlatChange,
                label = { Text("Flat No. & Floor") },
                placeholder = { Text("e.g. Flat 803, 8th Floor") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Delivery Notes Input
            OutlinedTextField(
                value = notesInput,
                onValueChange = onNotesChange,
                label = { Text("Delivery Instructions (Optional)") },
                placeholder = { Text("e.g. Leave with security / Ring bell") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
