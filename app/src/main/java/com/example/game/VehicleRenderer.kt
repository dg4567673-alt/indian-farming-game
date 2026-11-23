package com.example.game

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawVehicleTopDown(
    vehicle: VehicleEntity,
    isActive: Boolean,
    isNight: Boolean,
    hornFlash: Float
) {
    val deg = vehicle.angleRad * (180f / PI.toFloat())
    withTransform({
        translate(vehicle.x, vehicle.y)
        rotate(deg, pivot = Offset.Zero)
    }) {
        // Active ring highlight
        if (isActive) {
            drawCircle(
                color = Color(0xFFFFD166).copy(alpha = 0.28f),
                radius = vehicle.type.bodyLength * 0.68f,
                center = Offset.Zero
            )
            drawCircle(
                color = Color(0xFFFFB703),
                radius = vehicle.type.bodyLength * 0.68f,
                center = Offset.Zero,
                style = Stroke(width = 3f)
            )
        }

        // Headlight beams at dusk/night or when horn honks
        if (isNight || hornFlash > 0f) {
            val beamAlpha = if (hornFlash > 0f) 0.42f else 0.28f
            val beamPath = Path().apply {
                moveTo(vehicle.type.bodyLength * 0.48f, -vehicle.type.bodyWidth * 0.28f)
                lineTo(vehicle.type.bodyLength * 0.48f + 190f, -85f)
                lineTo(vehicle.type.bodyLength * 0.48f + 190f, 85f)
                lineTo(vehicle.type.bodyLength * 0.48f, vehicle.type.bodyWidth * 0.28f)
                close()
            }
            drawPath(beamPath, Color(0xFFFFF3B0).copy(alpha = beamAlpha))
        }

        // Ground shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(-vehicle.type.bodyLength * 0.5f + 5f, -vehicle.type.bodyWidth * 0.5f + 6f),
            size = Size(vehicle.type.bodyLength, vehicle.type.bodyWidth),
            cornerRadius = CornerRadius(10f, 10f)
        )

        when (vehicle.type) {
            VehicleType.TRACTOR -> drawTractorShape(vehicle)
            VehicleType.HARVESTER -> drawHarvesterShape(vehicle)
            VehicleType.JCB -> drawJcbShape(vehicle)
            VehicleType.BULLDOZER -> drawBulldozerShape(vehicle)
            VehicleType.TRUCK -> drawTruckShape(vehicle)
            VehicleType.PICKUP -> drawPickupShape(vehicle)
        }
    }
}

private fun DrawScope.drawTractorShape(v: VehicleEntity) {
    // Big lugged rear wheels
    drawRoundRect(Color(0xFF1F2421), Offset(-34f, -26f), Size(26f, 11f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1F2421), Offset(-34f, 15f), Size(26f, 11f), CornerRadius(3f, 3f))
    // Steering front wheels
    drawRoundRect(Color(0xFF1F2421), Offset(16f, -22f), Size(16f, 8f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1F2421), Offset(16f, 14f), Size(16f, 8f), CornerRadius(3f, 3f))
    // Chassis & crimson engine hood
    drawRoundRect(Color(0xFF333333), Offset(-30f, -15f), Size(62f, 30f), CornerRadius(6f, 6f))
    drawRoundRect(v.type.primaryColor, Offset(-26f, -14f), Size(56f, 28f), CornerRadius(8f, 8f))
    // Yellow racing stripe on hood
    drawRect(Color(0xFFFFB703), Offset(-2f, -3f), Size(28f, 6f))
    // Driver canopy / seat
    drawRoundRect(Color(0xFF1B4332), Offset(-24f, -12f), Size(20f, 24f), CornerRadius(5f, 5f))
    drawCircle(Color(0xFF111111), 6f, Offset(-14f, 0f))
    // Exhaust pipe
    drawCircle(Color(0xFF555555), 3.5f, Offset(10f, -11f))
    // Headlights
    drawCircle(Color(0xFFFFF3B0), 3.5f, Offset(31f, -9f))
    drawCircle(Color(0xFFFFF3B0), 3.5f, Offset(31f, 9f))
}

