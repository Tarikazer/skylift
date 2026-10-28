package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ExerciseSet
import com.example.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SkyLift", appName)
  }

  @Test
  fun `exercise set json serialization test`() {
    val sets = listOf(
      ExerciseSet(1, 100.0, 10, true),
      ExerciseSet(2, 110.0, 8, false)
    )
    val json = ExerciseSet.listToJsonString(sets)
    val restored = ExerciseSet.listFromJsonString(json)

    assertEquals(2, restored.size)
    assertEquals(100.0, restored[0].weightKg, 0.01)
    assertTrue(restored[0].isCompleted)
    assertEquals(110.0, restored[1].weightKg, 0.01)
  }

  @Test
  fun `muscle group lookup test`() {
    val chest = MuscleGroup.fromString("Chest")
    assertEquals(MuscleGroup.CHEST, chest)

    val legs = MuscleGroup.fromString("legs")
    assertEquals(MuscleGroup.LEGS, legs)
  }
}
