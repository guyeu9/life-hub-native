package com.lifehub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lifehub.LifeHubApplication

class ScheduleViewModelFactory(private val app: LifeHubApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScheduleViewModel(app) as T
    }
}
