package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FarmingWorldCanvas(
    world: GameWorldState,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 1f, 2f, android.graphics.Color.BLACK)
        }
    }
    val popupPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(5f, 1f, 2f, android.graphics.Color.BLACK)
        }
    }

    val activeVehicle = world.vehicles[world.activeVehicle]
    val camX = activeVehicle?.x ?: 640f
    val camY = activeVehicle?.y ?: 480f
    val isNight = world.timeOfDay > 0.78f || world.timeOfDay < 0.14f

    Canvas(modifier = modifier.fillMaxSize()) {
        val viewW = size.width
        val viewH = size.height
        val zoom = (minOf(viewW, viewH) / 720f).coerceIn(0.68f, 1.15f)
        val offsetX = viewW * 0.5f - camX * zoom
        val offsetY = viewH * 0.5f - camY * zoom

        // Base countryside green backdrop
        drawRect(Color(0xFF4F8A47))

        withTransform({
            translate(offsetX, offsetY)
            scale(zoom, zoom, pivot = Offset.Zero)
        }) {
            drawTerrainAndRoads(world)
            drawFarmFields(world, labelPaint)
            drawVillageZones(world, labelPaint)
            drawVillageHouses(world.houses, labelPaint)
            drawLivestock(world.animals)

            // Draw dust particles
            world.particles.forEach { p ->
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f) * 0.65f
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
            }

            // Draw NPC traffic on main road
            world.npcTraffic.forEach { car ->
                drawNpcCar(car)
            }

            // Draw Trolley
            drawTrolleyTopDown(world.trolley)

            // Draw all player vehicles (inactive first, active on top)
            world.vehicles.values
                .filter { it.type != world.activeVehicle }
                .forEach { v ->
                    drawVehicleTopDown(v, isActive = false, isNight = isNight, hornFlash = 0f)
                }
            activeVehicle?.let { v ->
                drawVehicleTopDown(v, isActive = true, isNight = isNight, hornFlash = world.hornFlash)
            }

            // Draw trees on top for depth
            drawVillageTrees(world.trees)

            // Draw floating feedback popups
            world.popups.forEach { pop ->
                val alpha = (pop.life / 1.6f).coerceIn(0f, 1f)
                popupPaint.color = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    (pop.color.red * 255).toInt(),
                    (pop.color.green * 255).toInt(),
                    (pop.color.blue * 255).toInt()
                )
                drawContext.canvas.nativeCanvas.drawText(pop.text, pop.x, pop.y, popupPaint)
            }
        }

        // Rain overlay
        if (world.isRaining) {
            world.rainDrops.forEach { drop ->
                drawLine(
                    color = Color(0x99BAE6FD),
                    start = Offset(drop.x, drop.y),
                    end = Offset(drop.x - 8f, drop.y + 24f),
                    strokeWidth = 2.2f
                )
            }
        }

        // Day / Sunset / Night atmospheric tint
        val tintColor = when {
            world.timeOfDay > 0.80f || world.timeOfDay < 0.12f -> Color(0x450B132B) // Night
            world.timeOfDay in 0.66f..0.80f -> Color(0x28E07A5F) // Sunset
            world.timeOfDay in 0.12f..0.24f -> Color(0x1FF4A261) // Sunrise
            else -> Color.Transparent
        }
        if (tintColor != Color.Transparent) {
            drawRect(tintColor)
        }

        // Corner GPS Minimap
        drawMinimap(world, viewW)
    }
}

