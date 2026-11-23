package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FarmSave
import com.example.data.MarketLog
import com.example.game.AnimalKind
import com.example.game.CropType
import com.example.game.FarmMission
import com.example.game.VehicleEntity
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.FreshSproutGreen
import com.example.ui.theme.TerracottaWarm
import com.example.ui.theme.TractorCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageModalSheet(
    sheetType: ActiveModalSheet,
    save: FarmSave,
    activeVehicle: VehicleEntity?,
    missions: List<FarmMission>,
    marketLogsState: UiState<List<MarketLog>>,
    onDismiss: () -> Unit,
    onSellCrop: (CropType, Long) -> Unit,
    onSellMilk: (Long) -> Unit,
    onBuySeeds: (CropType, Long) -> Unit,
    onBuyFodder: (Long) -> Unit,
    onFeedAnimals: () -> Unit,
    onBuyAnimal: (AnimalKind) -> Unit,
    onRefuel: (Boolean) -> Unit,
    onEmergencyRefuel: () -> Unit,
    onClaimMission: (FarmMission) -> Unit
) {
    if (sheetType == ActiveModalSheet.NONE) return

    BackHandler(onBack = onDismiss)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = when (sheetType) {
                    ActiveModalSheet.MARKET -> "🏪 गाँव का बाजार (मंडी)"
                    ActiveModalSheet.PETROL_PUMP -> "⛽ भारत पेट्रोल पंप"
                    ActiveModalSheet.LIVESTOCK -> "🐄 पशु बाड़ा और डेयरी"
                    ActiveModalSheet.MISSIONS_LEDGER -> "🎯 मिशन और मंडी खाता"
                    ActiveModalSheet.NONE -> ""
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = ForestGreen,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "₹${save.coins}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (sheetType) {
                ActiveModalSheet.MARKET -> MarketSheetContent(
                    save = save,
                    onSellCrop = onSellCrop,
                    onSellMilk = onSellMilk,
                    onBuySeeds = onBuySeeds,
                    onBuyFodder = onBuyFodder
                )
                ActiveModalSheet.PETROL_PUMP -> PetrolPumpContent(
                    save = save,
                    activeVehicle = activeVehicle,
                    onRefuel = onRefuel,
                    onEmergencyRefuel = onEmergencyRefuel
                )
                ActiveModalSheet.LIVESTOCK -> LivestockContent(
                    save = save,
                    onFeedAnimals = onFeedAnimals,
                    onSellMilk = onSellMilk,
                    onBuyAnimal = onBuyAnimal
                )
                ActiveModalSheet.MISSIONS_LEDGER -> MissionsAndLedgerContent(
                    missions = missions,
                    marketLogsState = marketLogsState,
                    onClaimMission = onClaimMission
                )
                ActiveModalSheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun MarketSheetContent(
    save: FarmSave,
    onSellCrop: (CropType, Long) -> Unit,
    onSellMilk: (Long) -> Unit,
    onBuySeeds: (CropType, Long) -> Unit,
    onBuyFodder: (Long) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(430.dp)
    ) {
        item {
            Text(
                text = "फसल और दूध मंडी में बेचें",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        val crops = listOf(
            CropType.WHEAT to save.wheatStock,
            CropType.RICE to save.riceStock,
            CropType.SUGARCANE to save.sugarcaneStock,
            CropType.MUSTARD to save.mustardStock
        )
        items(crops) { (crop, stock) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${crop.titleHi} (स्टॉक: $stock)", style = MaterialTheme.typography.titleMedium)
                        Text("मंडी भाव: ₹${crop.sellPrice} प्रति यूनिट", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = { onSellCrop(crop, stock) },
                        enabled = stock > 0L,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.testTag("sell_${crop.id.lowercase()}")
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("सब बेचें")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🥛 शुद्ध दूध (स्टॉक: ${save.milkCount} लीटर)", style = MaterialTheme.typography.titleMedium)
                        Text("डेयरी भाव: ₹85 प्रति लीटर", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = { onSellMilk(save.milkCount) },
                        enabled = save.milkCount > 0L,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaWarm),
                        modifier = Modifier.testTag("sell_milk_button")
                    ) {
                        Text("दूध बेचें")
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            Text(
                text = "बीज और चारा खरीदें",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val seedOffers = listOf(
            CropType.WHEAT to save.wheatSeeds,
            CropType.RICE to save.riceSeeds,
            CropType.SUGARCANE to save.sugarcaneSeeds,
            CropType.MUSTARD to save.mustardSeeds
        )
        items(seedOffers) { (crop, seedCount) ->
            val packCost = crop.seedPrice * 5L
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🌱 ${crop.titleHi} बीज (मौजूद: $seedCount)", style = MaterialTheme.typography.titleMedium)
                        Text("5 पैकेट = ₹$packCost", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = { onBuySeeds(crop, 5L) },
                        enabled = save.coins >= packCost
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("5 खरीदें")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🌿 हरा चारा (मौजूद: ${save.fodderCount} गट्ठर)", style = MaterialTheme.typography.titleMedium)
                        Text("5 गट्ठर = ₹175", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = { onBuyFodder(5L) },
                        enabled = save.coins >= 175L
                    ) {
                        Text("5 खरीदें")
                    }
                }
            }
        }
    }
}

@Composable
private fun PetrolPumpContent(
    save: FarmSave,
    activeVehicle: VehicleEntity?,
    onRefuel: (Boolean) -> Unit,
    onEmergencyRefuel: () -> Unit
) {
    val fuel = activeVehicle?.fuel ?: 100f
    val name = activeVehicle?.type?.titleHi ?: "ट्रैक्टर"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("गाड़ी: $name", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("डीजल स्तर: ${fuel.toInt()}%", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (fuel / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = if (fuel < 25f) TractorCrimson else FreshSproutGreen
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onRefuel(false) },
                enabled = fuel < 99f && save.coins >= 25L,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("refuel_partial_button")
            ) {
                Icon(Icons.Filled.LocalGasStation, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("+30% डीजल")
            }
            Button(
                onClick = { onRefuel(true) },
                enabled = fuel < 99f && save.coins >= 25L,
                colors = ButtonDefaults.buttonColors(containerColor = TractorCrimson),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("refuel_full_button")
            ) {
                Icon(Icons.Filled.LocalGasStation, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("टंकी फुल करें")
            }
        }

        if (fuel < 20f) {
            OutlinedButton(
                onClick = onEmergencyRefuel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("इमरजेंसी जेरीकैन (मुफ्त +25% डीजल)")
            }
        }
    }
}

@Composable
private fun LivestockContent(
    save: FarmSave,
    onFeedAnimals: () -> Unit,
    onSellMilk: (Long) -> Unit,
    onBuyAnimal: (AnimalKind) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(420.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "🐄 गाय: ${save.cowCount}   🐃 भैंस: ${save.buffaloCount}   🐐 बकरी: ${save.goatCount}   🐔 मुर्गी: ${save.henCount}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "हरा चारा: ${save.fodderCount} गट्ठर  |  दूध: ${save.milkCount} लीटर",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onFeedAnimals,
                            enabled = save.fodderCount > 0L,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("feed_animals_button")
                        ) {
                            Icon(Icons.Filled.Pets, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("चारा दें व दूध लें")
                        }
                        OutlinedButton(
                            onClick = { onSellMilk(save.milkCount) },
                            enabled = save.milkCount > 0L,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("दूध बेचें (₹${save.milkCount * 85})")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "नए जानवर खरीदें",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(AnimalKind.entries) { kind ->
            val emoji = when (kind) {
                AnimalKind.COW -> "🐄"
                AnimalKind.BUFFALO -> "🐃"
                AnimalKind.GOAT -> "🐐"
                AnimalKind.HEN -> "🐔"
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("$emoji ${kind.titleHi}", style = MaterialTheme.typography.titleMedium)
                        Text("कीमत: ₹${kind.price}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = { onBuyAnimal(kind) },
                        enabled = save.coins >= kind.price,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaWarm)
                    ) {
                        Text("खरीदें")
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionsAndLedgerContent(
    missions: List<FarmMission>,
    marketLogsState: UiState<List<MarketLog>>,
    onClaimMission: (FarmMission) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(430.dp)
    ) {
        item {
            Text(
                text = "आज के खेती मिशन",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(missions, key = { it.id }) { mission ->
            val ready = !mission.claimed && mission.currentCount >= mission.targetCount
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mission.titleHi, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${mission.descriptionHi} (${mission.currentCount}/${mission.targetCount})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (mission.claimed) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Claimed",
                            tint = FreshSproutGreen
                        )
                    } else {
                        Button(
                            onClick = { onClaimMission(mission) },
                            enabled = ready,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                        ) {
                            Text("₹${mission.rewardCoins}")
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            Text(
                text = "मंडी और पंप लेन-देन खाता (Cloud Ledger)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (marketLogsState) {
            is UiState.Loading -> item {
                Text("लेन-देन लोड हो रहे हैं...", style = MaterialTheme.typography.bodyMedium)
            }
            is UiState.Error -> item {
                Text(marketLogsState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (marketLogsState.data.isEmpty()) {
                    item {
                        Text(
                            "अभी कोई लेन-देन नहीं हुआ है। मंडी में फसल बेचें या डीजल भरवाएं!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(marketLogsState.data, key = { it.id }) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            val sign = if (log.amountCoins >= 0) "+" else ""
                            Text(
                                text = "${sign}₹${log.amountCoins}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (log.amountCoins >= 0) ForestGreen else TractorCrimson
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
