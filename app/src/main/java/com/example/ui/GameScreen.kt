package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.signOut
import com.example.data.FarmSave
import com.example.game.ContextActionType
import com.example.game.CropType
import com.example.game.FarmingWorldCanvas
import com.example.game.VehicleType
import com.example.game.ZoneType
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.MustardAmber
import com.example.ui.theme.TerracottaWarm
import com.example.ui.theme.TractorCrimson
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun ShubhamFarmingGameScreen(
    viewModel: GameViewModel,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val farmState by viewModel.farmUiState.collectAsStateWithLifecycle()
    val world by viewModel.worldState.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val marketLogsState by viewModel.marketLogsState.collectAsStateWithLifecycle()

    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var gasPressed by remember { mutableStateOf(false) }
    var revPressed by remember { mutableStateOf(false) }

    fun pushControls(jx: Float = joyX, jy: Float = joyY, gas: Boolean = gasPressed, rev: Boolean = revPressed) {
        viewModel.setControls(jx, jy, if (gas) 1f else 0f, if (rev) 1f else 0f)
    }

    when (val state = farmState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ForestGreen),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = HarvestGold)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Shubham Farming Simulator लोड हो रहा है...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { signOut(credentialManager, scope, onSignedOut) },
                        colors = ButtonDefaults.buttonColors(containerColor = TractorCrimson)
                    ) {
                        Text("साइन आउट करें")
                    }
                }
            }
        }
        is UiState.Success -> {
            val save: FarmSave = state.data
            val activeVehicle = world.vehicles[world.activeVehicle]

            Box(modifier = Modifier.fillMaxSize()) {
                // Full-screen 60fps 2D Village World Canvas
                FarmingWorldCanvas(
                    world = world,
                    modifier = Modifier.fillMaxSize()
                )

                // Top HUD Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xCC111827),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(color = ForestGreen, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = "₹${save.coins}",
                                        color = HarvestGold,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Lv.${save.level}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                val fuelPct = activeVehicle?.fuel?.toInt() ?: 100
                                Text(
                                    text = "⛽$fuelPct%",
                                    color = if (fuelPct < 25) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "${world.speedKmh} km/h",
                                    color = Color(0xFF93C5FD),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.toggleRain() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (world.isRaining) Icons.Filled.WaterDrop else Icons.Filled.WbSunny,
                                        contentDescription = "Weather",
                                        tint = if (world.isRaining) Color(0xFF38BDF8) else HarvestGold
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.syncNow() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudUpload,
                                        contentDescription = "Cloud Sync",
                                        tint = Color(0xFF86EFAC)
                                    )
                                }
                                IconButton(
                                    onClick = { signOut(credentialManager, scope, onSignedOut) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Quick Village GPS / Zone Navigator Strip + Trolley status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val zoneShortcuts = listOf(
                            ZoneType.SHUBHAM_HOME to "🏡 घर",
                            ZoneType.ANIMAL_SHED to "🐄 पशु बाड़ा",
                            ZoneType.THRESHER_YARD to "⚙️ थ्रेशर",
                            ZoneType.PETROL_PUMP to "⛽ पंप",
                            ZoneType.VILLAGE_MARKET to "🏪 बाजार",
                            ZoneType.VILLAGE_HOUSES to "🏘️ गाँव"
                        )
                        zoneShortcuts.forEach { (zType, label) ->
                            Surface(
                                color = Color(0xBB1F2937),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable { viewModel.quickDriveToZone(zType) }
                                    .testTag("zone_chip_${zType.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        if (world.activeVehicle == VehicleType.TRACTOR) {
                            Surface(
                                color = if (world.trolley.attachedToTractor) TerracottaWarm else Color(0xBB374151),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable { viewModel.toggleTrolleyHitch() }
                                    .testTag("trolley_hitch_chip")
                            ) {
                                Text(
                                    text = if (world.trolley.attachedToTractor) "ट्रॉली जुड़ी (${world.trolley.load})" else "ट्रॉली जोड़ें",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Seed selection bar when driving Tractor
                    AnimatedVisibility(visible = world.activeVehicle == VehicleType.TRACTOR) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                CropType.WHEAT to save.wheatSeeds,
                                CropType.RICE to save.riceSeeds,
                                CropType.SUGARCANE to save.sugarcaneSeeds,
                                CropType.MUSTARD to save.mustardSeeds
                            ).forEach { (crop, count) ->
                                FilterChip(
                                    selected = world.selectedSeed == crop,
                                    onClick = { viewModel.selectSeed(crop) },
                                    label = {
                                        Text(
                                            "🌱 ${crop.titleHi} ($count)",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xAA1F2937),
                                        labelColor = Color.White,
                                        selectedContainerColor = ForestGreen,
                                        selectedLabelColor = HarvestGold
                                    )
                                )
                            }
                        }
                    }

                    // Status Banner
                    Surface(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = world.statusBannerHi,
                            color = HarvestGold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Bottom Controls & Vehicle/Village Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Driving Controls Row: Left Steering Joystick | Center Context Action | Right Pedals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left Steering Joystick + Horn
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = Color(0xBB1F2937),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { viewModel.honkHorn() }
                                    .testTag("horn_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.VolumeUp,
                                        contentDescription = "Horn",
                                        tint = MustardAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            VirtualJoystick(
                                onMove = { nx, ny ->
                                    joyX = nx
                                    joyY = ny
                                    pushControls(jx = nx, jy = ny)
                                }
                            )
                        }

                        // Center Context Action Button (appears when near field / pump / market / shed / thresher)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            AnimatedVisibility(visible = world.availableAction != ContextActionType.NONE) {
                                Button(
                                    onClick = { viewModel.triggerContextAction() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MustardAmber,
                                        contentColor = Color(0xFF1A1410)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("context_action_button")
                                ) {
                                    Text(
                                        text = world.availableAction.labelHi,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Right Accelerator & Brake/Reverse Pedals
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Surface(
                                color = if (revPressed) TractorCrimson else Color(0xCC991B1B),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .size(width = 62.dp, height = 74.dp)
                                    .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                revPressed = true
                                                pushControls(rev = true)
                                                tryAwaitRelease()
                                                revPressed = false
                                                pushControls(rev = false)
                                            }
                                        )
                                    }
                                    .testTag("reverse_pedal")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "रिवर्स\nREV",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = if (gasPressed) Color(0xFF16A34A) else Color(0xCC15803D),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .size(width = 72.dp, height = 94.dp)
                                    .border(2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                gasPressed = true
                                                pushControls(gas = true)
                                                tryAwaitRelease()
                                                gasPressed = false
                                                pushControls(gas = false)
                                            }
                                        )
                                    }
                                    .testTag("gas_pedal")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "रेस\nGAS",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Vehicle Switcher Strip
                    Surface(
                        color = Color(0xCC111827),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val vehicleBadges = mapOf(
                                VehicleType.TRACTOR to "🚜 ट्रैक्टर",
                                VehicleType.HARVESTER to "🌾 हार्वेस्टर",
                                VehicleType.JCB to "🏗️ JCB",
                                VehicleType.BULLDOZER to "🚧 बुलडोजर",
                                VehicleType.TRUCK to "🚚 ट्रक",
                                VehicleType.PICKUP to "🚙 पिकअप"
                            )
                            VehicleType.entries.forEach { vType ->
                                val selected = world.activeVehicle == vType
                                Surface(
                                    color = if (selected) TractorCrimson else Color(0xFF283244),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clickable { viewModel.switchVehicle(vType) }
                                        .testTag("vehicle_${vType.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (selected) Color.White else HarvestGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = vehicleBadges[vType] ?: vType.shortHi,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Village Buildings Quick Dock: Market, Petrol Pump, Animal Shed, Missions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VillageDockButton(
                            icon = Icons.Filled.Storefront,
                            label = "बाजार",
                            containerColor = ForestGreen,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dock_market"),
                            onClick = { viewModel.openSheet(ActiveModalSheet.MARKET) }
                        )
                        VillageDockButton(
                            icon = Icons.Filled.LocalGasStation,
                            label = "पंप",
                            containerColor = TractorCrimson,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dock_pump"),
                            onClick = { viewModel.openSheet(ActiveModalSheet.PETROL_PUMP) }
                        )
                        VillageDockButton(
                            icon = Icons.Filled.Pets,
                            label = "पशु बाड़ा",
                            containerColor = TerracottaWarm,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dock_animals"),
                            onClick = { viewModel.openSheet(ActiveModalSheet.LIVESTOCK) }
                        )
                        VillageDockButton(
                            icon = Icons.Filled.TaskAlt,
                            label = "मिशन",
                            containerColor = Color(0xFF1D4ED8),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dock_missions"),
                            onClick = { viewModel.openSheet(ActiveModalSheet.MISSIONS_LEDGER) }
                        )
                    }
                }

                // Modals for Market, Petrol Pump, Animal Shed, and Missions/Ledger
                VillageModalSheet(
                    sheetType = activeSheet,
                    save = save,
                    activeVehicle = activeVehicle,
                    missions = viewModel.getCurrentMissions(),
                    marketLogsState = marketLogsState,
                    onDismiss = { viewModel.closeSheet() },
                    onSellCrop = { crop, qty -> viewModel.sellCrop(crop, qty) },
                    onSellMilk = { qty -> viewModel.sellMilk(qty) },
                    onBuySeeds = { crop, qty -> viewModel.buySeeds(crop, qty) },
                    onBuyFodder = { qty -> viewModel.buyFodder(qty) },
                    onFeedAnimals = { viewModel.feedAnimals() },
                    onBuyAnimal = { kind -> viewModel.buyAnimal(kind) },
                    onRefuel = { full -> viewModel.refuelActiveVehicle(full) },
                    onEmergencyRefuel = { viewModel.emergencyJerrycanRefuel() },
                    onClaimMission = { mission -> viewModel.claimMissionReward(mission) }
                )
            }
        }
    }
}

@Composable
private fun VillageDockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit
) {
    val sizeDp = 116.dp
    val knobDp = 46.dp
    val maxRadiusPx = with(LocalDensity.current) { ((sizeDp - knobDp) * 0.5f).toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0x88111827))
            .border(2.dp, HarvestGold.copy(alpha = 0.65f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        offsetX = 0f
                        offsetY = 0f
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        offsetX = 0f
                        offsetY = 0f
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nx = offsetX + dragAmount.x
                        val ny = offsetY + dragAmount.y
                        val dist = hypot(nx, ny)
                        if (dist > maxRadiusPx && dist > 0f) {
                            offsetX = (nx / dist) * maxRadiusPx
                            offsetY = (ny / dist) * maxRadiusPx
                        } else {
                            offsetX = nx
                            offsetY = ny
                        }
                        onMove((offsetX / maxRadiusPx).coerceIn(-1f, 1f), (offsetY / maxRadiusPx).coerceIn(-1f, 1f))
                    }
                )
            }
            .testTag("steering_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(knobDp)
                .clip(CircleShape)
                .background(HarvestGold)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