private fun DrawScope.drawTerrainAndRoads(world: GameWorldState) {
    // Main map grass
    drawRect(Color(0xFF58934D), Offset(0f, 0f), Size(WORLD_WIDTH, WORLD_HEIGHT))

    // Decorative grass texture bands
    for (gx in 120 until WORLD_WIDTH.toInt() step 360) {
        for (gy in 140 until WORLD_HEIGHT.toInt() step 340) {
            drawCircle(Color(0xFF4F8745), 65f, Offset(gx.toFloat(), gy.toFloat()))
        }
    }

    // Irrigation Canal (नहर) running vertically at X = 1810..1875
    drawRect(Color(0xFF219EBC), Offset(1805f, 0f), Size(75f, WORLD_HEIGHT))
    drawRect(Color(0xFF8ECAE6).copy(alpha = 0.45f), Offset(1822f, 0f), Size(18f, WORLD_HEIGHT))

    // Dirt village farm tracks (कच्ची सड़कें)
    val dirtColor = Color(0xFFC29B61)
    // Upper village lane connecting Home -> Shed -> Thresher -> Pump -> Market
    drawRoundRect(dirtColor, Offset(150f, 445f), Size(2240f, 88f), CornerRadius(12f, 12f))
    // Vertical connector roads
    drawRoundRect(dirtColor, Offset(520f, 445f), Size(85f, 1340f), CornerRadius(12f, 12f))
    drawRoundRect(dirtColor, Offset(1220f, 445f), Size(85f, 1340f), CornerRadius(12f, 12f))
    // Lower field access road
    drawRoundRect(dirtColor, Offset(210f, 990f), Size(1560f, 70f), CornerRadius(10f, 10f))
    drawRoundRect(dirtColor, Offset(210f, 1380f), Size(1560f, 65f), CornerRadius(10f, 10f))
    // Village homes lane
    drawRoundRect(dirtColor, Offset(1880f, 990f), Size(540f, 72f), CornerRadius(10f, 10f))

    // Main Paved Asphalt Highway (पक्की मुख्य सड़क) across Y = 720..845
    drawRect(Color(0xFF3D405B), Offset(0f, 720f), Size(WORLD_WIDTH, 125f))
    // Road shoulders
    drawRect(Color(0xFFE9C46A), Offset(0f, 718f), Size(WORLD_WIDTH, 4f))
    drawRect(Color(0xFFE9C46A), Offset(0f, 843f), Size(WORLD_WIDTH, 4f))
    // Dashed center markings
    for (rx in 20 until WORLD_WIDTH.toInt() step 75) {
        drawRect(Color.White, Offset(rx.toFloat(), 780f), Size(42f, 5f))
    }

    // Bridges over irrigation canal (at highway & upper lane)
    drawRect(Color(0xFF9CA3AF), Offset(1795f, 714f), Size(95f, 137f))
    drawRect(Color(0xFF6B7280), Offset(1795f, 712f), Size(95f, 8f))
    drawRect(Color(0xFF6B7280), Offset(1795f, 841f), Size(95f, 8f))

    drawRect(Color(0xFFB08968), Offset(1795f, 440f), Size(95f, 98f))
    drawRect(Color(0xFF7F5539), Offset(1795f, 438f), Size(95f, 7f))
    drawRect(Color(0xFF7F5539), Offset(1795f, 533f), Size(95f, 7f))
}

private fun DrawScope.drawFarmFields(world: GameWorldState, labelPaint: Paint) {
    world.fields.forEach { field ->
        val w = field.cols * field.tileSize
        val h = field.rows * field.tileSize
        // Field border / bund (मेड़)
        drawRoundRect(
            color = Color(0xFF6C584C),
            topLeft = Offset(field.left - 8f, field.top - 8f),
            size = Size(w + 16f, h + 16f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        field.tiles.forEachIndexed { idx, tile ->
            val col = idx % field.cols
            val row = idx / field.cols
            val tx = field.left + col * field.tileSize
            val ty = field.top + row * field.tileSize
            val ts = field.tileSize

            val soilColor = when (tile.stage) {
                SoilStage.ROUGH -> Color(0xFF8D7B56)
                SoilStage.UNTILLED -> Color(0xFFA98467)
                SoilStage.PLOWED, SoilStage.SOWN, SoilStage.GROWING, SoilStage.RIPE -> Color(0xFF5C3D2E)
            }
            drawRect(soilColor, Offset(tx + 1f, ty + 1f), Size(ts - 2f, ts - 2f))

            when (tile.stage) {
                SoilStage.ROUGH -> {
                    // Bushes and stones to clear with JCB/Bulldozer
                    drawCircle(Color(0xFF386641), 16f, Offset(tx + 28f, ty + 30f))
                    drawCircle(Color(0xFF6A994E), 12f, Offset(tx + 56f, ty + 55f))
                    drawCircle(Color(0xFF6C757D), 10f, Offset(tx + 58f, ty + 24f))
                }
                SoilStage.PLOWED -> {
                    for (line in 12..72 step 15) {
                        drawLine(
                            color = Color(0xFF3E2723),
                            start = Offset(tx + 6f, ty + line),
                            end = Offset(tx + ts - 6f, ty + line),
                            strokeWidth = 3f
                        )
                    }
                }
                SoilStage.SOWN -> {
                    for (sx in 16..70 step 18) {
                        for (sy in 18..68 step 22) {
                            drawCircle(Color(0xFF95D5B2), 4f, Offset(tx + sx, ty + sy))
                        }
                    }
                }
                SoilStage.GROWING -> {
                    val scale = (tile.growth / 100f).coerceIn(0.35f, 0.95f)
                    for (sx in 16..72 step 18) {
                        for (sy in 18..70 step 22) {
                            drawCircle(tile.crop.youngColor, 9f * scale, Offset(tx + sx, ty + sy))
                        }
                    }
                }
                SoilStage.RIPE -> {
                    for (sx in 15..73 step 17) {
                        for (sy in 16..72 step 19) {
                            drawCircle(tile.crop.ripeColor, 9.5f, Offset(tx + sx, ty + sy))
                            drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.5f), 3.5f, Offset(tx + sx - 2f, ty + sy - 2f))
                        }
                    }
                }
                SoilStage.UNTILLED -> Unit
            }
        }

        // Field Signpost Label
        drawContext.canvas.nativeCanvas.drawText(
            field.nameHi,
            field.centerX,
            field.top - 16f,
            labelPaint
        )
    }
}

