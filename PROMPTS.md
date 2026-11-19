# PROMPTS.md — Registro de uso de IA (rama `mejora-ia`)

Herramienta: Gemini (Agente de Android Studio, modelo Gemini 3.6 Flash).
Regla que cumplí en todos los prompts: nunca incluí la API key (vive solo en `local.properties`, ignorado por git) y pedí respetar MVVM + Repository (la View solo conoce su ViewModel; el ViewModel no usa `Context`; el `Context` solo llega a los Repository mediante `CampusViewModelFactory`).

Después de cada respuesta revisé el código en disco **antes** de aceptar los cambios, y corregí lo que no cumplía las reglas o fallaba al probarlo.

| # | Prompt que escribí | Qué respondió Gemini | Qué cambié o descarté | Por qué |
|---|--------------------|----------------------|-----------------------|---------|
| 1 | Identidad visual propia (paleta "Bosque": verde, naranja, dorado; sin colores genéricos), tipografías DM Serif Display y Poppins, iconos por categoría, mapa con estilo propio y marcadores personalizados. | Creó `Color.kt`, `Type.kt`, `Theme.kt`, `CategoriaEstilo.kt`, iconos de categoría, `mapa_claro.json` / `mapa_oscuro.json` y rediseñó `MapaUbicacion`, `ListaLugares` y `PantallaMapa`. | `mapa_claro.json` tenía un `featureType` inválido ("geometry") y Google ignoraba todo el estilo: lo cambié a `"all"` con `elementType`. Ajusté el ancla (`anchor`) de los marcadores para que la punta cayera sobre la coordenada. Los archivos de fuente los puse yo. | Con el `featureType` inválido el mapa no cambiaba de estilo; con el ancla por defecto el pin quedaba desplazado del lugar real. |
| 2 | Una mascota guía (Chaski) que diga la distancia, los minutos a pie y el rumbo hacia el lugar. La lógica debe ir en el ViewModel, no en el Composable. | Creó `Rumbo.kt`, `MensajeGuia`, el flujo `mensajeGuia` en `MapaViewModel` y el Composable `ChaskiGuia` con burbuja y animaciones. | El dibujo de Chaski (vector) lo hice yo, no Gemini. Subí el margen inferior de Chaski porque tapaba el logo de Google. | El texto y el cálculo se deciden en el ViewModel y el Composable solo dibuja. El logo de Google debe quedar visible. |
| 3 | Pantalla de inicio: Chaski perdido, botón "Comenzar", interactiva. Después, un fondo de la ciudad de Lima (cielo, sol, nubes, combi). | Creó `PantallaInicio`, `CampusApp` y el estado `mostrandoInicio` / frases en el ViewModel. | Añadí `statusBarsPadding` / `navigationBarsPadding` (la pantalla ignoraba las barras del sistema), `aspectRatio` para que Chaski escalara y reubiqué la ruta punteada, que tapaba su cabeza. Las capas de Lima, el sol y la luna son vectores míos. | Con edge-to-edge el contenido quedaba bajo las barras; un vector sin `aspectRatio` no se agranda. |
| 4 | Buscador de lugares sin Places API: Repository nuevo con `Geocoder`, resultados del campus y "Otros lugares", y Chaski informando el destino. | Creó `BuscadorRepository` + `BuscadorRepositoryGeocoder`, `Busqueda.kt`, `BarraBusqueda`, y el estado de búsqueda y destino en el ViewModel. | Corregí cuatro cosas: (1) elegir un lugar del campus no limpiaba el destino externo y Chaski seguía hablando del anterior; (2) "No encontré ese lugar" salía mientras se escribía; (3) el Geocoder devolvía lugares de otros países: lo limité a una caja de Lima; (4) la cámara se animaba dos veces. | Eran fallos de comportamiento al probar en el emulador; la arquitectura de Gemini estaba bien. |
| 4b | (Lo hice yo, sin Gemini) Sugerencias mientras se escribe, como en Google Maps. | — | Añadí una pausa de 350 ms (`delay`) y una búsqueda automática cancelable en `cambiarConsulta`. Agregué Tecsup Santa Anita y 7 lugares de Lima a `LugaresRepositoryLocal`. | Antes había que escribir el nombre completo y pulsar la lupa. |
| 5 | Más contraste entre el mapa y el panel inferior (hoja con esquinas redondeadas, tarjetas más claras con franja de color por categoría) y un panel "Sugeridos" al tocar la barra de búsqueda. | Actualizó los colores y el tema, los JSON del mapa, `ListaLugares` (franja de color), `BarraBusqueda` (panel Sugeridos) y el flujo `sugeridos` en el ViewModel. | **No modificó `PantallaMapa.kt`** (la hoja inferior no existía y `sugeridos` no estaba conectado): lo terminé yo. Añadí `contentPadding` al mapa porque la hoja tapaba el logo de Google y el zoom. | Sin esa conexión la mejora no se veía en la app. |

## Mejoras y commits (rama `mejora-ia`)

| Mejora | Cambia ViewModel / Repository | Commit |
|--------|-------------------------------|--------|
| Identidad visual propia (tema "Bosque", fuentes, marcadores, mapa con estilo) | No (solo capa visual) | `7a66e90` feat(ui): aplica identidad visual propia con tema Bosque, fuentes y marcadores |
| Chaski, mascota guía con distancia, minutos y rumbo | Sí: `MapaViewModel` (`mensajeGuia`) y `util/Rumbo.kt` | `39c4ba2` feat(guia): agrega mascota Chaski con mensajes y direcciones |
| Pantalla de bienvenida interactiva | Sí: `MapaViewModel` (estado de inicio) | `8087923` feat(inicio): agrega pantalla de bienvenida interactiva con Chaski |
| Fondo animado de Lima (cielo, sol, luna) | No | `340581b` feat(inicio): agrega fondo animado de Lima con cielo, sol y luna |
| Corrección de tamaño de Chaski y ruta | No | `0ef1d6a` fix(inicio): corrige el tamaño de Chaski y la ruta de la pantalla de inicio |
| Buscador con Geocoder y sugerencias al escribir | Sí: **Repository nuevo** (`BuscadorRepository`) y `MapaViewModel` | `9ae7326` feat(busqueda): agrega buscador de lugares con Geocoder y sugerencias al escribir |
| Profundidad visual y lugares sugeridos | Sí: `MapaViewModel` (`sugeridos`) | `55f56fb` feat(ui): agrega profundidad visual y lugares sugeridos al buscar |

Total en la rama `mejora-ia`: 7 commits (la guía pide mínimo 3), cada uno nombra la mejora que introduce. La fase sin IA (rama `main`) tiene los 10 commits de la guía.

## Para explicar en la sustentación

**Buscador:** el ViewModel recibe una interfaz (`BuscadorRepository`), nunca el `Context`. La implementación `BuscadorRepositoryGeocoder` es la única que conoce el `Geocoder`, y recibe el `Context` a través de `CampusViewModelFactory`. Por eso se puede cambiar de Geocoder a otra fuente sin tocar el ViewModel ni la pantalla.
