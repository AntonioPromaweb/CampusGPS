package com.vasquez.campusgps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vasquez.campusgps.R
import com.vasquez.campusgps.model.EstadoBusqueda
import com.vasquez.campusgps.model.ResultadoBusqueda
import com.vasquez.campusgps.ui.theme.obtenerEstiloCategoria

@Composable
fun BarraBusqueda(
    consulta: String,
    estado: EstadoBusqueda,
    sugeridos: List<ResultadoBusqueda> = emptyList(),
    onConsultaCambia: (String) -> Unit,
    onBuscar: () -> Unit,
    onElegir: (ResultadoBusqueda) -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var tieneFoco by remember { mutableStateOf(false) }

    val mostrandoSugeridos = consulta.isEmpty() && tieneFoco && sugeridos.isNotEmpty()
    val mostrandoResultados = consulta.isNotEmpty() && estado !is EstadoBusqueda.Inactiva

    Column(modifier = modifier.fillMaxWidth()) {
        // Campo de texto en forma de píldora
        OutlinedTextField(
            value = consulta,
            onValueChange = onConsultaCambia,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { tieneFoco = it.isFocused },
            placeholder = {
                Text(
                    text = "Buscar un lugar o dirección",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_buscar),
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (consulta.isNotEmpty()) {
                    IconButton(onClick = onLimpiar) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cerrar),
                            contentDescription = "Limpiar búsqueda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onBuscar()
                    keyboardController?.hide()
                }
            )
        )

        // Panel desplegable con los sugeridos o resultados
        AnimatedVisibility(
            visible = mostrandoSugeridos || mostrandoResultados,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .heightIn(max = 240.dp)
                ) {
                    if (mostrandoSugeridos) {
                        Text(
                            text = "Sugeridos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(sugeridos) { res ->
                                val estilo = obtenerEstiloCategoria(res.detalle)
                                ElementoResultado(
                                    resultado = res,
                                    iconoRes = estilo.iconoRes,
                                    colorIcono = estilo.color,
                                    onClick = {
                                        onElegir(res)
                                        keyboardController?.hide()
                                    }
                                )
                            }
                        }
                    } else when (estado) {
                        is EstadoBusqueda.Buscando -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Buscando…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is EstadoBusqueda.Error -> {
                            Text(
                                text = estado.mensaje,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        is EstadoBusqueda.Resultados -> {
                            val hayLocales = estado.locales.isNotEmpty()
                            val hayExternos = estado.externos.isNotEmpty()

                            if (!hayLocales && !hayExternos) {
                                Text(
                                    text = "No encontré ese lugar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (hayLocales) {
                                        item {
                                            Text(
                                                text = "Lugares de la app",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                        }
                                        items(estado.locales) { res ->
                                            val estilo = obtenerEstiloCategoria(res.detalle)
                                            ElementoResultado(
                                                resultado = res,
                                                iconoRes = estilo.iconoRes,
                                                colorIcono = estilo.color,
                                                onClick = {
                                                    onElegir(res)
                                                    keyboardController?.hide()
                                                }
                                            )
                                        }
                                    }

                                    if (hayExternos) {
                                        item {
                                            Text(
                                                text = "Otros lugares",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                        }
                                        items(estado.externos) { res ->
                                            val estilo = obtenerEstiloCategoria("otro")
                                            ElementoResultado(
                                                resultado = res,
                                                iconoRes = estilo.iconoRes,
                                                colorIcono = estilo.color,
                                                onClick = {
                                                    onElegir(res)
                                                    keyboardController?.hide()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        EstadoBusqueda.Inactiva -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementoResultado(
    resultado: ResultadoBusqueda,
    iconoRes: Int,
    colorIcono: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colorIcono),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconoRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resultado.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = resultado.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
