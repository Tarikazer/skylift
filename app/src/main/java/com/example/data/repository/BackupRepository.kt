package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.dao.BodyWeightLogDao
import com.example.data.dao.ExerciseLogDao
import com.example.data.dao.WorkoutDao
import com.example.data.entity.BodyWeightLog
import com.example.data.entity.ExerciseLog
import com.example.data.entity.Workout
import com.example.model.ExerciseSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(
    private val context: Context,
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val bodyWeightLogDao: BodyWeightLogDao
) {
    data class BackupSummary(
        val totalWorkouts: Int,
        val totalExercises: Int,
        val totalBodyWeightLogs: Int,
        val jsonPayload: String
    )

    data class RestoreResult(
        val success: Boolean,
        val workoutsRestored: Int = 0,
        val exercisesRestored: Int = 0,
        val bodyWeightLogsRestored: Int = 0,
        val errorMessage: String? = null
    )

    suspend fun generateBackupJson(): BackupSummary = withContext(Dispatchers.IO) {
        val workouts = workoutDao.getAllWorkouts()
        // Read current state from DB
        val dbWorkouts = mutableListOf<Workout>()
        val dbExercises = exerciseLogDao.getAllExerciseLogsOnce()
        val dbWeights = bodyWeightLogDao.getAllBodyWeightLogsOnce()

        // Fetch workouts directly
        val cursorWorkouts = workoutDao.getRoutineTemplates()
        // Collect all workouts
        val allWorkoutsList = mutableListOf<Workout>()
        // Let's get them from db directly
        val workoutsOnce = mutableListOf<Workout>()
        // Querying all
        val rootJson = JSONObject()
        rootJson.put("app", "SkyLift")
        rootJson.put("version", 1)
        rootJson.put("exportedAt", System.currentTimeMillis())

        val workoutsArray = JSONArray()
        // We'll read all workouts
        val appDb = com.example.data.database.AppDatabase.getInstance(context)
        val cursor = appDb.query("SELECT * FROM workouts", null)
        try {
            while (cursor.moveToNext()) {
                val w = Workout(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow("dayOfWeek")),
                    targetMuscles = cursor.getString(cursor.getColumnIndexOrThrow("targetMuscles")),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                    lastPerformedAt = if (cursor.isNull(cursor.getColumnIndexOrThrow("lastPerformedAt"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("lastPerformedAt")),
                    isRoutineTemplate = cursor.getInt(cursor.getColumnIndexOrThrow("isRoutineTemplate")) == 1,
                    durationMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("durationMinutes"))
                )
                workoutsOnce.add(w)

                val wObj = JSONObject().apply {
                    put("id", w.id)
                    put("name", w.name)
                    put("dayOfWeek", w.dayOfWeek)
                    put("targetMuscles", w.targetMuscles)
                    put("createdAt", w.createdAt)
                    put("lastPerformedAt", w.lastPerformedAt ?: -1L)
                    put("isRoutineTemplate", w.isRoutineTemplate)
                    put("durationMinutes", w.durationMinutes)
                }
                workoutsArray.put(wObj)
            }
        } finally {
            cursor.close()
        }
        rootJson.put("workouts", workoutsArray)

        val exercisesArray = JSONArray()
        dbExercises.forEach { e ->
            val eObj = JSONObject().apply {
                put("id", e.id)
                put("workoutId", e.workoutId)
                put("exerciseName", e.exerciseName)
                put("targetMuscle", e.targetMuscle)
                put("setsCount", e.setsCount)
                put("reps", e.reps)
                put("maxKg", e.maxKg)
                put("restTimeSeconds", e.restTimeSeconds)
                put("setsDataJson", e.setsDataJson)
                put("notes", e.notes)
                put("timestamp", e.timestamp)
            }
            exercisesArray.put(eObj)
        }
        rootJson.put("exerciseLogs", exercisesArray)

        val weightsArray = JSONArray()
        dbWeights.forEach { b ->
            val bObj = JSONObject().apply {
                put("id", b.id)
                put("weightKg", b.weightKg)
                put("dateEpochDay", b.dateEpochDay)
                put("timestamp", b.timestamp)
                put("notes", b.notes)
            }
            weightsArray.put(bObj)
        }
        rootJson.put("bodyWeightLogs", weightsArray)

        BackupSummary(
            totalWorkouts = workoutsOnce.size,
            totalExercises = dbExercises.size,
            totalBodyWeightLogs = dbWeights.size,
            jsonPayload = rootJson.toString(2)
        )
    }

    suspend fun saveBackupToUri(uri: Uri, jsonPayload: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonPayload.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveBackupToInternalStorage(jsonPayload: String): File? = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(backupDir, "SkyLift_Backup_$timeStamp.json")
            FileOutputStream(file).use { stream ->
                stream.write(jsonPayload.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreFromUri(uri: Uri, clearExisting: Boolean): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext RestoreResult(false, errorMessage = "Unable to read backup file")
            restoreFromJsonString(content, clearExisting)
        } catch (e: Exception) {
            RestoreResult(false, errorMessage = e.localizedMessage ?: "Unknown restore error")
        }
    }

    suspend fun restoreFromJsonString(jsonString: String, clearExisting: Boolean): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("workouts") && !root.has("bodyWeightLogs")) {
                return@withContext RestoreResult(false, errorMessage = "Invalid SkyLift backup file format")
            }

            if (clearExisting) {
                workoutDao.deleteAllWorkouts()
                exerciseLogDao.deleteAllExerciseLogs()
                bodyWeightLogDao.deleteAllBodyWeightLogs()
            }

            var workoutsCount = 0
            var exercisesCount = 0
            var weightsCount = 0

            val oldToNewWorkoutId = mutableMapOf<Long, Long>()

            val workoutsArray = root.optJSONArray("workouts")
            if (workoutsArray != null) {
                for (i in 0 until workoutsArray.length()) {
                    val obj = workoutsArray.getJSONObject(i)
                    val originalId = obj.optLong("id", 0L)
                    val lastPerf = obj.optLong("lastPerformedAt", -1L)
                    val workout = Workout(
                        id = if (clearExisting) originalId else 0L,
                        name = obj.getString("name"),
                        dayOfWeek = obj.optString("dayOfWeek", ""),
                        targetMuscles = obj.optString("targetMuscles", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastPerformedAt = if (lastPerf > 0) lastPerf else null,
                        isRoutineTemplate = obj.optBoolean("isRoutineTemplate", false),
                        durationMinutes = obj.optInt("durationMinutes", 0)
                    )
                    val newId = workoutDao.insertWorkout(workout)
                    if (originalId > 0) {
                        oldToNewWorkoutId[originalId] = if (clearExisting) originalId else newId
                    }
                    workoutsCount++
                }
            }

            val exercisesArray = root.optJSONArray("exerciseLogs")
            if (exercisesArray != null) {
                for (i in 0 until exercisesArray.length()) {
                    val obj = exercisesArray.getJSONObject(i)
                    val oldWorkoutId = obj.optLong("workoutId", 0L)
                    val mappedWorkoutId = oldToNewWorkoutId[oldWorkoutId] ?: oldWorkoutId
                    val ex = ExerciseLog(
                        id = if (clearExisting) obj.optLong("id", 0L) else 0L,
                        workoutId = mappedWorkoutId,
                        exerciseName = obj.getString("exerciseName"),
                        targetMuscle = obj.getString("targetMuscle"),
                        setsCount = obj.getInt("setsCount"),
                        reps = obj.getInt("reps"),
                        maxKg = obj.getDouble("maxKg"),
                        restTimeSeconds = obj.optInt("restTimeSeconds", 60),
                        setsDataJson = obj.optString("setsDataJson", ""),
                        notes = obj.optString("notes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    exerciseLogDao.insertExerciseLog(ex)
                    exercisesCount++
                }
            }

            val weightsArray = root.optJSONArray("bodyWeightLogs")
            if (weightsArray != null) {
                for (i in 0 until weightsArray.length()) {
                    val obj = weightsArray.getJSONObject(i)
                    val b = BodyWeightLog(
                        id = if (clearExisting) obj.optLong("id", 0L) else 0L,
                        weightKg = obj.getDouble("weightKg"),
                        dateEpochDay = obj.getLong("dateEpochDay"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        notes = obj.optString("notes", "")
                    )
                    bodyWeightLogDao.insertBodyWeightLog(b)
                    weightsCount++
                }
            }

            RestoreResult(
                success = true,
                workoutsRestored = workoutsCount,
                exercisesRestored = exercisesCount,
                bodyWeightLogsRestored = weightsCount
            )
        } catch (e: Exception) {
            RestoreResult(false, errorMessage = e.localizedMessage ?: "Failed parsing JSON")
        }
    }

    /**
     * Seeds initial standard workout routines (Push, Pull, Legs, Upper, Lower)
     * and demo bodyweight progress so the user has immediate rich data to test.
     */
    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        val existingRoutines = workoutDao.getRoutineTemplates()
        // Push Day
        val pushWorkout = Workout(
            name = "Push Day (Chest / Shoulders / Triceps)",
            dayOfWeek = "Monday",
            targetMuscles = "Chest, Shoulders, Arms",
            createdAt = System.currentTimeMillis() - 7 * 86400000L,
            lastPerformedAt = System.currentTimeMillis() - 2 * 86400000L,
            isRoutineTemplate = true
        )
        val pushId = workoutDao.insertWorkout(pushWorkout)
        val benchSets = listOf(
            ExerciseSet(1, 80.0, 10, true),
            ExerciseSet(2, 90.0, 8, true),
            ExerciseSet(3, 100.0, 6, true),
            ExerciseSet(4, 105.0, 4, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = pushId,
                exerciseName = "Barbell Bench Press",
                targetMuscle = "Chest",
                setsCount = 4,
                reps = 8,
                maxKg = 105.0,
                restTimeSeconds = 90,
                setsDataJson = ExerciseSet.listToJsonString(benchSets),
                notes = "Felt strong on final top set!"
            )
        )
        val ohpSets = listOf(
            ExerciseSet(1, 50.0, 10, true),
            ExerciseSet(2, 55.0, 8, true),
            ExerciseSet(3, 60.0, 6, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = pushId,
                exerciseName = "Overhead Shoulder Press",
                targetMuscle = "Shoulders",
                setsCount = 3,
                reps = 8,
                maxKg = 60.0,
                restTimeSeconds = 60,
                setsDataJson = ExerciseSet.listToJsonString(ohpSets)
            )
        )
        val dipSets = listOf(
            ExerciseSet(1, 0.0, 15, true),
            ExerciseSet(2, 15.0, 10, true),
            ExerciseSet(3, 20.0, 8, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = pushId,
                exerciseName = "Triceps Dips",
                targetMuscle = "Arms",
                setsCount = 3,
                reps = 10,
                maxKg = 20.0,
                restTimeSeconds = 60,
                setsDataJson = ExerciseSet.listToJsonString(dipSets)
            )
        )

        // Pull Day
        val pullWorkout = Workout(
            name = "Pull Day (Back / Biceps / Rear Delts)",
            dayOfWeek = "Wednesday",
            targetMuscles = "Back, Arms, Shoulders",
            createdAt = System.currentTimeMillis() - 6 * 86400000L,
            lastPerformedAt = System.currentTimeMillis() - 1 * 86400000L,
            isRoutineTemplate = true
        )
        val pullId = workoutDao.insertWorkout(pullWorkout)
        val rowSets = listOf(
            ExerciseSet(1, 70.0, 10, true),
            ExerciseSet(2, 80.0, 8, true),
            ExerciseSet(3, 85.0, 8, true),
            ExerciseSet(4, 90.0, 6, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = pullId,
                exerciseName = "Barbell Bent-Over Row",
                targetMuscle = "Back",
                setsCount = 4,
                reps = 8,
                maxKg = 90.0,
                restTimeSeconds = 90,
                setsDataJson = ExerciseSet.listToJsonString(rowSets)
            )
        )
        val pullupSets = listOf(
            ExerciseSet(1, 0.0, 12, true),
            ExerciseSet(2, 10.0, 8, true),
            ExerciseSet(3, 10.0, 7, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = pullId,
                exerciseName = "Weighted Pull-Ups",
                targetMuscle = "Back",
                setsCount = 3,
                reps = 8,
                maxKg = 10.0,
                restTimeSeconds = 90,
                setsDataJson = ExerciseSet.listToJsonString(pullupSets)
            )
        )
        val curlSets = listOf(
            ExerciseSet(1, 30.0, 12, true),
            ExerciseSet(2, 35.0, 10, true),
            ExerciseSet(3, 37.5, 8, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = pullId,
                exerciseName = "EZ Bar Bicep Curl",
                targetMuscle = "Arms",
                setsCount = 3,
                reps = 10,
                maxKg = 37.5,
                restTimeSeconds = 60,
                setsDataJson = ExerciseSet.listToJsonString(curlSets)
            )
        )

        // Legs Day
        val legsWorkout = Workout(
            name = "Leg Day (Quads / Hamstrings / Calves)",
            dayOfWeek = "Friday",
            targetMuscles = "Legs, Core",
            createdAt = System.currentTimeMillis() - 5 * 86400000L,
            lastPerformedAt = null,
            isRoutineTemplate = true
        )
        val legsId = workoutDao.insertWorkout(legsWorkout)
        val squatSets = listOf(
            ExerciseSet(1, 100.0, 8, true),
            ExerciseSet(2, 120.0, 6, true),
            ExerciseSet(3, 130.0, 5, true),
            ExerciseSet(4, 140.0, 3, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = legsId,
                exerciseName = "Barbell Back Squat",
                targetMuscle = "Legs",
                setsCount = 4,
                reps = 6,
                maxKg = 140.0,
                restTimeSeconds = 120,
                setsDataJson = ExerciseSet.listToJsonString(squatSets)
            )
        )
        val rdlSets = listOf(
            ExerciseSet(1, 90.0, 10, true),
            ExerciseSet(2, 105.0, 8, true),
            ExerciseSet(3, 115.0, 8, true)
        )
        exerciseLogDao.insertExerciseLog(
            ExerciseLog(
                workoutId = legsId,
                exerciseName = "Romanian Deadlift",
                targetMuscle = "Legs",
                setsCount = 3,
                reps = 8,
                maxKg = 115.0,
                restTimeSeconds = 90,
                setsDataJson = ExerciseSet.listToJsonString(rdlSets)
            )
        )

        // Seed Bodyweight samples over past 3 weeks
        val nowDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
        val sampleWeights = listOf(
            Triple(20L, 81.2, "Starting weight - bulk peak"),
            Triple(16L, 80.9, "Morning fasted"),
            Triple(12L, 80.4, "Water cut start"),
            Triple(8L, 80.0, "Consistent diet"),
            Triple(4L, 79.5, "Feeling leaner"),
            Triple(1L, 79.1, "Weekly low"),
            Triple(0L, 79.0, "Today morning check-in")
        )
        sampleWeights.forEach { (daysAgo, weight, note) ->
            val epochDay = nowDay - daysAgo
            val timestamp = epochDay * (24 * 60 * 60 * 1000L) + (8 * 3600 * 1000L)
            bodyWeightLogDao.insertBodyWeightLog(
                BodyWeightLog(
                    weightKg = weight,
                    dateEpochDay = epochDay,
                    timestamp = timestamp,
                    notes = note
                )
            )
        }
    }

    data class PreMadeExercise(
        val name: String,
        val targetMuscle: String,
        val setsCount: Int,
        val targetReps: Int,
        val repRangeNotes: String,
        val restTimeSeconds: Int
    )

    data class PreMadeRoutine(
        val name: String,
        val dayOfWeek: String,
        val targetMuscles: String,
        val exercises: List<PreMadeExercise>
    )

    /**
     * Loads the user's specific weekly split program:
     * Monday: Legs & Abs
     * Tuesday: Chest & Triceps
     * Thursday: Back & Biceps
     * Friday: Shoulders & Abs
     * Saturday: Cardio & Stretching
     * Wednesday & Sunday: Rest days
     * Skips any routines that already exist by name to avoid duplicates.
     */
    suspend fun loadCustomUserProgram(): Int = withContext(Dispatchers.IO) {
        val existingRoutines = workoutDao.getRoutineTemplatesOnce()
        val existingNames = existingRoutines.map { it.name.trim().lowercase() }.toSet()

        val userProgram = listOf(
            PreMadeRoutine(
                name = "Legs & Abs",
                dayOfWeek = "Monday",
                targetMuscles = "Legs, Core",
                exercises = listOf(
                    PreMadeExercise("Squat (Barbell)", "Legs", 4, 8, "6-10 reps", 105),
                    PreMadeExercise("Leg Press", "Legs", 4, 10, "10-12 reps", 105),
                    PreMadeExercise("Romanian Deadlift (RDL)", "Legs", 4, 8, "8-10 reps", 90),
                    PreMadeExercise("Leg Extension", "Legs", 3, 12, "12-15 reps", 75),
                    PreMadeExercise("Standing Calf Raise", "Legs", 4, 12, "12-15 reps", 75),
                    PreMadeExercise("Abdominal Machine", "Core", 3, 12, "12-15 reps", 60)
                )
            ),
            PreMadeRoutine(
                name = "Chest & Triceps",
                dayOfWeek = "Tuesday",
                targetMuscles = "Chest, Arms",
                exercises = listOf(
                    PreMadeExercise("Flat Bench Press (Barbell)", "Chest", 4, 8, "6-10 reps", 105),
                    PreMadeExercise("Incline Chest Press Machine", "Chest", 3, 10, "8-12 reps", 90),
                    PreMadeExercise("Chest Butterfly Machine", "Chest", 3, 12, "12-15 reps", 75),
                    PreMadeExercise("Triceps Press Machine", "Arms", 3, 10, "10-12 reps", 75),
                    PreMadeExercise("Triceps Rope Pushdown", "Arms", 3, 12, "10-15 reps", 75)
                )
            ),
            PreMadeRoutine(
                name = "Back & Biceps",
                dayOfWeek = "Thursday",
                targetMuscles = "Back, Arms",
                exercises = listOf(
                    PreMadeExercise("Lat Pulldown", "Back", 4, 10, "8-12 reps", 90),
                    PreMadeExercise("T-Bar Row", "Back", 4, 8, "8-10 reps", 90),
                    PreMadeExercise("Seated Cable Row", "Back", 3, 10, "10-12 reps", 90),
                    PreMadeExercise("Barbell Bicep Curl", "Arms", 3, 10, "8-12 reps", 75),
                    PreMadeExercise("Hammer Curl", "Arms", 3, 10, "10-12 reps", 75)
                )
            ),
            PreMadeRoutine(
                name = "Shoulders & Abs",
                dayOfWeek = "Friday",
                targetMuscles = "Shoulders, Core",
                exercises = listOf(
                    PreMadeExercise("Dumbbell Shoulder Press", "Shoulders", 4, 8, "8-10 reps", 105),
                    PreMadeExercise("Dumbbell Lateral Raise", "Shoulders", 4, 12, "12-15 reps", 75),
                    PreMadeExercise("Cable Rear Delt Fly", "Shoulders", 3, 12, "12-15 reps", 75),
                    PreMadeExercise("Abdominal Machine", "Core", 3, 12, "12-15 reps", 60)
                )
            ),
            PreMadeRoutine(
                name = "Cardio & Stretching",
                dayOfWeek = "Saturday",
                targetMuscles = "Cardio, Full Body",
                exercises = listOf(
                    PreMadeExercise("Cardio", "Cardio", 1, 35, "30-45 min", 0),
                    PreMadeExercise("Stretching", "Full Body", 1, 1, "Full Body Stretching", 0)
                )
            )
        )

        var createdCount = 0
        userProgram.forEach { routine ->
            if (!existingNames.contains(routine.name.trim().lowercase())) {
                val workout = Workout(
                    name = routine.name,
                    dayOfWeek = routine.dayOfWeek,
                    targetMuscles = routine.targetMuscles,
                    createdAt = System.currentTimeMillis(),
                    isRoutineTemplate = true
                )
                val workoutId = workoutDao.insertWorkout(workout)

                val exerciseLogs = routine.exercises.map { pe ->
                    val sets = (1..pe.setsCount).map { sNum ->
                        ExerciseSet(
                            setNumber = sNum,
                            weightKg = 0.0,
                            reps = pe.targetReps,
                            isCompleted = false
                        )
                    }
                    ExerciseLog(
                        workoutId = workoutId,
                        exerciseName = pe.name,
                        targetMuscle = pe.targetMuscle,
                        setsCount = pe.setsCount,
                        reps = pe.targetReps,
                        maxKg = 0.0, // empty at first
                        restTimeSeconds = pe.restTimeSeconds,
                        setsDataJson = ExerciseSet.listToJsonString(sets),
                        notes = pe.repRangeNotes
                    )
                }
                exerciseLogDao.insertExerciseLogs(exerciseLogs)
                createdCount++
            }
        }
        createdCount
    }
}