private fun DrawScope.drawVillageZones(world: GameWorldState, labelPaint: Paint) {
    world.zones.forEach { zone ->
        when (zone.type) {
            ZoneType.ANIMAL_SHED -> {
                // Fenced Dairy Pasture
                drawRoundRect(
                    color = Color(0xFF76A954),
                    topLeft = Offset(zone.x, zone.y),
                    size = Size(zone.width, zone.height),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = Color(0xFF7F5539),
                    topLeft = Offset(zone.x, zone.y),
                    size = Size(zone.width, zone.height),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 7f)
                )
                // Tin roof shed on north side
                drawRoundRect(Color(0xFF457B9D), Offset(zone.x + 20f, zone.y + 12f), Size(230f, 70f), CornerRadius(6f, 6f))
                // Haystacks & water trough
                drawCircle(Color(0xFFE9C46A), 24f, Offset(zone.x + 340f, zone.y + 52f))
                drawRoundRect(Color(0xFF4CC9F0), Offset(zone.x + 270f, zone.y + 34f), Size(42f, 24f), CornerRadius(5f, 5f))
            }
            ZoneType.THRESHER_YARD -> {
                drawRoundRect(Color(0xFFDDB892), Offset(zone.x, zone.y), Size(zone.width, zone.height), CornerRadius(14f, 14f))
                // Thresher Machine (थ्रेशर)
                drawRoundRect(Color(0xFFB5179E), Offset(zone.x + 68f, zone.y + 62f), Size(95f, 58f), CornerRadius(8f, 8f))
                drawRoundRect(Color(0xFFE9C46A), Offset(zone.x + 150f, zone.y + 74f), Size(32f, 34f), CornerRadius(4f, 4f))
                // Spinning flywheel
                withTransform({
                    translate(zone.x + 95f, zone.y + 91f)
                    rotate(world.thresherSpinning * (180f / PI.toFloat()), pivot = Offset.Zero)
                }) {
                    drawCircle(Color(0xFF222222), 20f)
                    drawLine(Color.White, Offset(-16f, 0f), Offset(16f, 0f), strokeWidth = 4f)
                    drawLine(Color.White, Offset(0f, -16f), Offset(0f, 16f), strokeWidth = 4f)
                }
                // Grain sacks
                drawCircle(Color(0xFFE9C46A), 14f, Offset(zone.x + 42f, zone.y + 145f))
                drawCircle(Color(0xFFD4A373), 14f, Offset(zone.x + 70f, zone.y + 150f))
            }
            ZoneType.PETROL_PUMP -> {
                // Concrete forecourt
                drawRoundRect(Color(0xFFCBD5E1), Offset(zone.x, zone.y), Size(zone.width, zone.height), CornerRadius(14f, 14f))
                // Glowing refueling bay
                drawRoundRect(
                    Color(0xFF22C55E).copy(alpha = 0.25f),
                    Offset(zone.x + 35f, zone.y + 130f),
                    Size(230f, 72f),
                    CornerRadius(10f, 10f)
                )
                // Iconic Canopy
                drawRoundRect(Color(0xFFDC2626), Offset(zone.x + 30f, zone.y + 24f), Size(240f, 92f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFFFACC15), Offset(zone.x + 42f, zone.y + 34f), Size(216f, 16f), CornerRadius(4f, 4f))
                // Diesel Dispensers
                drawRoundRect(Color(0xFF1E293B), Offset(zone.x + 76f, zone.y + 114f), Size(26f, 22f), CornerRadius(4f, 4f))
                drawRoundRect(Color(0xFF1E293B), Offset(zone.x + 194f, zone.y + 114f), Size(26f, 22f), CornerRadius(4f, 4f))
            }
            ZoneType.VILLAGE_MARKET -> {
                // Market paved plaza
                drawRoundRect(Color(0xFFE2D4B7), Offset(zone.x, zone.y), Size(zone.width, zone.height), CornerRadius(16f, 16f))
                // Colorful Mandi Stalls
                val stallColors = listOf(Color(0xFFEF476F), Color(0xFF06D6A0), Color(0xFF118AB2), Color(0xFFFF9F1C))
                stallColors.forEachIndexed { idx, col ->
                    val sx = zone.x + 26f + idx * 100f
                    drawRoundRect(col, Offset(sx, zone.y + 24f), Size(82f, 58f), CornerRadius(8f, 8f))
                    drawRect(Color.White.copy(alpha = 0.55f), Offset(sx + 8f, zone.y + 32f), Size(66f, 12f))
                }
                // Mandi weighing platform & grain bags
                drawRoundRect(Color(0xFF8D99AE), Offset(zone.x + 120f, zone.y + 150f), Size(180f, 75f), CornerRadius(8f, 8f))
                drawCircle(Color(0xFFE9C46A), 16f, Offset(zone.x + 56f, zone.y + 175f))
                drawCircle(Color(0xFFE9C46A), 16f, Offset(zone.x + 86f, zone.y + 185f))
            }
            else -> Unit
        }
        drawContext.canvas.nativeCanvas.drawText(
            zone.titleHi,
            zone.centerX,
            zone.y - 10f,
            labelPaint
        )
    }
}