private fun DrawScope.drawHarvesterShape(v: VehicleEntity) {
    val reelShift = sin(v.boomPhase * 2.5f) * 3f
    // Front wide cutter header bar
    drawRoundRect(Color(0xFFB5179E), Offset(28f, -44f), Size(22f, 88f), CornerRadius(4f, 4f))
    drawRect(Color(0xFFFFD166), Offset(38f + reelShift, -42f), Size(5f, 84f))
    // Wheels
    drawRoundRect(Color(0xFF1A1A1A), Offset(-10f, -34f), Size(26f, 10f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(-10f, 24f), Size(26f, 10f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(-42f, -26f), Size(18f, 8f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(-42f, 18f), Size(18f, 8f), CornerRadius(3f, 3f))
    // Main harvester body
    drawRoundRect(v.type.primaryColor, Offset(-44f, -26f), Size(76f, 52f), CornerRadius(10f, 10f))
    // Grain tank inside body
    drawRoundRect(Color(0xFF1B4332), Offset(-36f, -18f), Size(40f, 36f), CornerRadius(6f, 6f))
    drawRoundRect(Color(0xFFE9C46A), Offset(-32f, -14f), Size(32f, 28f), CornerRadius(4f, 4f))
    // Cab
    drawRoundRect(Color(0xFF90E0EF), Offset(8f, -16f), Size(20f, 32f), CornerRadius(6f, 6f))
    // Unloading auger pipe
    drawRoundRect(Color(0xFFDDDDDD), Offset(-38f, -34f), Size(34f, 7f), CornerRadius(3f, 3f))
}

private fun DrawScope.drawJcbShape(v: VehicleEntity) {
    val armSwing = sin(v.boomPhase) * 6f
    // Wheels
    drawRoundRect(Color(0xFF1E1E1E), Offset(-24f, -25f), Size(24f, 10f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1E1E1E), Offset(-24f, 15f), Size(24f, 10f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1E1E1E), Offset(10f, -23f), Size(18f, 9f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1E1E1E), Offset(10f, 14f), Size(18f, 9f), CornerRadius(3f, 3f))
    // Front loader bucket + arms
    drawRect(Color(0xFF333333), Offset(20f, -19f), Size(18f, 5f))
    drawRect(Color(0xFF333333), Offset(20f, 14f), Size(18f, 5f))
    drawRoundRect(Color(0xFF2B2D42), Offset(35f, -23f), Size(14f, 46f), CornerRadius(4f, 4f))
    // Main yellow chassis + black cab
    drawRoundRect(v.type.primaryColor, Offset(-24f, -17f), Size(48f, 34f), CornerRadius(6f, 6f))
    drawRoundRect(Color(0xFF2B2D42), Offset(-18f, -13f), Size(24f, 26f), CornerRadius(5f, 5f))
    drawRoundRect(Color(0xFF90E0EF), Offset(-14f, -10f), Size(16f, 20f), CornerRadius(3f, 3f))
    // Rear backhoe excavator boom
    drawRoundRect(Color(0xFFFFB703), Offset(-48f, -5f + armSwing * 0.4f), Size(26f, 10f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF222222), Offset(-56f, -8f + armSwing * 0.6f), Size(10f, 16f), CornerRadius(3f, 3f))
}

private fun DrawScope.drawBulldozerShape(v: VehicleEntity) {
    // Heavy caterpillar tracks
    drawRoundRect(Color(0xFF252525), Offset(-36f, -28f), Size(64f, 14f), CornerRadius(4f, 4f))
    drawRoundRect(Color(0xFF252525), Offset(-36f, 14f), Size(64f, 14f), CornerRadius(4f, 4f))
    // Track tread lines
    for (tx in -30..22 step 10) {
        drawLine(Color(0xFF555555), Offset(tx.toFloat(), -27f), Offset(tx.toFloat(), -15f), strokeWidth = 2f)
        drawLine(Color(0xFF555555), Offset(tx.toFloat(), 15f), Offset(tx.toFloat(), 27f), strokeWidth = 2f)
    }
    // Front heavy steel dozer blade
    drawRect(Color(0xFF555555), Offset(18f, -22f), Size(16f, 6f))
    drawRect(Color(0xFF555555), Offset(18f, 16f), Size(16f, 6f))
    drawRoundRect(Color(0xFF495057), Offset(32f, -33f), Size(14f, 66f), CornerRadius(4f, 4f))
    drawRect(Color(0xFFFFB703), Offset(34f, -31f), Size(4f, 62f))
    // Orange body + cab
    drawRoundRect(v.type.primaryColor, Offset(-32f, -18f), Size(54f, 36f), CornerRadius(6f, 6f))
    drawRoundRect(Color(0xFF2B2D42), Offset(-24f, -12f), Size(22f, 24f), CornerRadius(4f, 4f))
}

private fun DrawScope.drawTruckShape(v: VehicleEntity) {
    // Wheels
    drawRoundRect(Color(0xFF1A1A1A), Offset(-44f, -26f), Size(20f, 8f), CornerRadius(2f, 2f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(-44f, 18f), Size(20f, 8f), CornerRadius(2f, 2f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(28f, -26f), Size(18f, 8f), CornerRadius(2f, 2f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(28f, 18f), Size(18f, 8f), CornerRadius(2f, 2f))
    // Rear cargo bed
    drawRoundRect(Color(0xFFB45309), Offset(-56f, -23f), Size(74f, 46f), CornerRadius(5f, 5f))
    drawRoundRect(Color(0xFFD97706), Offset(-52f, -19f), Size(66f, 38f), CornerRadius(4f, 4f))
    // Decorative Indian truck strips
    drawRect(Color(0xFFEF4444), Offset(-52f, -5f), Size(66f, 10f))
    // Front cabin
    drawRoundRect(v.type.primaryColor, Offset(20f, -22f), Size(36f, 44f), CornerRadius(8f, 8f))
    drawRoundRect(Color(0xFF93C5FD), Offset(40f, -17f), Size(11f, 34f), CornerRadius(3f, 3f))
    drawCircle(Color(0xFFFFF3B0), 3.5f, Offset(55f, -15f))
    drawCircle(Color(0xFFFFF3B0), 3.5f, Offset(55f, 15f))
}

private fun DrawScope.drawPickupShape(v: VehicleEntity) {
    drawRoundRect(Color(0xFF1A1A1A), Offset(-28f, -22f), Size(16f, 7f), CornerRadius(2f, 2f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(-28f, 15f), Size(16f, 7f), CornerRadius(2f, 2f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(16f, -22f), Size(16f, 7f), CornerRadius(2f, 2f))
    drawRoundRect(Color(0xFF1A1A1A), Offset(16f, 15f), Size(16f, 7f), CornerRadius(2f, 2f))
    // Body
    drawRoundRect(v.type.primaryColor, Offset(-38f, -19f), Size(76f, 38f), CornerRadius(8f, 8f))
    // Rear open bed
    drawRoundRect(Color(0xFF334155), Offset(-34f, -15f), Size(32f, 30f), CornerRadius(4f, 4f))
    // Cabin & stripe
    drawRect(v.type.accentColor, Offset(-2f, -19f), Size(40f, 5f))
    drawRect(v.type.accentColor, Offset(-2f, 14f), Size(40f, 5f))
    drawRoundRect(Color(0xFF38BDF8), Offset(16f, -14f), Size(12f, 28f), CornerRadius(3f, 3f))
    drawCircle(Color(0xFFFFF3B0), 3f, Offset(38f, -12f))
    drawCircle(Color(0xFFFFF3B0), 3f, Offset(38f, 12f))
}

fun DrawScope.drawTrolleyTopDown(trolley: TrolleyEntity) {
    val deg = trolley.angleRad * (180f / PI.toFloat())
    withTransform({
        translate(trolley.x, trolley.y)
        rotate(deg, pivot = Offset.Zero)
    }) {
        // Hitch drawbar
        drawRect(Color(0xFF333333), Offset(28f, -3f), Size(22f, 6f))
        // Wheels
        drawRoundRect(Color(0xFF1A1A1A), Offset(-18f, -24f), Size(18f, 8f), CornerRadius(2f, 2f))
        drawRoundRect(Color(0xFF1A1A1A), Offset(-18f, 16f), Size(18f, 8f), CornerRadius(2f, 2f))
        // Trolley bed
        drawRoundRect(Color(0xFF9A3412), Offset(-34f, -20f), Size(64f, 40f), CornerRadius(5f, 5f))
        drawRoundRect(Color(0xFF7C2D12), Offset(-30f, -16f), Size(56f, 32f), CornerRadius(3f, 3f))
        // Dynamic grain heap inside trolley
        if (trolley.load > 0) {
            val fillRatio = (trolley.load.toFloat() / trolley.maxLoad).coerceIn(0.15f, 1f)
            drawRoundRect(
                color = trolley.cropType.ripeColor,
                topLeft = Offset(-28f, -14f),
                size = Size(52f * fillRatio, 28f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}
