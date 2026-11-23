package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FarmRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeFarmSave(userId: String): Flow<FarmSave?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            throw IllegalStateException("User must be signed in with matching UID.")
        }
        val docRef = db.collection("users").document(uid).collection("farms").document(MAIN_FARM_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) {
                        null
                    } else {
                        snapshot.toObject(
                            FarmSave::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeMarketLogs(userId: String): Flow<List<MarketLog>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || uid != userId) {
            throw IllegalStateException("User must be signed in with matching UID.")
        }
        val colRef = db.collection("users").document(uid).collection("market_logs")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MarketLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureInitialFarmSave(userId: String, defaultFarmerName: String): Result<Unit> {
        val path = "users/$userId/farms/$MAIN_FARM_ID"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid).collection("farms").document(MAIN_FARM_ID)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val sanitizedName = defaultFarmerName.trim().ifBlank { "शुभम" }.take(50)
                val initial = FarmSave(userId = uid, farmerName = sanitizedName)
                val payload = createPayload(uid, initial)
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncFarmProgress(userId: String, farm: FarmSave): Result<Unit> {
        val path = "users/$userId/farms/$MAIN_FARM_ID"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(uid).collection("farms").document(MAIN_FARM_ID)
            val safeVehicles = farm.unlockedVehicles.take(20).ifEmpty { listOf("tractor") }
            val safeFields = farm.fieldStates.take(16).ifEmpty { listOf("WHEAT:RIPE:100") }
            val updateMap = mapOf(
                "coins" to farm.coins.coerceIn(0L, 100_000_000L),
                "level" to farm.level.coerceIn(1L, 1000L),
                "xp" to farm.xp.coerceIn(0L, 100_000_000L),
                "dayCount" to farm.dayCount.coerceIn(1L, 100_000L),
                "activeVehicleId" to farm.activeVehicleId.ifBlank { "tractor" }.take(40),
                "unlockedVehicles" to safeVehicles,
                "fieldStates" to safeFields,
                "cowCount" to farm.cowCount.coerceIn(0L, 500L),
                "buffaloCount" to farm.buffaloCount.coerceIn(0L, 500L),
                "goatCount" to farm.goatCount.coerceIn(0L, 500L),
                "henCount" to farm.henCount.coerceIn(0L, 500L),
                "fodderCount" to farm.fodderCount.coerceIn(0L, 10_000L),
                "milkCount" to farm.milkCount.coerceIn(0L, 10_000L),
                "wheatSeeds" to farm.wheatSeeds.coerceIn(0L, 10_000L),
                "riceSeeds" to farm.riceSeeds.coerceIn(0L, 10_000L),
                "sugarcaneSeeds" to farm.sugarcaneSeeds.coerceIn(0L, 10_000L),
                "mustardSeeds" to farm.mustardSeeds.coerceIn(0L, 10_000L),
                "wheatStock" to farm.wheatStock.coerceIn(0L, 100_000L),
                "riceStock" to farm.riceStock.coerceIn(0L, 100_000L),
                "sugarcaneStock" to farm.sugarcaneStock.coerceIn(0L, 100_000L),
                "mustardStock" to farm.mustardStock.coerceIn(0L, 100_000L),
                "fuelReserve" to farm.fuelReserve.coerceIn(0L, 100L),
                "missionsCompleted" to farm.missionsCompleted.coerceIn(0L, 10_000L),
                "totalHarvested" to farm.totalHarvested.coerceIn(0L, 1_000_000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addMarketLog(
        userId: String,
        title: String,
        category: MarketCategory,
        amountCoins: Long
    ): Result<Unit> {
        var path = "users/$userId/market_logs"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val safeTitle = title.trim().ifBlank { "मंडी लेन-देन" }.take(100)
            val docRef = db.collection("users").document(uid).collection("market_logs").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "category" to category.name,
                "amountCoins" to amountCoins.coerceIn(-10_000_000L, 10_000_000L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private fun createPayload(uid: String, farm: FarmSave): Map<String, Any> {
        return mapOf(
            "userId" to uid,
            "farmerName" to farm.farmerName.trim().ifBlank { "शुभम" }.take(50),
            "coins" to farm.coins.coerceIn(0L, 100_000_000L),
            "level" to farm.level.coerceIn(1L, 1000L),
            "xp" to farm.xp.coerceIn(0L, 100_000_000L),
            "dayCount" to farm.dayCount.coerceIn(1L, 100_000L),
            "activeVehicleId" to farm.activeVehicleId.ifBlank { "tractor" }.take(40),
            "unlockedVehicles" to farm.unlockedVehicles.take(20),
            "fieldStates" to farm.fieldStates.take(16),
            "cowCount" to farm.cowCount.coerceIn(0L, 500L),
            "buffaloCount" to farm.buffaloCount.coerceIn(0L, 500L),
            "goatCount" to farm.goatCount.coerceIn(0L, 500L),
            "henCount" to farm.henCount.coerceIn(0L, 500L),
            "fodderCount" to farm.fodderCount.coerceIn(0L, 10_000L),
            "milkCount" to farm.milkCount.coerceIn(0L, 10_000L),
            "wheatSeeds" to farm.wheatSeeds.coerceIn(0L, 10_000L),
            "riceSeeds" to farm.riceSeeds.coerceIn(0L, 10_000L),
            "sugarcaneSeeds" to farm.sugarcaneSeeds.coerceIn(0L, 10_000L),
            "mustardSeeds" to farm.mustardSeeds.coerceIn(0L, 10_000L),
            "wheatStock" to farm.wheatStock.coerceIn(0L, 100_000L),
            "riceStock" to farm.riceStock.coerceIn(0L, 100_000L),
            "sugarcaneStock" to farm.sugarcaneStock.coerceIn(0L, 100_000L),
            "mustardStock" to farm.mustardStock.coerceIn(0L, 100_000L),
            "fuelReserve" to farm.fuelReserve.coerceIn(0L, 100L),
            "missionsCompleted" to farm.missionsCompleted.coerceIn(0L, 10_000L),
            "totalHarvested" to farm.totalHarvested.coerceIn(0L, 1_000_000L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    companion object {
        const val MAIN_FARM_ID = "main_farm"
    }
}
