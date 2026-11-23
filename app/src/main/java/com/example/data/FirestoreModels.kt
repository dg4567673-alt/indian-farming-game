package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = Firebase.auth.currentUser
    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@Keep
@IgnoreExtraProperties
data class FarmSave(
    @DocumentId val id: String = "",
    val userId: String = "",
    val farmerName: String = "शुभम",
    val coins: Long = 5000L,
    val level: Long = 1L,
    val xp: Long = 0L,
    val dayCount: Long = 1L,
    val activeVehicleId: String = "tractor",
    val unlockedVehicles: List<String> = listOf(
        "tractor",
        "trolley",
        "harvester",
        "thresher",
        "jcb",
        "bulldozer",
        "truck",
        "pickup"
    ),
    val fieldStates: List<String> = listOf(
        "WHEAT:RIPE:100",
        "RICE:GROWING:65",
        "SUGARCANE:PLOWED:0",
        "MUSTARD:UNTILLED:0",
        "EMPTY:ROUGH:0",
        "EMPTY:ROUGH:0"
    ),
    val cowCount: Long = 2L,
    val buffaloCount: Long = 2L,
    val goatCount: Long = 3L,
    val henCount: Long = 4L,
    val fodderCount: Long = 14L,
    val milkCount: Long = 3L,
    val wheatSeeds: Long = 12L,
    val riceSeeds: Long = 10L,
    val sugarcaneSeeds: Long = 8L,
    val mustardSeeds: Long = 8L,
    val wheatStock: Long = 4L,
    val riceStock: Long = 2L,
    val sugarcaneStock: Long = 0L,
    val mustardStock: Long = 0L,
    val fuelReserve: Long = 90L,
    val missionsCompleted: Long = 0L,
    val totalHarvested: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "CROP_SALE",
    val amountCoins: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class MarketCategory {
    CROP_SALE,
    MILK_SALE,
    VEHICLE_BUY,
    ANIMAL_BUY,
    FUEL_BUY,
    SEED_BUY,
    MISSION_REWARD;

    companion object {
        fun fromString(raw: String): MarketCategory {
            return entries.firstOrNull { it.name == raw } ?: CROP_SALE
        }
    }
}
