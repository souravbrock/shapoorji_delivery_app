package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.theme.AmberAccent

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: ((Int) -> Unit)? = null,
    starSize: Dp = 24.dp,
    starColor: Color = AmberAccent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val isSelected = i <= rating
            Icon(
                imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star $i",
                tint = if (isSelected) starColor else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (onRatingChanged != null) {
                            Modifier.clickable { onRatingChanged(i) }
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
fun ProductReviewDialog(
    product: Product,
    customerEmail: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Rate & Review",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your Rating:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                StarRatingBar(
                    rating = rating,
                    onRatingChanged = { rating = it },
                    starSize = 32.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Write your review") },
                    placeholder = { Text("e.g. Very fresh delivery to my flat in Sukhobristi!") },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Please enter a few words about this product.")
                        } else {
                            Text("Reviewing as: $customerEmail")
                        }
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.isBlank()) {
                        isError = true
                    } else {
                        onSubmit(rating, comment.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Submit Review")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
