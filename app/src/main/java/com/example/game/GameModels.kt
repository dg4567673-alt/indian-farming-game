package com.example.game

import androidx.compose.ui.graphics.Color

const val WORLD_WIDTH = 2600f
const val WORLD_HEIGHT = 1900f

enum class VehicleType(
    val id: String,
    val titleHi: String,
    val shortHi: String,
    val maxSpeed: Float,
    val acceleration: Float,
    val turnSpeed: Float,
    val bodyLength: Float,
    val bodyWidth: Float,
    val fuelRate: Float,
    val cargoCapacity: Int,
    val primaryColor: Color,
    val accentColor: Color
) {
    TRACTOR(
        id = "tractor",
        titleHi = "ट्रैक्टर (महिंद्रा)",
        shortHi = "ट्रैक्टर",
        maxSpeed = 235f,
        acceleration = 320f,
        turnSpeed = 2.6f,
        bodyLength = 74f,
        bodyWidth = 46f,
        fuelRate = 0.7f,
        cargoCapacity = 15,
        primaryColor = Color(0xFFD62828),
        accentColor = Color(0xFF222222)
    ),
    HARVESTER(
        id = "harvester",
        titleHi = "कंबाइन हार्वेस्टर",
        shortHi = "हार्वेस्टर",
        maxSpeed = 175f,
        acceleration = 220f,
        turnSpeed = 1.9f,
        bodyLength = 104f,
        bodyWidth = 68f,
        fuelRate = 1.15f,
        cargoCapacity = 40,
        primaryColor = Color(0xFFE09F1F),
        accentColor = Color(0xFF1B4332)
    ),
    JCB(
        id = "jcb",
        titleHi = "JCB खुदाई मशीन",
        shortHi = "JCB",
        maxSpeed = 185f,
        acceleration = 260f,
        turnSpeed = 2.2f,
        bodyLength = 88f,
        bodyWidth = 52f,
        fuelRate = 1.05f,
        cargoCapacity = 20,
        primaryColor = Color(0xFFFFB703),
        accentColor = Color(0xFF2B2D42)
    ),
    BULLDOZER(
        id = "bulldozer",
        titleHi = "भारी बुलडोजर",
        shortHi = "बुलडोजर",
        maxSpeed = 160f,
        acceleration = 240f,
        turnSpeed = 2.0f,
        bodyLength = 90f,
        bodyWidth = 58f,
        fuelRate = 1.25f,
        cargoCapacity = 10,
        primaryColor = Color(0xFFF77F00),
        accentColor = Color(0xFF333533)
    ),
    TRUCK(
        id = "truck",
        titleHi = "मालवाहक ट्रक",
        shortHi = "ट्रक",
        maxSpeed = 305f,
        acceleration = 340f,
        turnSpeed = 2.1f,
        bodyLength = 116f,
        bodyWidth = 54f,
        fuelRate = 0.95f,
        cargoCapacity = 60,
        primaryColor = Color(0xFF1D4ED8),
        accentColor = Color(0xFFF59E0B)
    ),
    PICKUP(
        id = "pickup",
        titleHi = "गाँव पिकअप जीप",
        shortHi = "पिकअप",
        maxSpeed = 335f,
        acceleration = 410f,
        turnSpeed = 2.8f,
        bodyLength = 82f,
        bodyWidth = 44f,
        fuelRate = 0.55f,
        cargoCapacity = 25,
        primaryColor = Color(0xFFF8FAFC),
        accentColor = Color(0xFF0F766E)
    );

    companion object {
        fun fromId(id: String): VehicleType = entries.firstOrNull { it.id == id } ?: TRACTOR
    }
}

enum class CropType(
    val id: String,
    val titleHi: String,
    val seedPrice: Long,
    val sellPrice: Long,
    val growSeconds: Float,
    val ripeColor: Color,
    val youngColor: Color
) {
    EMPTY("EMPTY", "खाली", 0L, 0L, 1f, Color(0xFF7F5539), Color(0xFF7F5539)),
    WHEAT("WHEAT", "गेहूँ", 40L, 130L, 18f, Color(0xFFE9C46A), Color(0xFF74C69D)),
    RICE("RICE", "धान", 55L, 175L, 22f, Color(0xFFD4A373), Color(0xFF40916C)),
    SUGARCANE("SUGARCANE", "गन्ना", 80L, 260L, 26f, Color(0xFF80B918), Color(0xFF55A630)),
    MUSTARD("MUSTARD", "सरसों", 50L, 165L, 20f, Color(0xFFFFD166), Color(0xFF52B788));

    companion object {
        fun fromId(id: String): CropType = entries.firstOrNull { it.id == id } ?: WHEAT
    }
}

enum class SoilStage {
    ROUGH,
    UNTILLED,
    PLOWED,
    SOWN,
    GROWING,
    RIPE;

    companion object {
        fun fromId(id: String): SoilStage = entries.firstOrNull { it.name == id } ?: UNTILLED
    }
}

data class VehicleEntity(
    val type: VehicleType,
    val x: Float,
    val y: Float,
    val angleRad: Float,
    val speed: Float = 0f,
    val steerInput: Float = 0f,
    val fuel: Float = 92f,
    val cargoLoad: Int = 0,
    val cargoCrop: CropType = CropType.WHEAT,
    val boomPhase: Float = 0f
)

data class TrolleyEntity(
    val x: Float,
    val y: Float,
    val angleRad: Float,
    val attachedToTractor: Boolean = true,
    val load: Int = 3,
    val maxLoad: Int = 40,
    val cropType: CropType = CropType.WHEAT
)

