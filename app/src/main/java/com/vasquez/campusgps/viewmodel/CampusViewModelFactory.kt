package com.vasquez.campusgps.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vasquez.campusgps.data.LugaresRepositoryLocal
import com.vasquez.campusgps.data.UbicacionRepositoryGoogle

// La fábrica es el único lugar que conoce las implementaciones concretas.
// El Context llega solo hasta el Repository, nunca al ViewModel.
class CampusViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapaViewModel(
            ubicacionRepo = UbicacionRepositoryGoogle(context.applicationContext),
            lugaresRepo = LugaresRepositoryLocal()
        ) as T
    }
}
