package com.example.ui

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FarmRepository
import com.example.data.FarmSave
import com.example.data.MarketCategory
import com.example.data.MarketLog
import com.example.game.AnimalKind
import com.example.game.ContextActionType
import com.example.game.CropType
import com.example.game.FarmMission
import com.example.game.FloatingPopup
import com.example.game.GameEngine
import com.example.game.GameWorldState
import com.example.game.SoilStage
import com.example.game.VehicleType
import com.example.game.ZoneType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ActiveModalSheet {
    NONE,
    MARKET,
    PETROL_PUMP,
    LIVESTOCK,
    MISSIONS_LEDGER
}

class GameViewModel(
    private val repository: FarmRepository,
    private val currentUserId: String,
    defaultFarmerName: String
) : ViewModel() {

    private val engine = GameEngine()
    private var localSave: FarmSave = FarmSave(userId = currentUserId, farmerName = defaultFarmerName)
    private var worldInitialized = false
    private var dirtySave = false
    private var localPopupSeq = 5000L

    private val _farmUiState = MutableStateFlow<UiState<FarmSave>>(UiState.Loading)
    val farmUiState: StateFlow<UiState<FarmSave>> = _farmUiState.asStateFlow()

    val marketLogsState: StateFlow<UiState<List<MarketLog>>> = repository.observeMarketLogs(currentUserId)
        .map<List<MarketLog>, UiState<List<MarketLog>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing market logs", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load market ledger"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _worldState = MutableStateFlow(engine.createInitialWorld(null))
    val worldState: StateFlow<GameWorldState> = _worldState.asStateFlow()

    private val _activeSheet = MutableStateFlow(ActiveModalSheet.NONE)
    val activeSheet: StateFlow<ActiveModalSheet> = _activeSheet.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    @Volatile
    private var inputJoyX = 0f

    @Volatile
    private var inputJoyY = 0f

    @Volatile
    private var inputThrottle = 0f

    @Volatile
    private var inputReverse = 0f

    private var simLoopJob: Job? = null

    init {
        viewModelScope.launch {
            val initResult = repository.ensureInitialFarmSave(currentUserId, defaultFarmerName)
            if (initResult.isFailure) {
                _farmUiState.value = UiState.Error(
                    initResult.exceptionOrNull()?.localizedMessage ?: "फार्म लोड नहीं हो पाया"
                )
            }
            repository.observeFarmSave(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing farm save", rootError)
                    _farmUiState.value = UiState.Error(rootError.message ?: "डेटा लोड त्रुटि")
                }
                .collect { cloudSave ->
                    if (cloudSave != null) {
                        localSave = cloudSave
                        _farmUiState.value = UiState.Success(cloudSave)
                        if (!worldInitialized) {
                            worldInitialized = true
                            _worldState.value = engine.createInitialWorld(cloudSave)
                            startSimulationLoop()
                        }
                    }
                }
        }
    }

    private fun startSimulationLoop() {
        if (simLoopJob != null) return
        simLoopJob = viewModelScope.launch {
            var lastTick = System.nanoTime()
            var syncTimer = 0f
            while (isActive) {
                delay(16L)
                val now = System.nanoTime()
                val dt = ((now - lastTick) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastTick = now

                val currentWorld = _worldState.value
                val (nextWorld, modifiedSave) = engine.step(
                    state = currentWorld,
                    dtSeconds = dt,
                    joyX = inputJoyX,
                    joyY = inputJoyY,
                    throttle = inputThrottle,
                    reverse = inputReverse,
                    save = localSave
                )
                _worldState.value = nextWorld
                if (modifiedSave != null) {
                    localSave = modifiedSave
                    _farmUiState.value = UiState.Success(modifiedSave)
                    dirtySave = true
                }

                syncTimer += dt
                if (syncTimer >= 10f && dirtySave) {
                    syncTimer = 0f
                    flushSaveToCloud()
                }
            }
        }
    }

    fun setControls(joyX: Float, joyY: Float, throttle: Float, reverse: Float) {
        inputJoyX = joyX
        inputJoyY = joyY
        inputThrottle = throttle
        inputReverse = reverse
    }

    fun switchVehicle(type: VehicleType) {
        _worldState.update { w ->
            w.copy(
                activeVehicle = type,
                statusBannerHi = "अब आप ${type.titleHi} चला रहे हैं!"
            )
        }
        updateLocalSave(localSave.copy(activeVehicleId = type.id), syncImmediately = true)
    }

    fun selectSeed(crop: CropType) {
        _worldState.update { w ->
            w.copy(
                selectedSeed = crop,
                statusBannerHi = "चुना गया बीज: ${crop.titleHi} 🌱"
            )
        }
    }

    fun honkHorn() {
        _worldState.update { w ->
            val v = w.vehicles[w.activeVehicle]
            val newPopups = if (v != null) {
                w.popups + FloatingPopup(localPopupSeq++, v.x, v.y - 40f, "पीं-पीं! 📢", Color(0xFFFFD166))
            } else w.popups
            w.copy(hornFlash = 0.4f, popups = newPopups)
        }
    }

    fun toggleRain() {
        _worldState.update { w ->
            val nextRain = !w.isRaining
            w.copy(
                isRaining = nextRain,
                statusBannerHi = if (nextRain) "मानसून की बारिश शुरू! फसलें तेजी से बढ़ेंगी 🌧️" else "मौसम साफ और धूप खिली है ☀️"
            )
        }
    }

    fun toggleTrolleyHitch() {
        _worldState.update { w ->
            val nextAttached = !w.trolley.attachedToTractor
            w.copy(
                trolley = w.trolley.copy(attachedToTractor = nextAttached),
                statusBannerHi = if (nextAttached) "ट्रॉली ट्रैक्टर से जुड़ गई 🚜" else "ट्रॉली अलग कर दी गई"
            )
        }
    }

    fun triggerContextAction() {
        val w = _worldState.value
        when (w.availableAction) {
            ContextActionType.OPEN_MARKET -> openSheet(ActiveModalSheet.MARKET)
            ContextActionType.REFUEL_PUMP -> openSheet(ActiveModalSheet.PETROL_PUMP)
            ContextActionType.TEND_ANIMALS -> openSheet(ActiveModalSheet.LIVESTOCK)
            ContextActionType.HITCH_TROLLEY, ContextActionType.UNHITCH_TROLLEY -> toggleTrolleyHitch()
            ContextActionType.THRESH_GRAIN -> runThresher()
            ContextActionType.PLOW,
            ContextActionType.SOW,
            ContextActionType.WATER,
            ContextActionType.HARVEST,
            ContextActionType.CLEAR_LAND -> applyInstantFieldAssist()
            ContextActionType.NONE -> Unit
        }
    }

    private fun applyInstantFieldAssist() {
        val w = _worldState.value
        val fieldIdx = w.nearbyFieldIndex ?: return
        val field = w.fields.getOrNull(fieldIdx) ?: return
        val activeType = w.activeVehicle
        val v = w.vehicles[activeType] ?: return
        var workingSave = localSave
        var changed = false
        var plowedAdd = 0
        var harvestedAdd = 0
        var clearedAdd = 0

        val updatedTiles = field.tiles.map { tile ->
            when {
                tile.stage == SoilStage.ROUGH && (activeType == VehicleType.JCB || activeType == VehicleType.BULLDOZER) -> {
                    clearedAdd++
                    changed = true
                    tile.copy(stage = SoilStage.UNTILLED, crop = CropType.EMPTY, growth = 0f)
                }
                tile.stage == SoilStage.UNTILLED && activeType == VehicleType.TRACTOR -> {
                    plowedAdd++
                    changed = true
                    tile.copy(stage = SoilStage.PLOWED, crop = CropType.EMPTY, growth = 0f)
                }
                tile.stage == SoilStage.PLOWED && activeType == VehicleType.TRACTOR -> {
                    val seed = w.selectedSeed
                    if (engine.getSeedCount(workingSave, seed) > 0L) {
                        workingSave = engine.decrementSeed(workingSave, seed)
                        changed = true
                        tile.copy(stage = SoilStage.SOWN, crop = seed, growth = 10f)
                    } else tile
                }
                (tile.stage == SoilStage.SOWN || tile.stage == SoilStage.GROWING) -> {
                    changed = true
                    val nextG = (tile.growth + 35f).coerceAtMost(100f)
                    tile.copy(growth = nextG, stage = if (nextG >= 100f) SoilStage.RIPE else SoilStage.GROWING)
                }
                tile.stage == SoilStage.RIPE && activeType == VehicleType.HARVESTER -> {
                    harvestedAdd++
                    workingSave = engine.incrementCropStock(workingSave, tile.crop, 1L)
                    changed = true
                    tile.copy(stage = SoilStage.UNTILLED, crop = CropType.EMPTY, growth = 0f)
                }
                else -> tile
            }
        }

        if (changed) {
            val newFields = w.fields.toMutableList()
            newFields[fieldIdx] = field.copy(tiles = updatedTiles)
            if (harvestedAdd > 0) {
                workingSave = workingSave.copy(
                    totalHarvested = workingSave.totalHarvested + harvestedAdd,
                    xp = workingSave.xp + harvestedAdd * 10L
                )
            }
            workingSave = workingSave.copy(fieldStates = newFields.map { it.encodeSummary() })
            _worldState.update {
                it.copy(
                    fields = newFields,
                    missionPlowedTiles = it.missionPlowedTiles + plowedAdd,
                    missionHarvestedTiles = it.missionHarvestedTiles + harvestedAdd,
                    missionClearedTiles = it.missionClearedTiles + clearedAdd,
                    popups = it.popups + FloatingPopup(localPopupSeq++, v.x, v.y - 35f, "खेत का काम पूरा! ✅", Color(0xFF52B788))
                )
            }
            updateLocalSave(workingSave, syncImmediately = true)
        }
    }

    fun runThresher() {
        val w = _worldState.value
        val save = localSave
        val bonusCoins = 120L
        val nextSave = save.copy(
            wheatStock = save.wheatStock + 2L,
            xp = save.xp + 25L,
            coins = save.coins + bonusCoins
        )
        _worldState.update {
            it.copy(
                popups = it.popups + FloatingPopup(localPopupSeq++, 1290f, 260f, "थ्रेशर से +2 गेहूँ बोरी & +₹120 ⚙️", Color(0xFFE9C46A)),
                statusBannerHi = "थ्रेशर मशीन ने अनाज की छँटाई पूरी की!"
            )
        }
        updateLocalSave(nextSave, syncImmediately = true)
    }

    fun openSheet(sheet: ActiveModalSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = ActiveModalSheet.NONE
    }

    fun quickDriveToZone(zoneType: ZoneType) {
        val zone = _worldState.value.zones.firstOrNull { it.type == zoneType } ?: return
        _worldState.update { w ->
            val active = w.vehicles[w.activeVehicle] ?: return@update w
            val targetX = zone.centerX
            val targetY = (zone.y + zone.height + 25f).coerceAtMost(1800f)
            val moved = active.copy(x = targetX, y = targetY, speed = 0f)
            val newVehicles = w.vehicles.toMutableMap().apply { put(w.activeVehicle, moved) }
            val nextTrolley = if (w.trolley.attachedToTractor && w.activeVehicle == VehicleType.TRACTOR) {
                w.trolley.copy(x = targetX - 58f, y = targetY)
            } else w.trolley
            w.copy(
                vehicles = newVehicles,
                trolley = nextTrolley,
                statusBannerHi = "${zone.titleHi} पर पहुँच गए!"
            )
        }
    }

    fun sellCrop(crop: CropType, quantity: Long) {
        val currentStock = when (crop) {
            CropType.WHEAT -> localSave.wheatStock
            CropType.RICE -> localSave.riceStock
            CropType.SUGARCANE -> localSave.sugarcaneStock
            CropType.MUSTARD -> localSave.mustardStock
            CropType.EMPTY -> 0L
        }
        val sellQty = quantity.coerceAtMost(currentStock)
        if (sellQty <= 0L) return
        val earned = sellQty * crop.sellPrice
        val updated = when (crop) {
            CropType.WHEAT -> localSave.copy(wheatStock = localSave.wheatStock - sellQty)
            CropType.RICE -> localSave.copy(riceStock = localSave.riceStock - sellQty)
            CropType.SUGARCANE -> localSave.copy(sugarcaneStock = localSave.sugarcaneStock - sellQty)
            CropType.MUSTARD -> localSave.copy(mustardStock = localSave.mustardStock - sellQty)
            CropType.EMPTY -> localSave
        }.copy(
            coins = localSave.coins + earned,
            xp = localSave.xp + sellQty * 15L
        )
        _worldState.update {
            it.copy(
                trolley = it.trolley.copy(load = (it.trolley.load - sellQty.toInt()).coerceAtLeast(0)),
                missionSoldValue = it.missionSoldValue + earned.toInt(),
                statusBannerHi = "${crop.titleHi} मंडी में बेचकर ₹$earned कमाए!"
            )
        }
        updateLocalSave(updated, syncImmediately = true)
        logTransaction("मंडी में $sellQty ${crop.titleHi} बेचा", MarketCategory.CROP_SALE, earned)
    }

    fun sellMilk(quantity: Long) {
        val sellQty = quantity.coerceAtMost(localSave.milkCount)
        if (sellQty <= 0L) return
        val earned = sellQty * 85L
        val updated = localSave.copy(
            milkCount = localSave.milkCount - sellQty,
            coins = localSave.coins + earned,
            xp = localSave.xp + sellQty * 10L
        )
        _worldState.update {
            it.copy(
                missionSoldValue = it.missionSoldValue + earned.toInt(),
                statusBannerHi = "$sellQty लीटर शुद्ध दूध ₹$earned में बेचा 🥛"
            )
        }
        updateLocalSave(updated, syncImmediately = true)
        logTransaction("डेयरी मंडी में $sellQty लीटर दूध बेचा", MarketCategory.MILK_SALE, earned)
    }

    fun buySeeds(crop: CropType, count: Long) {
        val totalCost = crop.seedPrice * count
        if (localSave.coins < totalCost || count <= 0L) return
        val updated = when (crop) {
            CropType.WHEAT -> localSave.copy(wheatSeeds = localSave.wheatSeeds + count)
            CropType.RICE -> localSave.copy(riceSeeds = localSave.riceSeeds + count)
            CropType.SUGARCANE -> localSave.copy(sugarcaneSeeds = localSave.sugarcaneSeeds + count)
            CropType.MUSTARD -> localSave.copy(mustardSeeds = localSave.mustardSeeds + count)
            CropType.EMPTY -> localSave
        }.copy(coins = localSave.coins - totalCost)
        updateLocalSave(updated, syncImmediately = true)
        logTransaction("$count ${crop.titleHi} बीज खरीदे", MarketCategory.SEED_BUY, -totalCost)
    }

    fun buyFodder(bundles: Long) {
        val cost = bundles * 35L
        if (localSave.coins < cost || bundles <= 0L) return
        val updated = localSave.copy(
            coins = localSave.coins - cost,
            fodderCount = localSave.fodderCount + bundles
        )
        updateLocalSave(updated, syncImmediately = true)
        logTransaction("$bundles गट्ठर हरा चारा खरीदा", MarketCategory.SEED_BUY, -cost)
    }

    fun feedAnimals() {
        if (localSave.fodderCount <= 0L) return
        val totalDairy = localSave.cowCount + localSave.buffaloCount
        val milkProduced = (totalDairy.coerceAtLeast(1L) + 1L) / 2L
        val updated = localSave.copy(
            fodderCount = (localSave.fodderCount - 1L).coerceAtLeast(0L),
            milkCount = localSave.milkCount + milkProduced,
            xp = localSave.xp + 20L
        )
        _worldState.update {
            it.copy(
                missionFedAnimals = it.missionFedAnimals + 1,
                statusBannerHi = "गाय-भैंस को चारा खिलाया! +$milkProduced लीटर दूध मिला 🐄"
            )
        }
        updateLocalSave(updated, syncImmediately = true)
    }

    fun buyAnimal(kind: AnimalKind) {
        if (localSave.coins < kind.price) return
        val updated = when (kind) {
            AnimalKind.COW -> localSave.copy(cowCount = localSave.cowCount + 1L)
            AnimalKind.BUFFALO -> localSave.copy(buffaloCount = localSave.buffaloCount + 1L)
            AnimalKind.GOAT -> localSave.copy(goatCount = localSave.goatCount + 1L)
            AnimalKind.HEN -> localSave.copy(henCount = localSave.henCount + 1L)
        }.copy(coins = localSave.coins - kind.price)

        _worldState.update { w ->
            w.copy(
                animals = engine.buildAnimals(
                    cows = updated.cowCount.toInt(),
                    buffaloes = updated.buffaloCount.toInt(),
                    goats = updated.goatCount.toInt(),
                    hens = updated.henCount.toInt()
                ),
                statusBannerHi = "नया पशु खरीदा: ${kind.titleHi}!"
            )
        }
        updateLocalSave(updated, syncImmediately = true)
        logTransaction("पशु बाड़े के लिए ${kind.titleHi} खरीदी", MarketCategory.ANIMAL_BUY, -kind.price)
    }

    fun refuelActiveVehicle(fullTank: Boolean) {
        val activeType = _worldState.value.activeVehicle
        val v = _worldState.value.vehicles[activeType] ?: return
        val needed = (100f - v.fuel).coerceAtLeast(0f)
        if (needed < 1f) return
        val fillAmount = if (fullTank) needed else needed.coerceAtMost(30f)
        val cost = (fillAmount * 3.5f).toLong().coerceAtLeast(25L)
        if (localSave.coins < cost) return

        val nextFuel = (v.fuel + fillAmount).coerceAtMost(100f)
        _worldState.update { w ->
            val newVehicles = w.vehicles.toMutableMap().apply {
                put(activeType, v.copy(fuel = nextFuel))
            }
            w.copy(
                vehicles = newVehicles,
                statusBannerHi = "${activeType.shortHi} में डीजल भरा गया ⛽"
            )
        }
        val updated = localSave.copy(
            coins = localSave.coins - cost,
            fuelReserve = nextFuel.toLong()
        )
        updateLocalSave(updated, syncImmediately = true)
        logTransaction("${activeType.shortHi} में डीजल भरवाया", MarketCategory.FUEL_BUY, -cost)
    }

    fun emergencyJerrycanRefuel() {
        val activeType = _worldState.value.activeVehicle
        val v = _worldState.value.vehicles[activeType] ?: return
        val nextFuel = (v.fuel + 25f).coerceAtMost(100f)
        _worldState.update { w ->
            val newVehicles = w.vehicles.toMutableMap().apply {
                put(activeType, v.copy(fuel = nextFuel))
            }
            w.copy(vehicles = newVehicles, statusBannerHi = "इमरजेंसी जेरीकैन से डीजल डाला गया!")
        }
    }

    fun getCurrentMissions(): List<FarmMission> {
        return engine.buildMissions(_worldState.value, localSave)
    }

    fun claimMissionReward(mission: FarmMission) {
        if (mission.claimed || mission.currentCount < mission.targetCount) return
        val updated = localSave.copy(
            coins = localSave.coins + mission.rewardCoins,
            missionsCompleted = localSave.missionsCompleted + 1L,
            xp = localSave.xp + 100L
        )
        _worldState.update {
            it.copy(
                claimedMissions = it.claimedMissions + mission.id,
                statusBannerHi = "मिशन पूरा! ₹${mission.rewardCoins} इनाम मिला 🎉"
            )
        }
        updateLocalSave(updated, syncImmediately = true)
        logTransaction("मिशन इनाम: ${mission.titleHi}", MarketCategory.MISSION_REWARD, mission.rewardCoins)
    }

    fun syncNow() {
        flushSaveToCloud()
    }

    private fun updateLocalSave(newSave: FarmSave, syncImmediately: Boolean) {
        val leveledUp = newSave.copy(level = (1L + newSave.xp / 250L).coerceAtLeast(1L))
        localSave = leveledUp
        _farmUiState.value = UiState.Success(leveledUp)
        dirtySave = true
        if (syncImmediately) {
            flushSaveToCloud()
        }
    }

    private fun flushSaveToCloud() {
        val snapshot = localSave
        dirtySave = false
        viewModelScope.launch {
            val res = repository.syncFarmProgress(currentUserId, snapshot)
            _syncMessage.value = if (res.isSuccess) "क्लाउड सेव हो गया ☁️" else "ऑफ़लाइन मोड (क्लाउड सिंक बाकी)"
        }
    }

    private fun logTransaction(title: String, category: MarketCategory, amountCoins: Long) {
        viewModelScope.launch {
            repository.addMarketLog(currentUserId, title, category, amountCoins)
        }
    }

    private companion object {
        const val TAG = "GameViewModel"
    }
}
