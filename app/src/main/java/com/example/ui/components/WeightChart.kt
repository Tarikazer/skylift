package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BodyWeightLog
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeightChart(
    logs: List<BodyWeightLog>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weight_trend_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weight Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No weight logs recorded for this period.\nTap '+ Log' below to start tracking!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else if (logs.size == 1) {
                val single = logs.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${single.weightKg} kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SkyBluePrimary
                        )
                        Text(
                            text = "First entry recorded! Add more to see the progress curve.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                var selectedLog by remember { mutableStateOf<BodyWeightLog?>(null) }
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant

                // Header for tapped point
                if (selectedLog != null) {
                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        .format(Date(selectedLog!!.timestamp))
                    Text(
                        text = "Selected: ${selectedLog!!.weightKg} kg on $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyBluePrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(logs) {
                                detectTapGestures { offset ->
                                    val w = size.width
                                    val pointSpacing = w / (logs.size - 1).coerceAtLeast(1)
                                    val index = ((offset.x + pointSpacing / 2) / pointSpacing).toInt()
                                        .coerceIn(0, logs.size - 1)
                                    selectedLog = logs[index]
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        val paddingHoriz = 30f
                        val paddingTop = 25f
                        val paddingBottom = 30f

                        val chartWidth = width - 2 * paddingHoriz
                        val chartHeight = height - paddingTop - paddingBottom

                        val minVal = (logs.minOf { it.weightKg } - 1.0).coerceAtLeast(0.0)
                        val maxVal = logs.maxOf { it.weightKg } + 1.0
                        val valRange = (maxVal - minVal).coerceAtLeast(1.0)

                        val pointSpacing = chartWidth / (logs.size - 1).coerceAtLeast(1)

                        // Draw Grid guidelines
                        val steps = 3
                        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        for (i in 0..steps) {
                            val y = paddingTop + (chartHeight / steps) * i
                            drawLine(
                                color = outlineColor,
                                start = Offset(paddingHoriz, y),
                                end = Offset(width - paddingHoriz, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dashPathEffect
                            )
                        }

                        // Calculate points
                        val points = logs.mapIndexed { index, log ->
                            val x = paddingHoriz + index * pointSpacing
                            val normY = ((log.weightKg - minVal) / valRange).toFloat()
                            val y = paddingTop + chartHeight * (1f - normY)
                            Offset(x, y)
                        }

                        // Fill under curve
                        val fillPath = Path().apply {
                            moveTo(points.first().x, height - paddingBottom)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height - paddingBottom)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.0f)
                                ),
                                startY = paddingTop,
                                endY = height - paddingBottom
                            )
                        )

                        // Line stroke
                        val strokePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw dots
                        points.forEachIndexed { idx, point ->
                            val isSelected = selectedLog?.id == logs[idx].id
                            drawCircle(
                                color = if (isSelected) SkyBlueLight else primaryColor,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }
            }
        }
    }
}
