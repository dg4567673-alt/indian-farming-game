package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.FarmSave
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameEngine {
    private var nextPopupId = 1L
    private var nextAnimalId = 1

    fun createInitialWorld(save: FarmSave?): GameWorldState {
        val vehicles = mapOf(
            VehicleType.TRACTOR to VehicleEntity(
                type = VehicleType.TRACTOR,
                x = 640f,
                y = 480f,
                angleRad = 0f,
                fuel = (save?.fuelReserve ?: 90L).toFloat().coerceIn(20f, 100f)
            ),
            VehicleType.HARVESTER to VehicleEntity(
                type = VehicleType.HARVESTER,
                x = 460f,
                y = 610f,
                angleRad = 0f,
                fuel = 88f
            ),
            VehicleType.JCB to VehicleEntity(
                type = VehicleType.JCB,
                x = 1780f,
                y = 660f,
                angleRad = PI.toFloat(),
                fuel = 92f
            ),
            VehicleType.BULLDOZER to VehicleEntity(
                type = VehicleType.BULLDOZER,
                x = 1920f,
                y = 660f,
                angleRad = PI.toFloat(),
                fuel = 90f
            ),
            VehicleType.TRUCK to VehicleEntity(
                type = VehicleType.TRUCK,
                x = 1120f,
                y = 910f,
                angleRad = 0f,
                fuel = 95f
            ),
            VehicleType.PICKUP to VehicleEntity(
                type = VehicleType.PICKUP,
                x = 390f,
                y = 450f,
                angleRad = 0.2f,
                fuel = 94f
            )
        )

        val savedStates = save?.fieldStates.orEmpty()
        val fields = listOf(
            buildField(0, "खेत 1 (गेहूँ)", 240f, 1080f, savedStates.getOrNull(0) ?: "WHEAT:RIPE:100"),
            buildField(1, "खेत 2 (धान)", 780f, 1080f, savedStates.getOrNull(1) ?: "RICE:GROWING:65"),
            buildField(2, "खेत 3 (गन्ना)", 1320f, 1080f, savedStates.getOrNull(2) ?: "SUGARCANE:PLOWED:0"),
            buildField(3, "खेत 4 (सरसों)", 240f, 1460f, savedStates.getOrNull(3) ?: "MUSTARD:UNTILLED:0"),
            buildField(4, "खेत 5 (बंजर भूमि)", 780f, 1460f, savedStates.getOrNull(4) ?: "EMPTY:ROUGH:0"),
            buildField(5, "खेत 6 (जंगल प्लॉट)", 1320f, 1460f, savedStates.getOrNull(5) ?: "EMPTY:ROUGH:0")
        )

        val zones = listOf(
            WorldZone(ZoneType.SHUBHAM_HOME, "शुभम का घर 🏡", 180f, 200f, 250f, 200f),
            WorldZone(ZoneType.ANIMAL_SHED, "पशु बाड़ा 🐄", 680f, 180f, 410f, 250f),
            WorldZone(ZoneType.THRESHER_YARD, "थ्रेशर अड्डा ⚙️", 1180f, 210f, 240f, 190f),
            WorldZone(ZoneType.PETROL_PUMP, "पेट्रोल पंप ⛽", 1540f, 210f, 300f, 220f),
            WorldZone(ZoneType.VILLAGE_MARKET, "गाँव का बाजार 🏪", 1930f, 200f, 440f, 270f),
            WorldZone(ZoneType.VILLAGE_HOUSES, "गाँव बस्ती 🏘️", 1920f, 1080f, 520f, 650f)
        )

        val houses = listOf(
            VillageHouse(200f, 220f, 180f, 130f, Color(0xFFB54426), Color(0xFFF3E5AB), "शुभम निवास"),
            VillageHouse(1980f, 1110f, 145f, 110f, Color(0xFFC85A32), Color(0xFFEAD7B7), "रामू काका घर"),
            VillageHouse(2200f, 1130f, 155f, 115f, Color(0xFF9E3B24), Color(0xFFF5E6CA), "चौपाल घर"),
            VillageHouse(1970f, 1340f, 160f, 115f, Color(0xFFB85D3B), Color(0xFFE8D5B7), "पंचायत भवन"),
            VillageHouse(2210f, 1370f, 140f, 105f, Color(0xFF8D4028), Color(0xFFF2E2C4), "शर्मा घर"),
            VillageHouse(2020f, 1570f, 150f, 110f, Color(0xFFA94A2A), Color(0xFFEFE1C6), "गोदाम")
        )

        val trees = listOf(
            VillageTree(110f, 150f, 44f),
            VillageTree(470f, 170f, 40f),
            VillageTree(560f, 340f, 36f),
            VillageTree(1120f, 140f, 42f),
            VillageTree(1450f, 150f, 38f, isPalm = true),
            VillageTree(1870f, 140f, 42f),
            VillageTree(2440f, 220f, 46f),
            VillageTree(120f, 960f, 42f),
            VillageTree(130f, 1380f, 48f),
            VillageTree(130f, 1740f, 44f, isPalm = true),
            VillageTree(1840f, 1020f, 38f),
            VillageTree(2430f, 1270f, 44f),
            VillageTree(2440f, 1650f, 46f, isPalm = true),
            VillageTree(1260f, 1790f, 40f),
            VillageTree(720f, 1790f, 42f)
        )

        val animals = buildAnimals(
            cows = (save?.cowCount ?: 2L).toInt(),
            buffaloes = (save?.buffaloCount ?: 2L).toInt(),
            goats = (save?.goatCount ?: 3L).toInt(),
            hens = (save?.henCount ?: 4L).toInt()
        )

        val traffic = listOf(
            NpcTrafficCar(x = 300f, y = 805f, speed = 115f, color = Color(0xFF0284C7), isTruck = true),
            NpcTrafficCar(x = 1700f, y = 755f, speed = -125f, color = Color(0xFFE11D48), isTruck = false)
        )

        val activeType = VehicleType.fromId(save?.activeVehicleId ?: "tractor")

        return GameWorldState(
            vehicles = vehicles,
            activeVehicle = activeType,
            trolley = TrolleyEntity(x = 550f, y = 480f, angleRad = 0f, attachedToTractor = true),
            fields = fields,
            zones = zones,
            animals = animals,
            trees = trees,
            houses = houses,
            npcTraffic = traffic
        )
    }

    private fun buildField(index: Int, nameHi: String, left: Float, top: Float, encoded: String): FarmField {
        val parts = encoded.split(":")
        val crop = CropType.fromId(parts.getOrNull(0) ?: "WHEAT")
        val stage = SoilStage.fromId(parts.getOrNull(1) ?: "UNTILLED")
        val growth = (parts.getOrNull(2)?.toFloatOrNull() ?: 0f).coerceIn(0f, 100f)
        val cols = 5
        val rows = 4
        val tileSize = 86f
        val tiles = List(cols * rows) {
            FieldTile(stage = stage, crop = crop, growth = growth)
        }
        return FarmField(
            index = index,
            nameHi = nameHi,
            left = left,
            top = top,
            cols = cols,
            rows = rows,
            tileSize = tileSize,
            tiles = tiles
        )
    }

    fun buildAnimals(cows: Int, buffaloes: Int, goats: Int, hens: Int): List<AnimalEntity> {
        val list = mutableListOf<AnimalEntity>()
        nextAnimalId = 1
        fun addBatch(kind: AnimalKind, count: Int) {
            repeat(count.coerceAtMost(12)) {
                val ax = 710f + Random.nextFloat() * 340f
                val ay = 225f + Random.nextFloat() * 175f
                list.add(
                    AnimalEntity(
                        id = nextAnimalId++,
                        kind = kind,
                        x = ax,
                        y = ay,
                        targetX = 710f + Random.nextFloat() * 340f,
                        targetY = 225f + Random.nextFloat() * 175f,
                        angleRad = Random.nextFloat() * 6.28f
                    )
                )
            }
        }
        addBatch(AnimalKind.COW, cows)
        addBatch(AnimalKind.BUFFALO, buffaloes)
        addBatch(AnimalKind.GOAT, goats)
        addBatch(AnimalKind.HEN, hens)
        return list
    }

    fun step(
        state: GameWorldState,
        dtSeconds: Float,
        joyX: Float,
        joyY: Float,
        throttle: Float,
        reverse: Float,
        save: FarmSave
    ): Pair<GameWorldState, FarmSave?> {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)
        var updatedSave: FarmSave? = null
        val activeType = state.activeVehicle
        val currentVehicle = state.vehicles[activeType] ?: return state to null

        // Steering and throttle calculation
        val joyMag = hypot(joyX, joyY)
        val inputAccel = when {
            throttle > 0.1f -> throttle
            reverse > 0.1f -> -reverse * 0.7f
            joyMag > 0.25f -> joyMag.coerceIn(0f, 1f)
            else -> 0f
        }

        val hasFuel = currentVehicle.fuel > 0.2f
        val effectiveAccel = if (hasFuel) inputAccel else 0f

        var newSpeed = currentVehicle.speed
        if (abs(effectiveAccel) > 0.05f) {
            newSpeed += effectiveAccel * activeType.acceleration * dt
        } else {
            val drag = 260f * dt
            newSpeed = when {
                newSpeed > drag -> newSpeed - drag
                newSpeed < -drag -> newSpeed + drag
                else -> 0f
            }
        }
        newSpeed = newSpeed.coerceIn(-activeType.maxSpeed * 0.45f, activeType.maxSpeed)

        var newAngle = currentVehicle.angleRad
        var steerDelta = 0f
        if (joyMag > 0.2f) {
            if (throttle > 0.1f || reverse > 0.1f) {
                // Joystick steers left/right relative to screen OR absolute direction when driving with pedals
                steerDelta = joyX.coerceIn(-1f, 1f)
                if (abs(newSpeed) > 12f) {
                    val dirSign = if (newSpeed >= 0f) 1f else -1f
                    newAngle += steerDelta * activeType.turnSpeed * dirSign * dt
                }
            } else {
                // One-handed joystick driving: steer smoothly toward joystick angle
                val targetAngle = atan2(joyY, joyX)
                var diff = targetAngle - newAngle
                while (diff > PI) diff -= (2f * PI).toFloat()
                while (diff < -PI) diff += (2f * PI).toFloat()
                val turnStep = activeType.turnSpeed * 1.45f * dt
                newAngle += diff.coerceIn(-turnStep, turnStep)
                steerDelta = (diff / PI.toFloat()).coerceIn(-1f, 1f)
            }
        }

        var newX = (currentVehicle.x + cos(newAngle) * newSpeed * dt).coerceIn(70f, WORLD_WIDTH - 70f)
        var newY = (currentVehicle.y + sin(newAngle) * newSpeed * dt).coerceIn(70f, WORLD_HEIGHT - 70f)

        // Fuel consumption
        val fuelUsed = (abs(newSpeed) / activeType.maxSpeed) * activeType.fuelRate * dt
        val newFuel = (currentVehicle.fuel - fuelUsed).coerceIn(0f, 100f)

        // Animate JCB/Harvester/Bulldozer moving parts
        val newBoomPhase = if (abs(newSpeed) > 10f || state.actionCooldown > 0f) {
            (currentVehicle.boomPhase + dt * 5f) % (2f * PI.toFloat())
        } else {
            currentVehicle.boomPhase
        }

        val updatedVehicle = currentVehicle.copy(
            x = newX,
            y = newY,
            angleRad = newAngle,
            speed = newSpeed,
            steerInput = steerDelta,
            fuel = newFuel,
            boomPhase = newBoomPhase
        )

        // Articulated Trolley towing physics when Tractor is pulling it
        var trolley = state.trolley
        if (trolley.attachedToTractor) {
            val tractor = if (activeType == VehicleType.TRACTOR) updatedVehicle else state.vehicles[VehicleType.TRACTOR]
            if (tractor != null) {
                val hitchX = tractor.x - cos(tractor.angleRad) * 44f
                val hitchY = tractor.y - sin(tractor.angleRad) * 44f
                val dx = hitchX - trolley.x
                val dy = hitchY - trolley.y
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val targetAngle = atan2(dy, dx)
                val desiredDist = 50f
                val tx = hitchX - cos(targetAngle) * desiredDist
                val ty = hitchY - sin(targetAngle) * desiredDist
                trolley = trolley.copy(x = tx, y = ty, angleRad = targetAngle)
            }
        }

        // Update particles
        val particles = state.particles.mapNotNull { p ->
            val rem = p.life - dt
            if (rem <= 0f) null
            else p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt, life = rem)
        }.toMutableList()

        if (abs(newSpeed) > 55f && particles.size < 65 && Random.nextFloat() < 0.45f) {
            val dustColor = if (newY > 1020f) Color(0xFFB08968) else Color(0xFFD6CCC2)
            particles.add(
                DustParticle(
                    x = newX - cos(newAngle) * 32f + (Random.nextFloat() - 0.5f) * 16f,
                    y = newY - sin(newAngle) * 32f + (Random.nextFloat() - 0.5f) * 16f,
                    vx = (Random.nextFloat() - 0.5f) * 20f,
                    vy = (Random.nextFloat() - 0.5f) * 20f,
                    life = 0.55f,
                    maxLife = 0.55f,
                    radius = 5f + Random.nextFloat() * 5f,
                    color = dustColor
                )
            )
        }

        // Update popups
        val popups = state.popups.mapNotNull { pop ->
            val rem = pop.life - dt
            if (rem <= 0f) null
            else pop.copy(y = pop.y - 28f * dt, life = rem)
        }.toMutableList()

        // Update crop growth in fields & automatic vehicle field interaction while driving!
        var missionPlowed = state.missionPlowedTiles
        var missionHarvested = state.missionHarvestedTiles
        var missionCleared = state.missionClearedTiles
        var workingSave = save
        var saveChanged = false

        val growthMultiplier = if (state.isRaining) 1.85f else 1.0f
        val newFields = state.fields.map { field ->
            var fieldModified = false
            val newTiles = field.tiles.mapIndexed { idx, tile ->
                var cur = tile
                // Natural crop growth
                if (cur.stage == SoilStage.SOWN || cur.stage == SoilStage.GROWING) {
                    val growStep = (100f / cur.crop.growSeconds) * growthMultiplier * dt
                    val nextG = (cur.growth + growStep).coerceAtMost(100f)
                    val nextStage = when {
                        nextG >= 100f -> SoilStage.RIPE
                        nextG >= 30f -> SoilStage.GROWING
                        else -> SoilStage.SOWN
                    }
                    if (nextStage != cur.stage) fieldModified = true
                    cur = cur.copy(growth = nextG, stage = nextStage)
                }

                // Check if active vehicle is driving over this tile
                val col = idx % field.cols
                val row = idx / field.cols
                val tileCenterX = field.left + (col + 0.5f) * field.tileSize
                val tileCenterY = field.top + (row + 0.5f) * field.tileSize
                val distToTile = hypot(newX - tileCenterX, newY - tileCenterY)

                if (distToTile < field.tileSize * 0.62f && abs(newSpeed) > 15f) {
                    when (activeType) {
                        VehicleType.JCB, VehicleType.BULLDOZER -> {
                            if (cur.stage == SoilStage.ROUGH) {
                                cur = cur.copy(stage = SoilStage.UNTILLED, crop = CropType.EMPTY, growth = 0f)
                                missionCleared++
                                fieldModified = true
                                popups.add(FloatingPopup(nextPopupId++, tileCenterX, tileCenterY, "साफ किया! 🏗️", Color(0xFFFFB703)))
                                particles.add(DustParticle(tileCenterX, tileCenterY, 0f, -20f, 0.7f, 0.7f, 10f, Color(0xFF9C6644)))
                            }
                        }
                        VehicleType.TRACTOR -> {
                            if (cur.stage == SoilStage.UNTILLED) {
                                cur = cur.copy(stage = SoilStage.PLOWED, crop = CropType.EMPTY, growth = 0f)
                                missionPlowed++
                                fieldModified = true
                                popups.add(FloatingPopup(nextPopupId++, tileCenterX, tileCenterY, "जुताई! 🚜", Color(0xFF8D5524)))
                            } else if (cur.stage == SoilStage.PLOWED) {
                                val seed = state.selectedSeed
                                val seedCount = getSeedCount(workingSave, seed)
                                if (seedCount > 0L) {
                                    workingSave = decrementSeed(workingSave, seed)
                                    cur = cur.copy(stage = SoilStage.SOWN, crop = seed, growth = 5f)
                                    fieldModified = true
                                    saveChanged = true
                                    popups.add(FloatingPopup(nextPopupId++, tileCenterX, tileCenterY, "${seed.titleHi} बोया 🌱", Color(0xFF2D6A4F)))
                                }
                            }
                        }
                        VehicleType.HARVESTER -> {
                            if (cur.stage == SoilStage.RIPE && cur.crop != CropType.EMPTY) {
                                val harvestedCrop = cur.crop
                                workingSave = incrementCropStock(workingSave, harvestedCrop, 1L).copy(
                                    totalHarvested = workingSave.totalHarvested + 1L,
                                    xp = workingSave.xp + 12L
                                )
                                if (trolley.attachedToTractor && trolley.load < trolley.maxLoad) {
                                    trolley = trolley.copy(load = trolley.load + 1, cropType = harvestedCrop)
                                }
                                cur = cur.copy(stage = SoilStage.UNTILLED, crop = CropType.EMPTY, growth = 0f)
                                missionHarvested++
                                fieldModified = true
                                saveChanged = true
                                popups.add(FloatingPopup(nextPopupId++, tileCenterX, tileCenterY, "+1 ${harvestedCrop.titleHi} 🌾", Color(0xFFE9C46A)))
                                particles.add(DustParticle(tileCenterX, tileCenterY, 15f, -25f, 0.7f, 0.7f, 9f, Color(0xFFE9C46A)))
                            }
                        }
                        else -> Unit
                    }
                }
                cur
            }
            if (fieldModified) field.copy(tiles = newTiles) else field
        }

        if (saveChanged) {
            val encodedFields = newFields.map { it.encodeSummary() }
            workingSave = workingSave.copy(fieldStates = encodedFields)
            updatedSave = workingSave
        }

        // Update Animals wandering gently inside पशु बाड़ा
        val animals = state.animals.map { a ->
            val dx = a.targetX - a.x
            val dy = a.targetY - a.y
            val dist = hypot(dx, dy)
            if (dist < 10f) {
                a.copy(
                    targetX = 705f + Random.nextFloat() * 355f,
                    targetY = 220f + Random.nextFloat() * 185f
                )
            } else {
                val spd = if (a.kind == AnimalKind.HEN) 28f else 18f
                val ang = atan2(dy, dx)
                a.copy(
                    x = a.x + cos(ang) * spd * dt,
                    y = a.y + sin(ang) * spd * dt,
                    angleRad = ang,
                    walkPhase = a.walkPhase + dt * 6f
                )
            }
        }

        // Update ambient NPC Village Road Traffic (along Y ~ 755 and 805)
        val npcTraffic = state.npcTraffic.map { car ->
            var nx = car.x + car.speed * dt
            if (nx > WORLD_WIDTH + 140f) nx = -140f
            if (nx < -140f) nx = WORLD_WIDTH + 140f
            car.copy(x = nx)
        }

        // Update Day/Night cycle & occasional rain
        val nextTime = (state.timeOfDay + dt * 0.0045f) % 1.0f
        val rainDrops = if (state.isRaining) {
            val list = state.rainDrops.map { r ->
                var ry = r.y + r.speed * dt
                var rx = r.x - 45f * dt
                if (ry > 900f) {
                    ry = -20f
                    rx = Random.nextFloat() * 1400f
                }
                r.copy(x = rx, y = ry)
            }
            if (list.isEmpty()) List(45) { RainDrop(Random.nextFloat() * 1400f, Random.nextFloat() * 900f, 520f + Random.nextFloat() * 180f) }
            else list
        } else emptyList()

        // Proximity detection for context action button
        val nearbyZone = state.zones.firstOrNull { z ->
            newX >= z.x - 45f && newX <= z.x + z.width + 45f &&
                newY >= z.y - 45f && newY <= z.y + z.height + 45f
        }?.type

        val nearbyField = state.fields.firstOrNull { f ->
            newX >= f.left - 35f && newX <= f.right + 35f &&
                newY >= f.top - 35f && newY <= f.bottom + 35f
        }

        val nearTrolley = activeType == VehicleType.TRACTOR &&
            hypot(newX - trolley.x, newY - trolley.y) < 135f

        val availableAction = resolveContextAction(
            activeType = activeType,
            nearbyZone = nearbyZone,
            nearbyField = nearbyField,
            nearTrolley = nearTrolley,
            trolleyAttached = trolley.attachedToTractor
        )

        val newVehicles = state.vehicles.toMutableMap().apply {
            put(activeType, updatedVehicle)
        }

        val speedKmh = (abs(newSpeed) * 0.16f).toInt()
        val newCooldown = (state.actionCooldown - dt).coerceAtLeast(0f)
        val newHorn = (state.hornFlash - dt).coerceAtLeast(0f)

        val newState = state.copy(
            vehicles = newVehicles,
            trolley = trolley,
            fields = newFields,
            animals = animals,
            npcTraffic = npcTraffic,
            particles = particles,
            popups = popups,
            rainDrops = rainDrops,
            thresherSpinning = (state.thresherSpinning + dt * 4.5f) % (2f * PI.toFloat()),
            timeOfDay = nextTime,
            hornFlash = newHorn,
            actionCooldown = newCooldown,
            nearbyZone = nearbyZone,
            nearbyFieldIndex = nearbyField?.index,
            nearTrolley = nearTrolley,
            availableAction = availableAction,
            speedKmh = speedKmh,
            missionPlowedTiles = missionPlowed,
            missionHarvestedTiles = missionHarvested,
            missionClearedTiles = missionCleared
        )
        return newState to updatedSave
    }

    private fun resolveContextAction(
        activeType: VehicleType,
        nearbyZone: ZoneType?,
        nearbyField: FarmField?,
        nearTrolley: Boolean,
        trolleyAttached: Boolean
    ): ContextActionType {
        return when (nearbyZone) {
            ZoneType.PETROL_PUMP -> ContextActionType.REFUEL_PUMP
            ZoneType.VILLAGE_MARKET -> ContextActionType.OPEN_MARKET
            ZoneType.ANIMAL_SHED -> ContextActionType.TEND_ANIMALS
            ZoneType.THRESHER_YARD -> ContextActionType.THRESH_GRAIN
            else -> {
                if (nearbyField != null) {
                    val stage = nearbyField.dominantStage()
                    when {
                        stage == SoilStage.ROUGH && (activeType == VehicleType.JCB || activeType == VehicleType.BULLDOZER) ->
                            ContextActionType.CLEAR_LAND
                        stage == SoilStage.UNTILLED && activeType == VehicleType.TRACTOR ->
                            ContextActionType.PLOW
                        stage == SoilStage.PLOWED && activeType == VehicleType.TRACTOR ->
                            ContextActionType.SOW
                        stage == SoilStage.SOWN || stage == SoilStage.GROWING ->
                            ContextActionType.WATER
                        stage == SoilStage.RIPE && activeType == VehicleType.HARVESTER ->
                            ContextActionType.HARVEST
                        nearTrolley && activeType == VehicleType.TRACTOR ->
                            if (trolleyAttached) ContextActionType.UNHITCH_TROLLEY else ContextActionType.HITCH_TROLLEY
                        else -> ContextActionType.NONE
                    }
                } else if (nearTrolley && activeType == VehicleType.TRACTOR) {
                    if (trolleyAttached) ContextActionType.UNHITCH_TROLLEY else ContextActionType.HITCH_TROLLEY
                } else {
                    ContextActionType.NONE
                }
            }
        }
    }

    fun getSeedCount(save: FarmSave, crop: CropType): Long = when (crop) {
        CropType.WHEAT -> save.wheatSeeds
        CropType.RICE -> save.riceSeeds
        CropType.SUGARCANE -> save.sugarcaneSeeds
        CropType.MUSTARD -> save.mustardSeeds
        CropType.EMPTY -> 0L
    }

    fun decrementSeed(save: FarmSave, crop: CropType): FarmSave = when (crop) {
        CropType.WHEAT -> save.copy(wheatSeeds = (save.wheatSeeds - 1L).coerceAtLeast(0L))
        CropType.RICE -> save.copy(riceSeeds = (save.riceSeeds - 1L).coerceAtLeast(0L))
        CropType.SUGARCANE -> save.copy(sugarcaneSeeds = (save.sugarcaneSeeds - 1L).coerceAtLeast(0L))
        CropType.MUSTARD -> save.copy(mustardSeeds = (save.mustardSeeds - 1L).coerceAtLeast(0L))
        CropType.EMPTY -> save
    }

    fun incrementCropStock(save: FarmSave, crop: CropType, amount: Long): FarmSave = when (crop) {
        CropType.WHEAT -> save.copy(wheatStock = save.wheatStock + amount)
        CropType.RICE -> save.copy(riceStock = save.riceStock + amount)
        CropType.SUGARCANE -> save.copy(sugarcaneStock = save.sugarcaneStock + amount)
        CropType.MUSTARD -> save.copy(mustardStock = save.mustardStock + amount)
        CropType.EMPTY -> save
    }

    fun buildMissions(state: GameWorldState, save: FarmSave): List<FarmMission> {
        return listOf(
            FarmMission(
                id = "m_plow",
                titleHi = "ट्रैक्टर से खेत जुताई",
                descriptionHi = "ट्रैक्टर से 8 खेत टाइल जोतें",
                targetCount = 8,
                currentCount = state.missionPlowedTiles.coerceAtMost(8),
                rewardCoins = 600L,
                claimed = state.claimedMissions.contains("m_plow")
            ),
            FarmMission(
                id = "m_harvest",
                titleHi = "हार्वेस्टर से कटाई",
                descriptionHi = "पकी हुई फसल के 10 टाइल काटें",
                targetCount = 10,
                currentCount = state.missionHarvestedTiles.coerceAtMost(10),
                rewardCoins = 950L,
                claimed = state.claimedMissions.contains("m_harvest")
            ),
            FarmMission(
                id = "m_jcb",
                titleHi = "JCB / बुलडोजर से जमीन सफाई",
                descriptionHi = "बंजर भूमि के 8 हिस्से साफ करें",
                targetCount = 8,
                currentCount = state.missionClearedTiles.coerceAtMost(8),
                rewardCoins = 850L,
                claimed = state.claimedMissions.contains("m_jcb")
            ),
            FarmMission(
                id = "m_animals",
                titleHi = "पशु सेवा व डेयरी",
                descriptionHi = "पशु बाड़े में गाय-भैंस को चारा दें (3 बार)",
                targetCount = 3,
                currentCount = state.missionFedAnimals.coerceAtMost(3),
                rewardCoins = 500L,
                claimed = state.claimedMissions.contains("m_animals")
            ),
            FarmMission(
                id = "m_market",
                titleHi = "मंडी में बिक्री",
                descriptionHi = "गाँव के बाजार में फसल या दूध बेचें",
                targetCount = 1,
                currentCount = if (state.missionSoldValue > 0 || save.missionsCompleted > 0) 1 else 0,
                rewardCoins = 750L,
                claimed = state.claimedMissions.contains("m_market")
            )
        )
    }
}
