package com.habilio.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

enum class OrdenResultados(val etiqueta: String) {
    RECIENTES("Recientes"),
    CALIFICACION("★ Mejor calificación"),
    TRABAJOS("Más trabajos comprobados")
}

@Composable
fun PantallaBuscadorCliente(
    trabajadores: List<PerfilUsuario>,
    evidencias: List<EvidenciaItem>,
    resenas: List<ResenaItem>,
    cargando: Boolean,
    alRefrescar: () -> Unit,
    alVerEspecialista: (PerfilUsuario) -> Unit,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alIrAMiPerfil: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var categoriaActiva by remember { mutableStateOf("Todos") }
    var zonaActiva by remember { mutableStateOf("Todas las zonas") }
    var orden by remember { mutableStateOf(OrdenResultados.RECIENTES) }
    // Acta #6: filtros combinados
    var dispActivas by remember { mutableStateOf(setOf<String>()) }
    var expMinima by remember { mutableIntStateOf(0) }
    var precioMaximo by remember { mutableIntStateOf(0) }
    // Acta #1: sección voluntaria; no se pide ni se guarda el género de nadie
    var soloMujeresEnOficios by remember { mutableStateOf(false) }

    val categorias = listOf("Todos") + CATALOGO_OFICIOS.map { it.nombre }

    val zonasDisponibles = listOf("Todas las zonas") + ZONAS_HABILIO

    // Cada filtro es una función para poder contar qué pasaría si se quitara (sugerencias sin resultados)
    fun cumpleCategoria(t: PerfilUsuario) = categoriaActiva == "Todos" ||
        t.oficio.contains(categoriaActiva, ignoreCase = true) ||
        t.especialidad.contains(categoriaActiva, ignoreCase = true)
    fun cumpleZona(t: PerfilUsuario) = zonaActiva == "Todas las zonas" ||
        t.zona.contains(zonaActiva, ignoreCase = true) ||
        t.direccion.contains(zonaActiva, ignoreCase = true)
    // Acta #5/#6: el texto también encuentra sinónimos ("tubero" -> Plomería) y no distingue acentos
    val oficioPorSinonimo = remember(busqueda) { oficioPorTexto(busqueda) }
    fun cumpleTexto(t: PerfilUsuario) = busqueda.isBlank() ||
        t.nombre.contains(busqueda, ignoreCase = true) ||
        t.oficio.contains(busqueda, ignoreCase = true) ||
        t.especialidad.contains(busqueda, ignoreCase = true) ||
        t.zona.contains(busqueda, ignoreCase = true) ||
        t.descripcion.contains(busqueda, ignoreCase = true) ||
        (oficioPorSinonimo != null && t.oficio == oficioPorSinonimo)
    fun cumpleDisp(t: PerfilUsuario) = dispActivas.isEmpty() ||
        dispActivas.any { d -> t.disponibilidad.contains(d, ignoreCase = true) }
    fun cumpleExp(t: PerfilUsuario) = expMinima == 0 || t.aniosExperiencia >= expMinima
    fun cumplePrecio(t: PerfilUsuario) = precioMaximo == 0 || (t.precioDesde > 0 && t.precioDesde <= precioMaximo)
    fun cumpleMujeres(t: PerfilUsuario) = !soloMujeresEnOficios || t.visibleTalentoMujeres

    val trabajadoresFiltrados = remember(trabajadores, busqueda, categoriaActiva, zonaActiva, dispActivas, expMinima, precioMaximo, soloMujeresEnOficios) {
        trabajadores.filter { t ->
            // Acta #4: una persona de 15 a 17 años no aparece hasta que su autorización esté verificada
            t.autorizacionEstado != "pendiente" && t.autorizacionEstado != "rechazada" &&
            cumpleCategoria(t) && cumpleZona(t) && cumpleTexto(t) && cumpleDisp(t) &&
                cumpleExp(t) && cumplePrecio(t) && cumpleMujeres(t)
        }
    }
    val hayFiltrosAvanzados = dispActivas.isNotEmpty() || expMinima > 0 || precioMaximo > 0 || soloMujeresEnOficios
    val hayAlgunFiltro = hayFiltrosAvanzados || busqueda.isNotBlank() || categoriaActiva != "Todos" || zonaActiva != "Todas las zonas"

    // Trabajos comprobados = trabajos del portafolio (un antes/después cuenta como uno solo)
    val trabajosPorTrabajador = remember(evidencias) {
        agruparEvidencias(evidencias).groupingBy { it.trabajadorId }.eachCount()
    }
    val resenasPorTrabajador = remember(resenas) {
        resenas.groupingBy { it.trabajadorId }.eachCount()
    }

    val resultados = remember(trabajadoresFiltrados, orden, trabajosPorTrabajador, resenasPorTrabajador) {
        // Sin reseñas no hay puntaje: esos perfiles quedan al final al ordenar por calificación
        fun puntaje(t: PerfilUsuario) = if ((resenasPorTrabajador[t.id] ?: 0) > 0) t.calificacion else -1.0
        fun cantidadTrabajos(t: PerfilUsuario) = trabajosPorTrabajador[t.id] ?: 0
        fun cantidadResenas(t: PerfilUsuario) = resenasPorTrabajador[t.id] ?: 0
        when (orden) {
            OrdenResultados.RECIENTES -> trabajadoresFiltrados
            OrdenResultados.CALIFICACION -> trabajadoresFiltrados.sortedWith(
                compareByDescending<PerfilUsuario> { puntaje(it) }
                    .thenByDescending { cantidadResenas(it) }
                    .thenByDescending { cantidadTrabajos(it) }
            )
            OrdenResultados.TRABAJOS -> trabajadoresFiltrados.sortedWith(
                compareByDescending<PerfilUsuario> { cantidadTrabajos(it) }
                    .thenByDescending { puntaje(it) }
                    .thenByDescending { cantidadResenas(it) }
            )
        }
    }

    val conEvidenciaReal = remember(trabajadoresFiltrados, trabajosPorTrabajador) {
        trabajadoresFiltrados.count { (trabajosPorTrabajador[it.id] ?: 0) > 0 }
    }

    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = zonaActiva, alAccionDerecha = alCambiarRol) },
        bottomBar = {
            HabilioBarraInferior(
                primerTabSeleccionado = true,
                etiquetaPrimero = "Explorar",
                iconoPrimero = Icons.Default.Search,
                alClickPrimero = alRefrescar,
                etiquetaSegundo = "Mi Perfil",
                iconoSegundo = Icons.Outlined.Person,
                alClickSegundo = alIrAMiPerfil
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("FILTRAR POR ZONA DE SERVICIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = HabilioTheme.RojoUrgente, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(zonaActiva, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                TextButton(onClick = alRefrescar) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar", color = HabilioTheme.AmarilloAcentoTexto, fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                zonasDisponibles.forEach { z ->
                    val sel = zonaActiva == z
                    AssistChip(
                        onClick = { zonaActiva = z },
                        label = { Text(z, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (sel) HabilioTheme.FondoAzulSuave else HabilioTheme.SuperficieTarjeta,
                            labelColor = if (sel) HabilioTheme.AzulVerificado else HabilioTheme.TextoSecundario
                        ),
                        border = BorderStroke(1.dp, if (sel) HabilioTheme.AzulIcono else HabilioTheme.BordeTarjeta)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Buscar oficio, nombre o colonia (ej. tubero, costurera)...", color = HabilioTheme.TextoPlaceholder, fontSize = 14.sp)
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HabilioTheme.TextoSecundario) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HabilioTheme.SuperficieTarjeta,
                    unfocusedContainerColor = HabilioTheme.SuperficieTarjeta,
                    focusedBorderColor = HabilioTheme.AmarilloPrimario,
                    unfocusedBorderColor = HabilioTheme.BordeInput,
                    cursorColor = HabilioTheme.AmarilloAcentoTexto
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorias.forEach { cat ->
                    val seleccionado = categoriaActiva == cat
                    FilterChip(
                        selected = seleccionado,
                        onClick = { categoriaActiva = cat },
                        label = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.AmarilloPrimario,
                            selectedLabelColor = HabilioTheme.TextoSobreAmarillo,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = seleccionado,
                            borderColor = HabilioTheme.BordeTarjeta
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("DISPONIBILIDAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DISPONIBILIDADES_HABILIO.forEach { d ->
                    val sel = d in dispActivas
                    FilterChip(
                        selected = sel,
                        onClick = { dispActivas = if (sel) dispActivas - d else dispActivas + d },
                        label = { Text(d, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.FondoAzulSuave,
                            selectedLabelColor = HabilioTheme.AzulVerificado,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = sel,
                            borderColor = HabilioTheme.BordeInput, selectedBorderColor = HabilioTheme.AzulIcono
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("EXPERIENCIA DECLARADA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Cualquiera", 2 to "2+ años", 5 to "5+ años", 10 to "10+ años").forEach { (valor, etiqueta) ->
                    val sel = expMinima == valor
                    FilterChip(
                        selected = sel, onClick = { expMinima = valor },
                        label = { Text(etiqueta, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.FondoAzulSuave,
                            selectedLabelColor = HabilioTheme.AzulVerificado,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = sel,
                            borderColor = HabilioTheme.BordeInput, selectedBorderColor = HabilioTheme.AzulIcono
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("PRECIO ORIENTATIVO (DESDE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Cualquiera", 300 to "Hasta $300", 600 to "Hasta $600", 1000 to "Hasta $1,000").forEach { (valor, etiqueta) ->
                    val sel = precioMaximo == valor
                    FilterChip(
                        selected = sel, onClick = { precioMaximo = valor },
                        label = { Text(etiqueta, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.FondoAzulSuave,
                            selectedLabelColor = HabilioTheme.AzulVerificado,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = sel,
                            borderColor = HabilioTheme.BordeInput, selectedBorderColor = HabilioTheme.AzulIcono
                        )
                    )
                }
            }
            if (precioMaximo > 0) {
                Text("Solo aparecen quienes registraron un precio orientativo.", fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Acta #1 y #26: sección voluntaria de visibilidad del talento de mujeres en oficios
            FilterChip(
                selected = soloMujeresEnOficios,
                onClick = { soloMujeresEnOficios = !soloMujeresEnOficios },
                leadingIcon = { Icon(Icons.Default.Diversity1, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text("Mujeres en oficios", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HabilioTheme.FondoAmarilloSuave,
                    selectedLabelColor = HabilioTheme.AmarilloAcentoTexto,
                    containerColor = HabilioTheme.SuperficieTarjeta,
                    labelColor = HabilioTheme.TextoPrimario
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = soloMujeresEnOficios,
                    borderColor = HabilioTheme.BordeInput, selectedBorderColor = HabilioTheme.AmarilloAcentoTexto
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("ORDENAR POR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSecundario)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrdenResultados.values().forEach { opcion ->
                    val seleccionado = orden == opcion
                    FilterChip(
                        selected = seleccionado,
                        onClick = { orden = opcion },
                        label = { Text(opcion.etiqueta, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.FondoAzulSuave,
                            selectedLabelColor = HabilioTheme.AzulVerificado,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = seleccionado,
                            borderColor = HabilioTheme.BordeInput,
                            selectedBorderColor = HabilioTheme.AzulIcono
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HabilioTarjeta(
                colorFondo = HabilioTheme.FondoAzulSuave,
                colorBorde = HabilioTheme.AzulIcono.copy(alpha = 0.4f),
                radioEsquina = 10.dp,
                paddingInterno = 12.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = HabilioTheme.AzulIcono, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${resultados.size} especialistas encontrados ($conEvidenciaReal con portafolio fotográfico)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EstadoCargaOVacio(
                cargando = cargando,
                estaVacio = false,
                mensajeVacio = ""
            )

            // Acta #6: escenario sin coincidencias. Se explica por qué y qué cambiar, con números reales.
            if (!cargando && resultados.isEmpty()) {
                HabilioTarjeta(paddingInterno = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (trabajadores.isEmpty()) "Todavía no hay personas registradas." else "No encontramos a nadie con esa combinación.",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario
                        )
                    }
                    if (trabajadores.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        val sugerencias = buildList {
                            if (soloMujeresEnOficios) {
                                val n = trabajadores.count { cumpleCategoria(it) && cumpleZona(it) && cumpleTexto(it) && cumpleDisp(it) && cumpleExp(it) && cumplePrecio(it) }
                                if (n > 0) add("Quita \"Mujeres en oficios\": hay $n resultado(s). Esa sección es voluntaria y solo incluye perfiles que decidieron aparecer.")
                            }
                            if (precioMaximo > 0) {
                                val n = trabajadores.count { cumpleCategoria(it) && cumpleZona(it) && cumpleTexto(it) && cumpleDisp(it) && cumpleExp(it) && cumpleMujeres(it) }
                                if (n > 0) add("Quita el filtro de precio: hay $n resultado(s). Muchas personas aún no registran precio orientativo.")
                            }
                            if (expMinima > 0) {
                                val n = trabajadores.count { cumpleCategoria(it) && cumpleZona(it) && cumpleTexto(it) && cumpleDisp(it) && cumplePrecio(it) && cumpleMujeres(it) }
                                if (n > 0) add("Baja la experiencia mínima: hay $n resultado(s).")
                            }
                            if (dispActivas.isNotEmpty()) {
                                val n = trabajadores.count { cumpleCategoria(it) && cumpleZona(it) && cumpleTexto(it) && cumpleExp(it) && cumplePrecio(it) && cumpleMujeres(it) }
                                if (n > 0) add("Quita la disponibilidad: hay $n resultado(s).")
                            }
                            if (zonaActiva != "Todas las zonas") {
                                val n = trabajadores.count { cumpleCategoria(it) && cumpleTexto(it) && cumpleDisp(it) && cumpleExp(it) && cumplePrecio(it) && cumpleMujeres(it) }
                                if (n > 0) add("Prueba \"Todas las zonas\": hay $n resultado(s) en otras colonias.")
                            }
                            if (oficioPorSinonimo != null && categoriaActiva != "Todos" && categoriaActiva != oficioPorSinonimo) {
                                add("Tu búsqueda parece ser de $oficioPorSinonimo, pero tienes elegida la categoría $categoriaActiva.")
                            }
                            if (isEmpty()) add("Prueba con otra palabra (por ejemplo el nombre del oficio) o amplía la zona.")
                        }
                        sugerencias.forEach {
                            Text("• $it", fontSize = 13.sp, color = HabilioTheme.TextoSecundario, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        if (hayAlgunFiltro) {
                            Spacer(Modifier.height(10.dp))
                            BotonPrimarioHabilio(texto = "Limpiar todos los filtros", alClick = {
                                busqueda = ""; categoriaActiva = "Todos"; zonaActiva = "Todas las zonas"
                                dispActivas = emptySet(); expMinima = 0; precioMaximo = 0; soloMujeresEnOficios = false
                            })
                        }
                    }
                }
            }

            if (!cargando) {
                // Acta #23: paginación de resultados. Solo se dibujan (y cargan sus fotos) los primeros
                // TAMANO_PAGINA; "Ver más" agrega la siguiente página. Al cambiar un filtro se vuelve a la primera.
                var paginasVisibles by remember(resultados) { mutableIntStateOf(1) }
                val evidenciasPorTrabajador = remember(evidencias) { evidencias.groupBy { it.trabajadorId } }
                val resenasPorPersona = remember(resenas) { resenas.groupingBy { it.trabajadorId }.eachCount() }
                val visibles = resultados.take(paginasVisibles * TAMANO_PAGINA)
                visibles.forEach { esp ->
                    TarjetaEspecialistaBuscador(
                        especialista = esp,
                        evidencias = evidenciasPorTrabajador[esp.id].orEmpty(),
                        cantidadResenas = resenasPorPersona[esp.id] ?: 0,
                        alClickFotoEvidencia = alClickFotoEvidencia,
                        alVerDetalle = { alVerEspecialista(esp) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                BotonVerMas(mostrados = visibles.size, total = resultados.size, alClick = { paginasVisibles++ })
            }
        }
    }
}

@Composable
fun TarjetaEspecialistaBuscador(
    especialista: PerfilUsuario,
    evidencias: List<EvidenciaItem>,
    cantidadResenas: Int,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alVerDetalle: () -> Unit
) {
    val tieneEvidenciaComprobable = evidencias.isNotEmpty()
    val trabajos = remember(evidencias) { agruparEvidencias(evidencias) }

    HabilioTarjeta {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            AvatarUsuario(mostrarPuntoDisponible = false, fotoUrl = especialista.fotoUrl)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(especialista.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (tieneEvidenciaComprobable) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Tiene trabajos publicados",
                            tint = HabilioTheme.AzulIcono,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = especialista.oficio +
                            if (especialista.especialidad.isNotBlank()) " • ${especialista.especialidad}" else "",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HabilioTheme.AmarilloAcentoTexto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("📍 ${especialista.zona}", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Text(textoDisponibilidad(especialista), fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                if (especialista.visibleTalentoMujeres) {
                    Text("Mujeres en oficios", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.AmarilloAcentoTexto)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (tieneEvidenciaComprobable) {
                    HabilioBadge(
                        texto = "TRABAJOS PUBLICADOS",
                        colorTexto = HabilioTheme.AmarilloAcentoTexto,
                        colorFondo = HabilioTheme.FondoAmarilloSuave
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★", color = HabilioTheme.AmarilloAcentoTexto, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    if (cantidadResenas > 0) {
                        Text(String.format("%.1f", especialista.calificacion), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("($cantidadResenas)", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    } else {
                        Text("Sin reseñas", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = construirResumenLimpio(especialista),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (index in 0 until 3) {
                MiniaturaTrabajo(
                    trabajo = trabajos.getOrNull(index),
                    alClickFoto = alClickFotoEvidencia,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✓ ${especialista.modalidad}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = HabilioTheme.TextoSecundario,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = alVerDetalle,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.AmarilloPrimario,
                    contentColor = HabilioTheme.TextoSobreAmarillo
                ),
                modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))
            ) {
                Text("Ver especialista", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

fun construirResumenLimpio(esp: PerfilUsuario): String {
    val tallerTexto = if (esp.nombreTaller.isNotBlank()) " (${esp.nombreTaller})" else ""
    return "Especialista en ${esp.oficio}$tallerTexto con atención en ${esp.zona}."
}
