package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.BodyWeightLog
import com.example.data.repository.BodyWeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WeightTimeframe(val label: String, val days: Int) {
    SEVEN_DAYS("7D", 7),
    THIRTY_DAYS("30D", 30),
    NINETY_DAYS("90D", 90),
    ALL_TIME("All", Int.MAX_VALUE)
}

data class WeightStats(
    val current: Double? = null,
    val previous: Double? = null,
    val change: Double? = null,
    val min: Double? = null,
    val max: Double? = null,
    val average7d: Double? = null
)

class BodyWeightViewModel(
    private val repository: BodyWeightRepository
) : ViewModel() {

    val allLogs: StateFlow<List<BodyWeightLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestLog: StateFlow<BodyWeightLog?> = repository.latestLog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTimeframe = MutableStateFlow(WeightTimeframe.THIRTY_DAYS)
    val selectedTimeframe: StateFlow<WeightTimeframe> = _selectedTimeframe.asStateFlow()

    val filteredLogs: StateFlow<List<BodyWeightLog>> = combine(
        allLogs,
        _selectedTimeframe
    ) { logs, timeframe ->
        if (timeframe == WeightTimeframe.ALL_TIME) {
            logs.reversed() // oldest to newest for charts
        } else {
            val nowDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
            val cutoff = nowDay - timeframe.days
            logs.filter { it.dateEpochDay >= cutoff }.reversed()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<WeightStats> = allLogs.combine(_selectedTimeframe) { logs, _ ->
        if (logs.isEmpty()) {
            WeightStats()
        } else {
            val current = logs.firstOrNull()?.weightKg
            val previous = logs.getOrNull(1)?.weightKg
            val change = if (current != null && previous != null) current - previous else null
            val min = logs.minOfOrNull { it.weightKg }
            val max = logs.maxOfOrNull { it.weightKg }

            val nowDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
            val logs7d = logs.filter { it.dateEpochDay >= nowDay - 7 }
            val avg7d = if (logs7d.isNotEmpty()) logs7d.map { it.weightKg }.average() else null

            WeightStats(
                current = current,
                previous = previous,
                change = change,
                min = min,
                max = max,
                average7d = avg7d
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightStats())

    fun setTimeframe(timeframe: WeightTimeframe) {
        _selectedTimeframe.value = timeframe
    }

    fun addWeightLog(weightKg: Double, daysAgo: Long = 0L, notes: String = "") {
        viewModelScope.launch {
            val nowDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
            val epochDay = nowDay - daysAgo
            val timestamp = epochDay * (24 * 60 * 60 * 1000L) + (9 * 3600 * 1000L)
            val log = BodyWeightLog(
                weightKg = weightKg,
                dateEpochDay = epochDay,
                timestamp = timestamp,
                notes = notes
            )
            repository.insertLog(log)
        }
    }

    fun updateWeightLog(log: BodyWeightLog) {
        viewModelScope.launch {
            repository.updateLog(log)
        }
    }

    fun deleteWeightLog(log: BodyWeightLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }
}

class BodyWeightViewModelFactory(
    private val repository: BodyWeightRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BodyWeightViewModel(repository) as T
    }
}