private fun DrawScope.drawVillageHouses(houses: List<VillageHouse>, labelPaint: Paint) {
    houses.forEach { house ->
        // Drop shadow
        drawRoundRect(
            Color.Black.copy(alpha = 0.25f),
            Offset(house.x + 8f, house.y + 8f),
            Size(house.w, house.h),
            CornerRadius(10f, 10f)
        )
        // Courtyard / Walls
        drawRoundRect(house.wallColor, Offset(house.x, house.y), Size(house.w, house.h), CornerRadius(8f, 8f))
        // Terracotta Pitched Roof
        drawRoundRect(house.roofColor, Offset(house.x + 8f, house.y + 8f), Size(house.w - 16f, house.h - 16f), CornerRadius(6f, 6f))
        // Roof ridge line
        drawLine(
            color = Color(0xFF5C2415),
            start = Offset(house.x + 14f, house.y + house.h * 0.5f),
            end = Offset(house.x + house.w - 14f, house.y + house.h * 0.5f),
            strokeWidth = 5f
        )
        if (house.label.isNotEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                house.label,
                house.x + house.w * 0.5f,
                house.y + house.h + 26f,
                labelPaint
            )
        }
    }
}

private fun DrawScope.drawLivestock(animals: List<AnimalEntity>) {
    animals.forEach { a ->
        val deg = a.angleRad * (180f / PI.toFloat())
        withTransform({
            translate(a.x, a.y)
            rotate(deg, pivot = Offset.Zero)
        }) {
            when (a.kind) {
                AnimalKind.COW -> {
                    // White & brown spotted cow
                    drawRoundRect(Color(0xFFF8FAFC), Offset(-14f, -8f), Size(28f, 16f), CornerRadius(7f, 7f))
                    drawCircle(Color(0xFF8D5524), 5f, Offset(-3f, -2f))
                    drawCircle(Color(0xFF8D5524), 4f, Offset(5f, 3f))
                    drawCircle(Color(0xFFF1F5F9), 6f, Offset(14f, 0f))
                    drawLine(Color(0xFFCBD5E1), Offset(15f, -5f), Offset(18f, -8f), strokeWidth = 2.5f)
                    drawLine(Color(0xFFCBD5E1), Offset(15f, 5f), Offset(18f, 8f), strokeWidth = 2.5f)
                }
                AnimalKind.BUFFALO -> {
                    // Dark charcoal buffalo with curved horns
                    drawRoundRect(Color(0xFF1F2937), Offset(-16f, -9f), Size(32f, 18f), CornerRadius(8f, 8f))
                    drawCircle(Color(0xFF111827), 6.5f, Offset(16f, 0f))
                    drawLine(Color(0xFF9CA3AF), Offset(16f, -5f), Offset(12f, -11f), strokeWidth = 3f)
                    drawLine(Color(0xFF9CA3AF), Offset(16f, 5f), Offset(12f, 11f), strokeWidth = 3f)
                }
                AnimalKind.GOAT -> {
                    drawRoundRect(Color(0xFFD6B89C), Offset(-9f, -5f), Size(18f, 10f), CornerRadius(4f, 4f))
                    drawCircle(Color(0xFF9C7A5B), 4f, Offset(9f, 0f))
                }
                AnimalKind.HEN -> {
                    drawCircle(Color(0xFFEA580C), 5.5f, Offset.Zero)
                    drawCircle(Color(0xFFDC2626), 2.5f, Offset(5f, 0f))
                }
            }
        }
    }
}

