package com.plcoding.cryptotracker.crypto.presentation.coin_details.line_chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.cryptotracker.crypto.domain.CoinPrice
import com.plcoding.cryptotracker.ui.theme.CryptoTrackerTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun LineChart(
    data: List<DataPoint>,
    style: ChartStyle,
    visibleDataPointsIndices: IntRange,
    unit: String,
    modifier: Modifier = Modifier,
    selectedDataPoint: DataPoint? = null,
    onSelectedDataPoint: (DataPoint) -> Unit = {},
    onXLabelWidthChange: (Float) -> Unit = {},
    showHelperLines: Boolean = true,
) {
    val textStyle = LocalTextStyle.current.copy(
        fontSize = style.labelFontSize
    )

    val visibleDataPoints = remember(data, visibleDataPointsIndices) {
        data.slice(visibleDataPointsIndices)
    }

    val maxYValue = remember(visibleDataPoints) {
        visibleDataPoints.maxOfOrNull { it.y } ?: 0f
    }

    val minYValue = remember(visibleDataPoints) {
        visibleDataPoints.minOfOrNull { it.y } ?: 0f
    }

    val measurer = rememberTextMeasurer()

    var xLabelWidth by remember {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(key1 = xLabelWidth) {
        onXLabelWidthChange(xLabelWidth)
    }

    val selectedDataPointIndex = remember(selectedDataPoint) {
        data.indexOf(selectedDataPoint)
    }

    var drawPoints by remember {
        mutableStateOf(listOf<DataPoint>())
    }

    var isShowingDataPoints by remember {
        mutableStateOf(selectedDataPoint != null)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(drawPoints, xLabelWidth) {
                detectHorizontalDragGestures { change, _ ->
                    val newSelectedDataPointIndex = getSelectedDataPointIndex(
                        touchOffsetX = change.position.x,
                        triggerWidth = xLabelWidth,
                        drawPoints = drawPoints
                    )

                    isShowingDataPoints =
                        (newSelectedDataPointIndex + visibleDataPointsIndices.first) in visibleDataPointsIndices

                    if (isShowingDataPoints) {
                        onSelectedDataPoint(data[newSelectedDataPointIndex])
                    }
                }
            }
    ) {
        val minYLabelSpacingPx = style.minYLabelSpacing.toPx()
        val verticalPaddingPx = style.verticalPadding.toPx()
        val horizontalPaddingPx = style.horizontalPadding.toPx()
        val xAxisLabelSpacingPx = style.xAxisLabelSpacing.toPx()

        val xLabelTextLayoutResults = visibleDataPoints.map {
            measurer.measure(
                text = it.xLabel,
                style = textStyle.copy(textAlign = TextAlign.Center)
            )
        }
        val maxXLabelWidth = xLabelTextLayoutResults.maxOfOrNull { it.size.width } ?: 0
        val maxXLabelHeight = xLabelTextLayoutResults.maxOfOrNull { it.size.height } ?: 0
        val maxXLabelLineCount = xLabelTextLayoutResults.maxOfOrNull { it.lineCount } ?: 0
        val labelLineHeight = maxXLabelHeight / maxXLabelLineCount


        val viewPortHeightPx =
            size.height - (maxXLabelHeight + 2 * verticalPaddingPx + labelLineHeight + xAxisLabelSpacingPx)

        val labelViewPortHeightPx = viewPortHeightPx + labelLineHeight
        val labelCountWithNoLastLabel =
            ((labelViewPortHeightPx / (labelLineHeight + minYLabelSpacingPx))).toInt()

        val valueDelta = (maxYValue - minYValue) / labelCountWithNoLastLabel

        val yLabels = (0..labelCountWithNoLastLabel).map {
            ValueLabel(
                value = maxYValue - (valueDelta * it),
                unit = unit
            )
        }

        val yLabelTextLayoutResults = yLabels.map {
            measurer.measure(
                text = it.formatted(),
                style = textStyle
            )
        }
        val maxYLabelWidth = yLabelTextLayoutResults.maxOfOrNull { it.size.width } ?: 0

        val viewPortTopY = verticalPaddingPx + labelLineHeight + 10f
        val viewPortBottomY = viewPortTopY + viewPortHeightPx
        val viewPortLeftX = 2f * horizontalPaddingPx + maxYLabelWidth
        val viewPortRightX = size.width

        var xOffset = 0f
        xLabelWidth = maxXLabelWidth + xAxisLabelSpacingPx
        xLabelTextLayoutResults.forEachIndexed { i, result ->
            val x = viewPortLeftX + xAxisLabelSpacingPx / 2f + xLabelWidth * i
            xOffset = (xLabelWidth - result.size.width) / 2f
            drawText(
                textLayoutResult = result,
                topLeft = Offset(
                    x = x + xOffset,
                    y = viewPortBottomY + xAxisLabelSpacingPx
                ),
                color = if (i == selectedDataPointIndex) style.selectedColor else style.unselectedColor,
            )

            if (showHelperLines) {
                drawLine(
                    color = if (i == selectedDataPointIndex) style.selectedColor else style.unselectedColor,
                    start = Offset(
                        x = x + xLabelWidth / 2f,
                        y = viewPortTopY
                    ),
                    end = Offset(
                        x = x + xLabelWidth / 2f,
                        y = viewPortBottomY
                    ),
                    strokeWidth = if (i == selectedDataPointIndex) style.helperLinesThicknessPx * 1.8f else style.helperLinesThicknessPx
                )
            }

            if (selectedDataPointIndex == i) {
                val valueLabel = ValueLabel(
                    value = visibleDataPoints[i].y,
                    unit = unit
                )
                val valueResult = measurer.measure(
                    text = valueLabel.formatted(),
                    style = textStyle.copy(
                        color = style.selectedColor
                    ),
                    maxLines = 1
                )

                val textPositionX = if (selectedDataPointIndex == visibleDataPointsIndices.last) {
                    x - valueResult.size.width
                } else {
                    x - valueResult.size.width / 2f
                } + result.size.width / 2f

                val isTextInVisibleRange =
                    (size.width - textPositionX).roundToInt() in 0..size.width.roundToInt()
                if (isTextInVisibleRange) {
                    drawText(
                        textLayoutResult = valueResult,
                        topLeft = Offset(
                            x = textPositionX,
                            y = viewPortTopY - valueResult.size.height - 10f
                        ),
                    )
                }
            }
        }

        val totalLabelsHeight = labelLineHeight * (labelCountWithNoLastLabel + 1)
        val remainingHeightForLabels = (labelViewPortHeightPx - totalLabelsHeight)
        val labelSpacing = remainingHeightForLabels / labelCountWithNoLastLabel

        yLabelTextLayoutResults.forEachIndexed { i, result ->
            val y = viewPortTopY + i * (labelLineHeight + labelSpacing) - labelLineHeight / 2f
            val x = horizontalPaddingPx + maxYLabelWidth - result.size.width.toFloat()
            drawText(
                textLayoutResult = result,
                topLeft = Offset(
                    x = x,
                    y = y
                ),
                color = style.unselectedColor
            )

            if (showHelperLines) {
                drawLine(
                    color = style.unselectedColor,
                    start = Offset(
                        x = viewPortLeftX,
                        y = y + result.size.height.toFloat() / 2f
                    ),
                    end = Offset(
                        x = viewPortRightX,
                        y = y + result.size.height.toFloat() / 2f
                    ),
                    strokeWidth = style.helperLinesThicknessPx
                )
            }
        }

        drawPoints = visibleDataPointsIndices.map {
            val x =
                viewPortLeftX + (it - visibleDataPointsIndices.first) * xLabelWidth + xLabelWidth / 2f

            val ratio = (data[it].y - minYValue) / (maxYValue - minYValue)
            val y = viewPortTopY + viewPortHeightPx * (1 - ratio)

            DataPoint(
                x = x + xOffset,
                y = y,
                xLabel = data[it].xLabel
            )
        }

        val connectionPoints1 = mutableListOf<DataPoint>()
        val connectionPoints2 = mutableListOf<DataPoint>()

        for (i in 1 until drawPoints.size) {
            val p0 = drawPoints[i - 1]
            val p1 = drawPoints[i]

            val x = (p1.x + p0.x) / 2f
            val y1 = p0.y
            val y2 = p1.y

            connectionPoints1.add(DataPoint(x, y1, ""))
            connectionPoints2.add(DataPoint(x, y2, ""))
        }

        val linePath = Path().apply {
            if (drawPoints.isNotEmpty()) {
                moveTo(drawPoints.first().x, drawPoints.first().y)

                for (i in 1 until drawPoints.size) {
                    cubicTo(
                        x1 = connectionPoints1[i - 1].x,
                        y1 = connectionPoints1[i - 1].y,
                        x2 = connectionPoints2[i - 1].x,
                        y2 = connectionPoints2[i - 1].y,
                        x3 = drawPoints[i].x,
                        y3 = drawPoints[i].y
                    )
                }
            }
        }

        drawPath(
            path = linePath,
            color = style.chartLineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        drawPoints.forEachIndexed { i, point ->
            if (isShowingDataPoints) {
                val centerOffset = Offset(x = point.x, y = point.y)
                drawCircle(
                    color = style.selectedColor,
                    radius = 10f,
                    center = centerOffset
                )

                if (selectedDataPointIndex == i) {
                    drawCircle(
                        color = Color.White,
                        radius = 15f,
                        center = centerOffset
                    )
                    drawCircle(
                        color = style.selectedColor,
                        radius = 15f,
                        center = centerOffset,
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

private fun getSelectedDataPointIndex(
    touchOffsetX: Float,
    triggerWidth: Float,
    drawPoints: List<DataPoint>
): Int {
    val triggerRangeLeft = touchOffsetX - triggerWidth / 2f
    val triggerRangeRight = touchOffsetX + triggerWidth / 2f
    return drawPoints.indexOfFirst {
        it.x in triggerRangeLeft..triggerRangeRight
    }
}

@Preview(widthDp = 1000)
@Composable
private fun LineChartPreview() {
    CryptoTrackerTheme {
        val coinHistory = remember {
            (1..20).map {
                CoinPrice(
                    priceUsd = Random.nextFloat() * 1000.0,
                    dateTime = ZonedDateTime.now().minusDays(2L).plusHours((it * 3).toLong())
                )
            }
        }
        val style = ChartStyle(
            chartLineColor = Color.Black,
            unselectedColor = Color(0xFF7C7C7C),
            selectedColor = Color.Black,
            helperLinesThicknessPx = 1f,
            axisLinesThicknessPx = 5f,
            labelFontSize = 14.sp,
            minYLabelSpacing = 25.dp,
            verticalPadding = 8.dp,
            horizontalPadding = 8.dp,
            xAxisLabelSpacing = 8.dp
        )

        val dataPoints = remember {
            coinHistory.map {
                DataPoint(
                    x = it.dateTime.hour.toFloat(),
                    y = it.priceUsd.toFloat(),
                    xLabel = DateTimeFormatter
                        .ofPattern("ha\nM/d")
                        .format(it.dateTime)
                )
            }
        }

        LineChart(
            data = dataPoints,
            style = style,
            visibleDataPointsIndices = 0..19,
            unit = "$",
            modifier = Modifier
                .width(700.dp)
                .height(300.dp)
                .background(Color.White),
            selectedDataPoint = dataPoints[2]
        )
    }
}