data class FieldTile(
    val stage: SoilStage,
    val crop: CropType,
    val growth: Float
)

data class FarmField(
    val index: Int,
    val nameHi: String,
    val left: Float,
    val top: Float,
    val cols: Int,
    val rows: Int,
    val tileSize: Float,
    val tiles: List<FieldTile>
) {
    val right: Float get() = left + cols * tileSize
    val bottom: Float get() = top + rows * tileSize
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f

    fun dominantStage(): SoilStage {
        if (tiles.isEmpty()) return SoilStage.UNTILLED
        return tiles.groupingBy { it.stage }.eachCount().maxByOrNull { it.value }?.key ?: SoilStage.UNTILLED
    }

    fun dominantCrop(): CropType {
        val nonEmpty = tiles.filter { it.crop != CropType.EMPTY }
        if (nonEmpty.isEmpty()) return CropType.EMPTY
        return nonEmpty.groupingBy { it.crop }.eachCount().maxByOrNull { it.value }?.key ?: CropType.WHEAT
    }

    fun encodeSummary(): String {
        val crop = dominantCrop().id
        val stage = dominantStage().name
        val avgGrowth = tiles.map { it.growth }.average().toInt().coerceIn(0, 100)
        return "$crop:$stage:$avgGrowth"
    }
}

enum class ZoneType {
    SHUBHAM_HOME,
    VILLAGE_MARKET,
    PETROL_PUMP,
    ANIMAL_SHED,
    THRESHER_YARD,
    VILLAGE_HOUSES
}

data class WorldZone(
    val type: ZoneType,
    val titleHi: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    val centerX: Float get() = x + width * 0.5f
    val centerY: Float get() = y + height * 0.5f
}

enum class AnimalKind(val titleHi: String, val price: Long) {
    COW("गाय", 900L),
    BUFFALO("भैंस", 1300L),
    GOAT("बकरी", 450L),
    HEN("मुर्गी", 150L)
}

data class AnimalEntity(
    val id: Int,
    val kind: AnimalKind,
    val x: Float,
    val y: Float,
    val targetX: Float,
    val targetY: Float,
    val angleRad: Float,
    val walkPhase: Float = 0f
)

data class NpcTrafficCar(
    val x: Float,
    val y: Float,
    val speed: Float,
    val color: Color,
    val isTruck: Boolean
)

data class DustParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val maxLife: Float,
    val radius: Float,
    val color: Color
)

data class FloatingPopup(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val life: Float = 1.6f
)

data class RainDrop(
    val x: Float,
    val y: Float,
    val speed: Float
)

data class VillageTree(
    val x: Float,
    val y: Float,
    val radius: Float,
    val isPalm: Boolean = false
)

data class VillageHouse(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val roofColor: Color,
    val wallColor: Color,
    val label: String = ""
)

data class FarmMission(
    val id: String,
    val titleHi: String,
    val descriptionHi: String,
    val targetCount: Int,
    val currentCount: Int,
    val rewardCoins: Long,
    val claimed: Boolean = false
)

enum class ContextActionType(val labelHi: String) {
    NONE(""),
    PLOW("खेत जोतें"),
    SOW("बीज बोएं"),
    WATER("सिंचाई करें"),
    HARVEST("फसल काटें"),
    CLEAR_LAND("JCB/बुलडोजर से जमीन साफ करें"),
    HITCH_TROLLEY("ट्रॉली जोड़ें"),
    UNHITCH_TROLLEY("ट्रॉली अलग करें"),
    THRESH_GRAIN("थ्रेशर चलाएं"),
    OPEN_MARKET("बाजार / मंडी खोलें"),
    REFUEL_PUMP("डीजल भरें (पंप)"),
    TEND_ANIMALS("पशु बाड़ा / दूध")
}

data class GameWorldState(
    val vehicles: Map<VehicleType, VehicleEntity> = emptyMap(),
    val activeVehicle: VehicleType = VehicleType.TRACTOR,
    val trolley: TrolleyEntity = TrolleyEntity(x = 560f, y = 470f, angleRad = 0f),
    val fields: List<FarmField> = emptyList(),
    val zones: List<WorldZone> = emptyList(),
    val animals: List<AnimalEntity> = emptyList(),
    val trees: List<VillageTree> = emptyList(),
    val houses: List<VillageHouse> = emptyList(),
    val npcTraffic: List<NpcTrafficCar> = emptyList(),
    val particles: List<DustParticle> = emptyList(),
    val popups: List<FloatingPopup> = emptyList(),
    val rainDrops: List<RainDrop> = emptyList(),
    val selectedSeed: CropType = CropType.WHEAT,
    val thresherBuffer: Int = 6,
    val thresherSpinning: Float = 0f,
    val timeOfDay: Float = 0.32f, // 0..1 (0.25 morning, 0.5 noon, 0.75 sunset, 0.9 night)
    val isRaining: Boolean = false,
    val hornFlash: Float = 0f,
    val actionCooldown: Float = 0f,
    val nearbyZone: ZoneType? = null,
    val nearbyFieldIndex: Int? = null,
    val nearTrolley: Boolean = false,
    val availableAction: ContextActionType = ContextActionType.NONE,
    val speedKmh: Int = 0,
    val missionPlowedTiles: Int = 0,
    val missionHarvestedTiles: Int = 0,
    val missionClearedTiles: Int = 0,
    val missionSoldValue: Int = 0,
    val missionFedAnimals: Int = 0,
    val claimedMissions: Set<String> = emptySet(),
    val statusBannerHi: String = "शुभम फार्मिंग सिम्युलेटर में स्वागत है! ट्रैक्टर चलाकर खेत में जाएं।"
)