private fun DrawScope.drawNpcCar(car: NpcTrafficCar) {
    withTransform({
        translate(car.x, car.y)
        if (car.speed < 0) rotate(180f, pivot = Offset.Zero)
    }) {
        if (car.isTruck) {
            drawRoundRect(Color(0xFF92400E), Offset(-40f, -18f), Size(54f, 36f), CornerRadius(5f, 5f))
            drawRoundRect(car.color, Offset(14f, -17f), Size(26f, 34f), CornerRadius(6f, 6f))
        } else {
            drawRoundRect(car.color, Offset(-24f, -14f), Size(48f, 28f), CornerRadius(7f, 7f))
            drawRoundRect(Color(0xFFBAE6FD), Offset(4f, -10f), Size(10f, 20f), CornerRadius(2f, 2f))
        }
    }
}

private fun DrawScope.drawVillageTrees(trees: List<VillageTree>) {
    trees.forEach { t ->
        drawCircle(Color.Black.copy(alpha = 0.20f), t.radius * 0.9f, Offset(t.x + 8f, t.y + 8f))
        if (t.isPalm) {
            for (ang in 0 until 360 step 60) {
                val rad = ang * (PI.toFloat() / 180f)
                drawLine(
                    color = Color(0xFF2D6A4F),
                    start = Offset(t.x, t.y),
                    end = Offset(t.x + cos(rad) * t.radius, t.y + sin(rad) * t.radius),
                    strokeWidth = 10f
                )
            }
            drawCircle(Color(0xFF7F5539), 7f, Offset(t.x, t.y))
        } else {
            drawCircle(Color(0xFF1B4332), t.radius, Offset(t.x, t.y))
            drawCircle(Color(0xFF2D6A4F), t.radius * 0.72f, Offset(t.x - 6f, t.y - 6f))
            drawCircle(Color(0xFF40916C), t.radius * 0.42f, Offset(t.x - 9f, t.y - 10f))
        }
    }
}

private fun DrawScope.drawMinimap(world: GameWorldState, viewW: Float) {
    val mapW = 148f
    val mapH = 108f
    val mx = viewW - mapW - 18f
    val my = 128f
    val scaleX = mapW / WORLD_WIDTH
    val scaleY = mapH / WORLD_HEIGHT

    drawRoundRect(
        color = Color(0xCC111827),
        topLeft = Offset(mx, my),
        size = Size(mapW, mapH),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = Color(0x88E9C46A),
        topLeft = Offset(mx, my),
        size = Size(mapW, mapH),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 2f)
    )
    // Highway line
    drawRect(Color(0xFF6B7280), Offset(mx, my + 780f * scaleY), Size(mapW, 6f))
    // Fields on minimap
    world.fields.forEach { f ->
        val c = when (f.dominantStage()) {
            SoilStage.RIPE -> Color(0xFFE9C46A)
            SoilStage.GROWING, SoilStage.SOWN -> Color(0xFF52B788)
            SoilStage.ROUGH -> Color(0xFF9C6644)
            else -> Color(0xFF7F5539)
        }
        drawRect(
            color = c,
            topLeft = Offset(mx + f.left * scaleX, my + f.top * scaleY),
            size = Size(f.cols * f.tileSize * scaleX, f.rows * f.tileSize * scaleY)
        )
    }
    // Zones on minimap
    world.zones.forEach { z ->
        val zc = when (z.type) {
            ZoneType.PETROL_PUMP -> Color(0xFFEF4444)
            ZoneType.VILLAGE_MARKET -> Color(0xFFF59E0B)
            ZoneType.ANIMAL_SHED -> Color(0xFF10B981)
            else -> Color(0xFF93C5FD)
        }
        drawCircle(zc, 4f, Offset(mx + z.centerX * scaleX, my + z.centerY * scaleY))
    }
    // Active vehicle blinking dot
    val active = world.vehicles[world.activeVehicle]
    if (active != null) {
        drawCircle(Color.White, 5.5f, Offset(mx + active.x * scaleX, my + active.y * scaleY))
        drawCircle(Color(0xFFD62828), 3.8f, Offset(mx + active.x * scaleX, my + active.y * scaleY))
    }
}
