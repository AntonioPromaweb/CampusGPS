package com.vasquez.campusgps.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.vasquez.campusgps.model.ResultadoBusqueda
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

class BuscadorRepositoryGeocoder(private val context: Context) : BuscadorRepository {

    private val geocoder = Geocoder(context, Locale("es", "PE"))

    private companion object {
        // Caja que cubre Lima Metropolitana: el campus está en Lima
        const val LIMA_SUR = -12.55
        const val LIMA_OESTE = -77.20
        const val LIMA_NORTE = -11.75
        const val LIMA_ESTE = -76.70
    }

    override suspend fun buscar(texto: String): Result<List<ResultadoBusqueda>> {
        if (!Geocoder.isPresent()) {
            return Result.failure(BuscadorNoDisponibleException())
        }
        if (texto.isBlank()) {
            return Result.success(emptyList())
        }

        return try {
            val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(texto, 5, LIMA_SUR, LIMA_OESTE, LIMA_NORTE, LIMA_ESTE, object : Geocoder.GeocodeListener {
                        override fun onGeocode(results: List<Address>) {
                            if (continuation.isActive) {
                                continuation.resume(results)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(texto, 5, LIMA_SUR, LIMA_OESTE, LIMA_NORTE, LIMA_ESTE) ?: emptyList()
                }
            }

            val resultados = addresses.map { addr ->
                val nombre = addr.featureName ?: addr.getAddressLine(0) ?: texto
                val detalle = addr.getAddressLine(0) ?: "${addr.latitude}, ${addr.longitude}"
                ResultadoBusqueda(
                    nombre = nombre,
                    detalle = detalle,
                    latitud = addr.latitude,
                    longitud = addr.longitude,
                    lugarId = null
                )
            }
            Result.success(resultados)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
