package com.habilio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabilioApp()
        }
    }
}
// ============================================================================
// 1. SISTEMA DE DISEÑO Y TOKENS DE COLOR (Accesibilidad WCAG AA mejorada)
// ============================================================================
object HabilioTheme {
    val FondoPrincipal = Color(0xFF0E1013)
    val SuperficieTarjeta = Color(0xFF181B20)
    val SuperficieInput = Color(0xFF121418)
    val SuperficieElevada = Color(0xFF22262E)
    val BordeTarjeta = Color(0xFF2A2F3A)

    val AmarilloPrimario = Color(0xFFF5B811)
    val TextoSobreAmarillo = Color(0xFF111315)

    val VerdeExito = Color(0xFF2ECC8F)
    val FondoVerdeSuave = Color(0xFF132A23)

    val AzulVerificado = Color(0xFF3898FF)
    val FondoAzulSuave = Color(0xFF132235)

    val RojoUrgente = Color(0xFFFF6B6B)
    val FondoRojoSuave = Color(0xFF2D181B)

    // Colores de texto con alto contraste para legibilidad
    val TextoPrimario = Color(0xFFF4F6F8)
    val TextoSecundario = Color(0xFFB0B8C4) // Mejorado respecto al gris oscuro del mockup
    val TextoPlaceholder = Color(0xFF8A94A6)
}

// ============================================================================
// 2. RUTAS Y CONTROLADOR DE FLUJO DE NAVEGACIÓN
// ============================================================================
enum class RutaHabilio {
    INICIO_ROL,             // 1. Inicio.jpg
    LOGIN_CLIENTE,          // 2. Inicio-Cliente.jpg
    REGISTRO_CLIENTE,       // 3. Registro-Cliente.jpg
    LOGIN_TRABAJADOR,       // 4. Inicio-Trabajador.jpg
    REGISTRO_TRABAJADOR,    // 5. Registro-Trabajador.jpg
    BUSCADOR_CLIENTE,       // 6. Buscador.jpg
    DETALLE_ESPECIALISTA,   // 7. Detalle.jpg
    PERFIL_CLIENTE,         // 8. Perfil-Cliente.jpg
    SOLICITUDES_TRABAJADOR, // 9. Solicitudes.jpg
    TALLER_TRABAJADOR       // 10. Trabajador perfil.jpg
}

@Composable
fun HabilioApp() {
    // Estado central de navegación.
    // NOTA: Si usas Jetpack Navigation Compose (NavHost), puedes reemplazar este
    // 'when (pantallaActual)' por composables de NavController en este mismo bloque.
    var pantallaActual by remember { mutableStateOf(RutaHabilio.INICIO_ROL) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = HabilioTheme.FondoPrincipal,
            surface = HabilioTheme.SuperficieTarjeta,
            primary = HabilioTheme.AmarilloPrimario,
            onPrimary = HabilioTheme.TextoSobreAmarillo,
            onBackground = HabilioTheme.TextoPrimario,
            onSurface = HabilioTheme.TextoPrimario
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = HabilioTheme.FondoPrincipal
        ) {
            // ====================================================================
            // CENTRAL DE NAVEGACIÓN: CAMBIA AQUÍ LAS RUTAS SI AJUSTAS EL FLUJO
            // ====================================================================
            when (pantallaActual) {
                RutaHabilio.INICIO_ROL -> PantallaInicioRol(
                    alElegirCliente = { pantallaActual = RutaHabilio.LOGIN_CLIENTE }, // CAMBIAR FLUJO AQUÍ
                    alElegirTrabajador = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR } // CAMBIAR FLUJO AQUÍ
                )

                RutaHabilio.LOGIN_CLIENTE -> PantallaLoginCliente(
                    alIrARegistro = { pantallaActual = RutaHabilio.REGISTRO_CLIENTE },
                    alIniciarSesion = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE }, // CAMBIAR FLUJO AQUÍ
                    alCambiarATrabajador = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.REGISTRO_CLIENTE -> PantallaRegistroCliente(
                    alIrALogin = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                    alCrearCuenta = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE }, // CAMBIAR FLUJO AQUÍ
                    alCambiarATrabajador = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.LOGIN_TRABAJADOR -> PantallaLoginTrabajador(
                    alIrARegistro = { pantallaActual = RutaHabilio.REGISTRO_TRABAJADOR },
                    alIniciarSesion = { pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR }, // CAMBIAR FLUJO AQUÍ
                    alCambiarACliente = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.REGISTRO_TRABAJADOR -> PantallaRegistroTrabajador(
                    alIrALogin = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                    alRegistrarTrabajador = { pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR }, // CAMBIAR FLUJO AQUÍ
                    alCambiarACliente = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                    alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.BUSCADOR_CLIENTE -> PantallaBuscadorCliente(
                    alVerEspecialista = { pantallaActual = RutaHabilio.DETALLE_ESPECIALISTA }, // CAMBIAR FLUJO AQUÍ
                    alIrAMiPerfil = { pantallaActual = RutaHabilio.PERFIL_CLIENTE },
                    alCambiarRol = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.DETALLE_ESPECIALISTA -> PantallaDetalleEspecialista(
                    alVolverAlListado = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                    alSolicitarServicio = { pantallaActual = RutaHabilio.PERFIL_CLIENTE }, // CAMBIAR FLUJO AQUÍ
                    alIrAExplorar = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                    alIrAMiPerfil = { pantallaActual = RutaHabilio.PERFIL_CLIENTE }
                )

                RutaHabilio.PERFIL_CLIENTE -> PantallaPerfilCliente(
                    alVerDetalleServicio = { pantallaActual = RutaHabilio.DETALLE_ESPECIALISTA }, // CAMBIAR FLUJO AQUÍ
                    alIrAExplorar = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                    alCambiarRol = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.SOLICITUDES_TRABAJADOR -> PantallaSolicitudesTrabajador(
                    alIrAMiTaller = { pantallaActual = RutaHabilio.TALLER_TRABAJADOR }, // CAMBIAR FLUJO AQUÍ
                    alCambiarRol = { pantallaActual = RutaHabilio.INICIO_ROL }
                )

                RutaHabilio.TALLER_TRABAJADOR -> PantallaTallerTrabajador(
                    alIrASolicitudes = { pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR }, // CAMBIAR FLUJO AQUÍ
                    alCambiarRol = { pantallaActual = RutaHabilio.INICIO_ROL }
                )
            }
        }
    }
}

// ============================================================================
// 3. COMPONENTES COMPARTIDOS (Consistencia Visual y Accesibilidad)
// ============================================================================

/**
 * Barra superior unificada de Habilio con logo, ubicación y botón opcional de salida/cambio de rol.
 */
@Composable
fun HabilioTopBar(
    subtituloUbicacion: String? = "Pénjamo, Gto.",
    modoTrabajador: Boolean = false,
    alAccionDerecha: (() -> Unit)? = null,
    iconoDerecha: ImageVector = Icons.Default.SwapHoriz
) {
    Surface(
        color = HabilioTheme.FondoPrincipal,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ícono de marca Habilio (Casa + Herramientas)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieTarjeta)
                            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = "Logo de Habilio",
                            tint = HabilioTheme.AmarilloPrimario,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Habilio",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HabilioTheme.TextoPrimario
                        )
                        if (subtituloUbicacion != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = subtituloUbicacion,
                                    fontSize = 13.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                                if (modoTrabajador) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    HabilioBadge(
                                        texto = "● Modo Trabajador",
                                        colorTexto = HabilioTheme.VerdeExito,
                                        colorFondo = HabilioTheme.FondoVerdeSuave
                                    )
                                }
                            }
                        }
                    }
                }

                if (alAccionDerecha != null) {
                    IconButton(
                        onClick = alAccionDerecha,
                        modifier = Modifier
                            .size(48.dp) // Área táctil accesible mínima de 48dp
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieTarjeta)
                    ) {
                        Icon(
                            imageVector = iconoDerecha,
                            contentDescription = "Cambiar modo o volver al inicio",
                            tint = HabilioTheme.TextoSecundario
                        )
                    }
                }
            }
            HorizontalDivider(color = HabilioTheme.BordeTarjeta, thickness = 1.dp)
        }
    }
}

/**
 * Encabezado centrado para pantallas de Login y Registro.
 */
@Composable
fun EncabezadoAutenticacion(
    titulo: String,
    subtitulo: String,
    alVolver: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = alVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver a selección de rol",
                    tint = HabilioTheme.TextoSecundario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cambiar rol", color = HabilioTheme.TextoSecundario, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HabilioTheme.SuperficieTarjeta)
                .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Handyman,
                contentDescription = "Logo Habilio",
                tint = HabilioTheme.AmarilloPrimario,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioBadge(
            texto = "📍 PÉNJAMO, GTO. Y REGIÓN",
            colorTexto = HabilioTheme.VerdeExito,
            colorFondo = HabilioTheme.FondoVerdeSuave
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = titulo,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HabilioTheme.TextoPrimario,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitulo,
            fontSize = 14.sp,
            color = HabilioTheme.TextoSecundario,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/**
 * Selector de pestañas (Iniciar Sesión / Crear Cuenta) consistente en flujos de autenticación.
 */
@Composable
fun SelectorModoAutenticacion(
    esLoginSeleccionado: Boolean,
    alSeleccionarLogin: () -> Unit,
    alSeleccionarRegistro: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HabilioTheme.SuperficieInput)
            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Button(
            onClick = alSeleccionarLogin,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (esLoginSeleccionado) HabilioTheme.AmarilloPrimario else Color.Transparent,
                contentColor = if (esLoginSeleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoSecundario
            ),
            elevation = null
        ) {
            Text("Iniciar Sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Button(
            onClick = alSeleccionarRegistro,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!esLoginSeleccionado) HabilioTheme.AmarilloPrimario else Color.Transparent,
                contentColor = if (!esLoginSeleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoSecundario
            ),
            elevation = null
        ) {
            Text("Crear Cuenta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/**
 * Campo de texto accesible con etiquetas claras, prefijo opcional y soporte para contraseña.
 */
@Composable
fun HabilioCampoTexto(
    etiqueta: String,
    etiquetaDerecha: String? = null,
    colorEtiquetaDerecha: Color = HabilioTheme.TextoSecundario,
    accionEtiquetaDerecha: (() -> Unit)? = null,
    valor: String,
    alCambiarValor: (String) -> Unit,
    placeholder: String,
    iconoInicial: ImageVector? = null,
    prefijoTexto: String? = null,
    esContrasena: Boolean = false
) {
    var mostrarContrasena by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = etiqueta,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HabilioTheme.TextoPrimario
            )
            if (etiquetaDerecha != null) {
                Text(
                    text = etiquetaDerecha,
                    fontSize = 12.sp,
                    color = colorEtiquetaDerecha,
                    fontWeight = FontWeight.Medium,
                    modifier = if (accionEtiquetaDerecha != null) {
                        Modifier
                            .clickable(role = Role.Button, onClick = accionEtiquetaDerecha)
                            .padding(vertical = 4.dp)
                    } else Modifier
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = valor,
            onValueChange = alCambiarValor,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = HabilioTheme.TextoPlaceholder,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = if (prefijoTexto != null) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    ) {
                        Text("🇲🇽", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prefijoTexto,
                            color = HabilioTheme.TextoPrimario,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(HabilioTheme.BordeTarjeta)
                        )
                    }
                }
            } else if (iconoInicial != null) {
                {
                    Icon(
                        imageVector = iconoInicial,
                        contentDescription = null,
                        tint = HabilioTheme.TextoSecundario
                    )
                }
            } else null,
            trailingIcon = if (esContrasena) {
                {
                    IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                        Icon(
                            imageVector = if (mostrarContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = HabilioTheme.TextoSecundario
                        )
                    }
                }
            } else null,
            visualTransformation = if (esContrasena && !mostrarContrasena) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HabilioTheme.SuperficieInput,
                unfocusedContainerColor = HabilioTheme.SuperficieInput,
                focusedBorderColor = HabilioTheme.AmarilloPrimario,
                unfocusedBorderColor = HabilioTheme.BordeTarjeta,
                focusedTextColor = HabilioTheme.TextoPrimario,
                unfocusedTextColor = HabilioTheme.TextoPrimario
            )
        )
    }
}

/**
 * Etiqueta / Insignia (Badge) reutilizable con buen contraste.
 */
@Composable
fun HabilioBadge(
    texto: String,
    colorTexto: Color,
    colorFondo: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = colorFondo,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Botón primario amarillo de alto impacto y altura accesible (52dp).
 */
@Composable
fun BotonPrimarioHabilio(
    texto: String,
    alClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarFlecha: Boolean = false
) {
    Button(
        onClick = alClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HabilioTheme.AmarilloPrimario,
            contentColor = HabilioTheme.TextoSobreAmarillo
        )
    ) {
        Text(
            text = texto,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        if (mostrarFlecha) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Barra de navegación inferior para el rol de Cliente (Explorar | Mi Perfil).
 */
@Composable
fun BarraInferiorCliente(
    enExplorar: Boolean,
    alIrAExplorar: () -> Unit,
    alIrAMiPerfil: () -> Unit
) {
    NavigationBar(
        containerColor = HabilioTheme.FondoPrincipal,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = enExplorar,
            onClick = alIrAExplorar,
            icon = { Icon(Icons.Default.Search, contentDescription = "Explorar oficios") },
            label = { Text("Explorar", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HabilioTheme.AmarilloPrimario,
                selectedTextColor = HabilioTheme.AmarilloPrimario,
                unselectedIconColor = HabilioTheme.TextoSecundario,
                unselectedTextColor = HabilioTheme.TextoSecundario,
                indicatorColor = HabilioTheme.SuperficieTarjeta
            )
        )
        NavigationBarItem(
            selected = !enExplorar,
            onClick = alIrAMiPerfil,
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Mi perfil de cliente") },
            label = { Text("Mi Perfil", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HabilioTheme.AmarilloPrimario,
                selectedTextColor = HabilioTheme.AmarilloPrimario,
                unselectedIconColor = HabilioTheme.TextoSecundario,
                unselectedTextColor = HabilioTheme.TextoSecundario,
                indicatorColor = HabilioTheme.SuperficieTarjeta
            )
        )
    }
}

/**
 * Barra de navegación inferior para el rol de Trabajador (Solicitudes | Mi Taller).
 */
@Composable
fun BarraInferiorTrabajador(
    enSolicitudes: Boolean,
    alIrASolicitudes: () -> Unit,
    alIrAMiTaller: () -> Unit
) {
    NavigationBar(
        containerColor = HabilioTheme.FondoPrincipal,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = enSolicitudes,
            onClick = alIrASolicitudes,
            icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Solicitudes de trabajo") },
            label = { Text("Solicitudes", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HabilioTheme.AmarilloPrimario,
                selectedTextColor = HabilioTheme.AmarilloPrimario,
                unselectedIconColor = HabilioTheme.TextoSecundario,
                unselectedTextColor = HabilioTheme.TextoSecundario,
                indicatorColor = HabilioTheme.SuperficieTarjeta
            )
        )
        NavigationBarItem(
            selected = !enSolicitudes,
            onClick = alIrAMiTaller,
            icon = { Icon(Icons.Default.Build, contentDescription = "Mi Taller y Portafolio") },
            label = { Text("Mi Taller", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HabilioTheme.AmarilloPrimario,
                selectedTextColor = HabilioTheme.AmarilloPrimario,
                unselectedIconColor = HabilioTheme.TextoSecundario,
                unselectedTextColor = HabilioTheme.TextoSecundario,
                indicatorColor = HabilioTheme.SuperficieTarjeta
            )
        )
    }
}

// ============================================================================
// 4. PANTALLA 1: SELECCIÓN DE ROL (Inicio.jpg)
// ============================================================================
@Composable
fun PantallaInicioRol(
    alElegirCliente: () -> Unit,
    alElegirTrabajador: () -> Unit
) {
    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = null) },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta central del logo
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.size(100.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Handyman,
                        contentDescription = null,
                        tint = HabilioTheme.AmarilloPrimario,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Habilio",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = HabilioTheme.TextoPrimario
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HabilioBadge(
                texto = "TALENTO LOCAL COMPROBABLE",
                colorTexto = HabilioTheme.AzulVerificado,
                colorFondo = HabilioTheme.FondoAzulSuave
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Demuestra tu oficio con hechos, no con un CV",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Elige cómo deseas ingresar a Habilio para conectar habilidades reales con oportunidades en tu comunidad.",
                fontSize = 14.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tarjeta de Rol: Cliente
            TarjetaSeleccionRol(
                titulo = "Cliente",
                descripcion = "Buscar trabajadores por oficio, revisar fotos de sus trabajos y contratar con seguridad.",
                icono = Icons.Default.Search,
                colorIcono = HabilioTheme.AmarilloPrimario,
                colorFondoIcono = Color(0xFF332910),
                alClick = alElegirCliente
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Rol: Trabajador
            TarjetaSeleccionRol(
                titulo = "Trabajador",
                descripcion = "Responder preguntas sencillas, subir fotos de mis trabajos y recibir clientes.",
                icono = Icons.Default.Build,
                colorIcono = HabilioTheme.AzulVerificado,
                colorFondoIcono = HabilioTheme.FondoAzulSuave,
                alClick = alElegirTrabajador
            )
        }
    }
}

@Composable
private fun TarjetaSeleccionRol(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    colorIcono: Color,
    colorFondoIcono: Color,
    alClick: () -> Unit
) {
    Surface(
        onClick = alClick,
        shape = RoundedCornerShape(16.dp),
        color = HabilioTheme.SuperficieTarjeta,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorFondoIcono),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HabilioTheme.TextoPrimario
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = descripcion,
                    fontSize = 14.sp,
                    color = HabilioTheme.TextoSecundario,
                    lineHeight = 19.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = HabilioTheme.TextoSecundario
            )
        }
    }
}

// ============================================================================
// 5. PANTALLA 2: LOGIN CLIENTE (Inicio-Cliente.jpg)
// ============================================================================
@Composable
fun PantallaLoginCliente(
    alIrARegistro: () -> Unit,
    alIniciarSesion: () -> Unit,
    alCambiarATrabajador: () -> Unit,
    alVolverInicio: () -> Unit
) {
    var telefonoOCorreo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Bienvenido a Habilio",
            subtitulo = "Encuentra y contrata a los mejores maestros de oficios certificados con evidencia real de trabajo.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = true,
            alSeleccionarLogin = {},
            alSeleccionarRegistro = alIrARegistro
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Formulario principal
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HabilioTheme.SuperficieTarjeta,
            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                HabilioCampoTexto(
                    etiqueta = "Teléfono móvil o Correo",
                    etiquetaDerecha = "WhatsApp / SMS directo",
                    valor = telefonoOCorreo,
                    alCambiarValor = { telefonoOCorreo = it },
                    placeholder = "469 123 4567 o tu@correo.com",
                    prefijoTexto = "+52"
                )

                Spacer(modifier = Modifier.height(16.dp))

                HabilioCampoTexto(
                    etiqueta = "Contraseña",
                    etiquetaDerecha = "¿La olvidaste?",
                    colorEtiquetaDerecha = HabilioTheme.AmarilloPrimario,
                    accionEtiquetaDerecha = {},
                    valor = contrasena,
                    alCambiarValor = { contrasena = it },
                    placeholder = "••••••••••••",
                    iconoInicial = Icons.Outlined.Lock,
                    esContrasena = true
                )

                Spacer(modifier = Modifier.height(22.dp))

                BotonPrimarioHabilio(
                    texto = "Continuar como Cliente",
                    alClick = alIniciarSesion,
                    mostrarFlecha = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tarjeta inferior para cambiar a Trabajador
        TarjetaCambioATrabajador(alClick = alCambiarATrabajador)
    }
}

@Composable
private fun TarjetaCambioATrabajador(alClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HabilioTheme.SuperficieTarjeta,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = HabilioTheme.AmarilloPrimario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "¿Haces trabajos de oficio?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = HabilioTheme.TextoPrimario
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Si eres albañil, plomero, electricista o carpintero en Guanajuato:",
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = alClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HabilioTheme.SuperficieElevada,
                    contentColor = HabilioTheme.TextoPrimario
                )
            ) {
                Text("Ingresar como Trabajador", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ============================================================================
// 6. PANTALLA 3: REGISTRO CLIENTE (Registro-Cliente.jpg)
// ============================================================================
@Composable
fun PantallaRegistroCliente(
    alIrALogin: () -> Unit,
    alCrearCuenta: () -> Unit,
    alCambiarATrabajador: () -> Unit,
    alVolverInicio: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Bienvenido a Habilio",
            subtitulo = "Encuentra y contrata a los mejores maestros de oficios certificados con evidencia real de trabajo.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = false,
            alSeleccionarLogin = alIrALogin,
            alSeleccionarRegistro = {}
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HabilioTheme.SuperficieTarjeta,
            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                HabilioCampoTexto(
                    etiqueta = "Nombre Completo",
                    etiquetaDerecha = "Requerido",
                    colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                    valor = nombre,
                    alCambiarValor = { nombre = it },
                    placeholder = "Ej. Juan Pérez o Familia López",
                    iconoInicial = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(14.dp))

                HabilioCampoTexto(
                    etiqueta = "Teléfono móvil (WhatsApp)",
                    etiquetaDerecha = "Para confirmaciones",
                    colorEtiquetaDerecha = HabilioTheme.VerdeExito,
                    valor = telefono,
                    alCambiarValor = { telefono = it },
                    placeholder = "469 123 4567",
                    prefijoTexto = "+52"
                )

                Spacer(modifier = Modifier.height(14.dp))

                HabilioCampoTexto(
                    etiqueta = "Correo electrónico",
                    etiquetaDerecha = "Opcional",
                    valor = correo,
                    alCambiarValor = { correo = it },
                    placeholder = "tu@correo.com",
                    iconoInicial = Icons.Outlined.Email
                )

                Spacer(modifier = Modifier.height(14.dp))

                HabilioCampoTexto(
                    etiqueta = "Crear Contraseña",
                    valor = contrasena,
                    alCambiarValor = { contrasena = it },
                    placeholder = "Mínimo 8 caracteres",
                    iconoInicial = Icons.Outlined.Lock,
                    esContrasena = true
                )

                Spacer(modifier = Modifier.height(22.dp))

                BotonPrimarioHabilio(
                    texto = "Crear Cuenta de Cliente",
                    alClick = alCrearCuenta,
                    mostrarFlecha = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        TarjetaCambioATrabajador(alClick = alCambiarATrabajador)
    }
}

// ============================================================================
// 7. PANTALLA 4: LOGIN TRABAJADOR (Inicio-Trabajador.jpg)
// ============================================================================
@Composable
fun PantallaLoginTrabajador(
    alIrARegistro: () -> Unit,
    alIniciarSesion: () -> Unit,
    alCambiarACliente: () -> Unit,
    alVolverInicio: () -> Unit
) {
    var telefonoOCorreo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Bienvenido a Habilio",
            subtitulo = "Encuentra y contrata a los mejores maestros de oficios certificados con evidencia real de trabajo.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = true,
            alSeleccionarLogin = {},
            alSeleccionarRegistro = alIrARegistro
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HabilioTheme.SuperficieTarjeta,
            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                HabilioCampoTexto(
                    etiqueta = "Teléfono móvil o Correo",
                    etiquetaDerecha = "WhatsApp / SMS directo",
                    valor = telefonoOCorreo,
                    alCambiarValor = { telefonoOCorreo = it },
                    placeholder = "469 123 4567 o tu@correo.com",
                    prefijoTexto = "+52"
                )

                Spacer(modifier = Modifier.height(16.dp))

                HabilioCampoTexto(
                    etiqueta = "Contraseña",
                    etiquetaDerecha = "¿La olvidaste?",
                    colorEtiquetaDerecha = HabilioTheme.AmarilloPrimario,
                    accionEtiquetaDerecha = {},
                    valor = contrasena,
                    alCambiarValor = { contrasena = it },
                    placeholder = "••••••••••••",
                    iconoInicial = Icons.Outlined.Lock,
                    esContrasena = true
                )

                Spacer(modifier = Modifier.height(22.dp))

                BotonPrimarioHabilio(
                    texto = "Continuar como trabajador",
                    alClick = alIniciarSesion,
                    mostrarFlecha = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        TarjetaCambioACliente(alClick = alCambiarACliente)
    }
}

@Composable
private fun TarjetaCambioACliente(alClick: () -> Unit) {
    Surface(
        onClick = alClick,
        shape = RoundedCornerShape(16.dp),
        color = HabilioTheme.SuperficieTarjeta,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¿Buscas contratar servicios para tu hogar?",
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ingresar o registrarme como Cliente >",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloPrimario
            )
        }
    }
}

// ============================================================================
// 8. PANTALLA 5: REGISTRO TRABAJADOR (Registro-Trabajador.jpg)
// ============================================================================
@Composable
fun PantallaRegistroTrabajador(
    alIrALogin: () -> Unit,
    alRegistrarTrabajador: () -> Unit,
    alCambiarACliente: () -> Unit,
    alVolverInicio: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var nombreTaller by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var oficioSeleccionado by remember { mutableStateOf("Albañilería") }
    var especialidad by remember { mutableStateOf("") }
    var zonaServicio by remember { mutableStateOf("Pénjamo Centro, Santa Ana Pacueco y alrededores") }
    var modalidadSeleccionada by remember { mutableStateOf(0) }
    var direccionTaller by remember { mutableStateOf("") }

    val oficios = listOf(
        Triple("Albañilería", "Obra y acabados", Icons.Default.Construction),
        Triple("Plomería", "Fugas y tubos", Icons.Default.Plumbing),
        Triple("Electricidad", "110v / 220v", Icons.Default.Bolt),
        Triple("Carpintería", "Madera y clósets", Icons.Default.Carpenter),
        Triple("Herrería", "Puertas y rejas", Icons.Default.Hardware),
        Triple("Pintura", "Interiores y fachadas", Icons.Default.FormatPaint)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Únete como Trabajador",
            subtitulo = "Publica tus trabajos con evidencia real, recibe solicitudes directas y haz crecer tu taller sin comisiones abusivas.",
            alVolver = alVolverInicio
        )

        Spacer(modifier = Modifier.height(18.dp))

        SelectorModoAutenticacion(
            esLoginSeleccionado = false,
            alSeleccionarLogin = alIrALogin,
            alSeleccionarRegistro = {}
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Tarjeta Foto de Perfil Profesional
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = HabilioTheme.SuperficieTarjeta,
            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.SuperficieElevada),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = null,
                            tint = HabilioTheme.AmarilloPrimario,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.AmarilloPrimario),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Subir foto",
                            tint = HabilioTheme.TextoSobreAmarillo,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Foto de Perfil Profesional",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = HabilioTheme.TextoPrimario
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sube una imagen clara de tu rostro, uniforme o taller. Un perfil con foto recibe 4x más solicitudes directas.",
                    fontSize = 13.sp,
                    color = HabilioTheme.TextoSecundario,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = HabilioTheme.AmarilloPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargar Fotografía", color = HabilioTheme.AmarilloPrimario, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // SECCIÓN: DATOS DE CONTACTO
        Text(
            text = "👤 DATOS DE CONTACTO",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HabilioTheme.AmarilloPrimario
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Nombre Completo",
            valor = nombre,
            alCambiarValor = { nombre = it },
            placeholder = "Ej. Roberto Méndez",
            iconoInicial = Icons.Outlined.Person
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Nombre del Taller / Negocio",
            etiquetaDerecha = "(Opcional)",
            valor = nombreTaller,
            alCambiarValor = { nombreTaller = it },
            placeholder = "Ej. Herrería El Sol o Taller Méndez",
            iconoInicial = Icons.Outlined.Storefront
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Teléfono Celular",
            valor = telefono,
            alCambiarValor = { telefono = it },
            placeholder = "469 123 4567",
            prefijoTexto = "+52"
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Correo Electrónico",
            valor = correo,
            alCambiarValor = { correo = it },
            placeholder = "maestro@correo.com",
            iconoInicial = Icons.Outlined.Email
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Crear Contraseña Segura",
            valor = contrasena,
            alCambiarValor = { contrasena = it },
            placeholder = "Mínimo 8 caracteres",
            iconoInicial = Icons.Outlined.Lock,
            esContrasena = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECCIÓN: OFICIO PRINCIPAL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛠️ OFICIO PRINCIPAL",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.AmarilloPrimario
            )
            Text(
                text = "Elige 1 o 2",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloPrimario
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Cuadrícula 2x3 de oficios
        oficios.chunked(2).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fila.forEach { (tituloOficio, subOficio, iconoOficio) ->
                    val seleccionado = oficioSeleccionado == tituloOficio
                    Surface(
                        onClick = { oficioSeleccionado = tituloOficio },
                        shape = RoundedCornerShape(12.dp),
                        color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.SuperficieTarjeta,
                        border = BorderStroke(
                            1.dp,
                            if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeTarjeta
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconoOficio,
                                contentDescription = null,
                                tint = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.AmarilloPrimario,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tituloOficio,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoPrimario
                                )
                                Text(
                                    text = subOficio,
                                    fontSize = 12.sp, // Subido a 12sp por accesibilidad
                                    color = if (seleccionado) HabilioTheme.TextoSobreAmarillo.copy(alpha = 0.8f) else HabilioTheme.TextoSecundario
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Especialidad o Servicios Específicos",
            valor = especialidad,
            alCambiarValor = { especialidad = it },
            placeholder = "Ej. Instalación de vitropiso, boiler solar, soldadura..."
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECCIÓN: COBERTURA Y MODALIDAD
        Text(
            text = "📍 COBERTURA Y MODALIDAD DE TRABAJO",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HabilioTheme.AmarilloPrimario
        )
        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Zona de Servicio Habitual",
            valor = zonaServicio,
            alCambiarValor = { zonaServicio = it },
            placeholder = "Pénjamo Centro y alrededores",
            iconoInicial = Icons.Outlined.Explore
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "¿Cómo atiendes a tus clientes?",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HabilioTheme.TextoPrimario
        )
        Spacer(modifier = Modifier.height(8.dp))

        val modalidades = listOf(
            "🚗 A Domicilio" to "Me traslado a la casa o negocio del cliente en la región.",
            "🏠 En mi Taller o Local" to "Tengo ubicación física.",
            "🔄 Ambos (Taller y a domicilio)" to "Flexibilidad total según lo que el cliente requiera."
        )

        modalidades.forEachIndexed { indice, (tituloMod, descMod) ->
            val activo = modalidadSeleccionada == indice
            Surface(
                onClick = { modalidadSeleccionada = indice },
                shape = RoundedCornerShape(12.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(
                    1.dp,
                    if (activo) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeTarjeta
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = activo,
                        onClick = { modalidadSeleccionada = indice },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = HabilioTheme.AmarilloPrimario,
                            unselectedColor = HabilioTheme.TextoSecundario
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tituloMod,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HabilioTheme.TextoPrimario
                        )
                        Text(
                            text = descMod,
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioCampoTexto(
            etiqueta = "Dirección o Referencia del Taller",
            etiquetaDerecha = "(Opcional)",
            valor = direccionTaller,
            alCambiarValor = { direccionTaller = it },
            placeholder = "Ej. Calle Manuel Doblado #45, Col. Los Fresnos",
            iconoInicial = Icons.Outlined.Store
        )

        Spacer(modifier = Modifier.height(24.dp))

        BotonPrimarioHabilio(
            texto = "Registrarme como trabajador",
            alClick = alRegistrarTrabajador,
            mostrarFlecha = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TarjetaCambioACliente(alClick = alCambiarACliente)
    }
}

// ============================================================================
// 9. PANTALLA 6: BUSCADOR DE ESPECIALISTAS (Buscador.jpg)
// ============================================================================
data class EspecialistaDemo(
    val nombre: String,
    val oficio: String,
    val ubicacion: String,
    val calificacion: String,
    val resenas: String,
    val esPremium: Boolean,
    val descripcion: String,
    val textoGarantia: String
)

@Composable
fun PantallaBuscadorCliente(
    alVerEspecialista: () -> Unit,
    alIrAMiPerfil: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var categoriaActiva by remember { mutableStateOf("Todos") }
    val categorias = listOf("Todos", "Albañilería", "Costura", "Plomería")

    val especialistas = remember {
        listOf(
            EspecialistaDemo(
                nombre = "Don Roberto Méndez",
                oficio = "Albañilería y Acabados",
                ubicacion = "Col. Centro • A 1.2 km",
                calificacion = "4.9",
                resenas = "(38)",
                esPremium = true,
                descripcion = "Maestro albañil con más de 15 años de experiencia práctica en levantamiento de muros, losas, acabados finos y remodelación general garantizada.",
                textoGarantia = "✓ Experiencia comprobada"
            ),
            EspecialistaDemo(
                nombre = "María Gómez",
                oficio = "Costura y Confección",
                ubicacion = "Col. Las Américas • A 0.8 km",
                calificacion = "4.8",
                resenas = "(24)",
                esPremium = false,
                descripcion = "Servicios de costura, remiendos, vestidos de fiesta y dobladillos. Trabajo de calidad y rápido con atención personalizada a domicilio.",
                textoGarantia = "✓ Garantía de confección"
            ),
            EspecialistaDemo(
                nombre = "Pedro Juárez",
                oficio = "Plomería y Fontanería",
                ubicacion = "Col. Degollado • A 2.1 km",
                calificacion = "5.0",
                resenas = "(52)",
                esPremium = true,
                descripcion = "Especialista en instalación de tuberías, reparación de fugas, mantenimiento de calentadores solares y boilers de paso. Atención rápida para emergencias.",
                textoGarantia = "✓ Servicio de emergencia"
            ),
            EspecialistaDemo(
                nombre = "Juan Torres",
                oficio = "Carpintería y Muebles",
                ubicacion = "Col. San José • A 1.5 km",
                calificacion = "4.7",
                resenas = "(19)",
                esPremium = false,
                descripcion = "Fabricación y reparación de todo tipo de muebles de madera, puertas, cocinas y clósets a medida. Madera sólida y acabados en barniz de alta durabilidad.",
                textoGarantia = "✓ Presupuestos sin compromiso"
            )
        )
    }

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = "Pénjamo, Gto.",
                alAccionDerecha = alCambiarRol
            )
        },
        bottomBar = {
            BarraInferiorCliente(
                enExplorar = true,
                alIrAExplorar = {},
                alIrAMiPerfil = alIrAMiPerfil
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
            // Selector de zona
            Text(
                text = "ZONA DE SERVICIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.TextoSecundario
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = HabilioTheme.RojoUrgente,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pénjamo, Gto.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.TextoPrimario
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Cambiar zona",
                    tint = HabilioTheme.TextoSecundario
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de búsqueda
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Buscar albañil, costurera, plomero...",
                        color = HabilioTheme.TextoPlaceholder,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = HabilioTheme.TextoSecundario)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = HabilioTheme.SuperficieTarjeta,
                    unfocusedContainerColor = HabilioTheme.SuperficieTarjeta,
                    focusedBorderColor = HabilioTheme.AmarilloPrimario,
                    unfocusedBorderColor = HabilioTheme.BordeTarjeta
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Chips de categorías
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
                        label = {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        },
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

            // Banner informativo de perfiles verificados
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HabilioTheme.FondoAzulSuave,
                border = BorderStroke(1.dp, HabilioTheme.AzulVerificado.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = HabilioTheme.AzulVerificado,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Perfiles con evidencia real: 4 disponibles cerca de ti",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HabilioTheme.TextoPrimario
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de tarjetas de especialistas
            especialistas.forEach { esp ->
                TarjetaEspecialistaBuscador(
                    especialista = esp,
                    alVerDetalle = alVerEspecialista
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TarjetaEspecialistaBuscador(
    especialista: EspecialistaDemo,
    alVerDetalle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HabilioTheme.SuperficieTarjeta,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar con indicador de disponibilidad
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HabilioTheme.SuperficieElevada),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = HabilioTheme.TextoSecundario,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.VerdeExito)
                            .border(2.dp, HabilioTheme.SuperficieTarjeta, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = especialista.nombre,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HabilioTheme.TextoPrimario
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Perfil verificado",
                            tint = HabilioTheme.AzulVerificado,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = especialista.oficio,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HabilioTheme.AmarilloPrimario
                    )
                    Text(
                        text = "📍 ${especialista.ubicacion}",
                        fontSize = 12.sp,
                        color = HabilioTheme.TextoSecundario
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (especialista.esPremium) {
                        HabilioBadge(
                            texto = "★ PREMIUM",
                            colorTexto = HabilioTheme.AmarilloPrimario,
                            colorFondo = Color(0xFF332910)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("★", color = HabilioTheme.AmarilloPrimario, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = especialista.calificacion,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HabilioTheme.TextoPrimario
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = especialista.resenas,
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = especialista.descripcion,
                fontSize = 14.sp,
                color = HabilioTheme.TextoPrimario,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Miniaturas de evidencia de portafolio (3 cuadros)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HabilioTheme.SuperficieElevada)
                            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = "Evidencia ${index + 1}",
                                tint = HabilioTheme.TextoSecundario
                            )
                            Text(
                                text = "Foto ${index + 1}",
                                fontSize = 11.sp,
                                color = HabilioTheme.TextoSecundario
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pie de tarjeta con garantía y botón "Ver Especialista"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = especialista.textoGarantia,
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
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Ver Especialista", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ============================================================================
// 10. PANTALLA 7: DETALLE DE ESPECIALISTA (Detalle.jpg)
// ============================================================================
@Composable
fun PantallaDetalleEspecialista(
    alVolverAlListado: () -> Unit,
    alSolicitarServicio: () -> Unit,
    alIrAExplorar: () -> Unit,
    alIrAMiPerfil: () -> Unit
) {
    Scaffold(
        topBar = { HabilioTopBar(subtituloUbicacion = "Pénjamo, Gto.") },
        bottomBar = {
            BarraInferiorCliente(
                enExplorar = true,
                alIrAExplorar = alIrAExplorar,
                alIrAMiPerfil = alIrAMiPerfil
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
            // Fila de navegación de retorno y sello de verificación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = alVolverAlListado,
                    modifier = Modifier.height(48.dp) // Área táctil corregida
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = HabilioTheme.TextoPrimario,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Volver al listado", color = HabilioTheme.TextoPrimario, fontSize = 14.sp)
                }

                HabilioBadge(
                    texto = "✓ Perfil Verificado",
                    colorTexto = HabilioTheme.AzulVerificado,
                    colorFondo = HabilioTheme.FondoAzulSuave
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tarjeta principal del Especialista
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HabilioTheme.SuperficieElevada),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = HabilioTheme.TextoSecundario,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "● Disponible hoy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HabilioTheme.VerdeExito
                            )
                            Text(
                                text = "Roberto Méndez",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabilioTheme.TextoPrimario
                            )
                            Text(
                                text = "Albañilería y Acabados",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HabilioTheme.AmarilloPrimario
                            )
                            Text(
                                text = "Col. Centro • A 1.2 km",
                                fontSize = 12.sp,
                                color = HabilioTheme.TextoSecundario
                            )
                        }
                        HabilioBadge(
                            texto = "★ PREMIUM",
                            colorTexto = HabilioTheme.TextoSobreAmarillo,
                            colorFondo = HabilioTheme.AmarilloPrimario
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Cajas de métricas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CajaMetricaDetalle("4.9 ★", "38 reseñas", HabilioTheme.AmarilloPrimario, Modifier.weight(1f))
                        CajaMetricaDetalle("54", "Servicios", HabilioTheme.VerdeExito, Modifier.weight(1f))
                        CajaMetricaDetalle("3 fotos", "Evidencias", HabilioTheme.AzulVerificado, Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resumen de Experiencia Práctica
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.FondoAzulSuave.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, HabilioTheme.AzulVerificado.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✦ Resumen de Experiencia Práctica",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HabilioTheme.AzulVerificado
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Maestro albañil con más de 15 años de experiencia práctica en levantamiento de muros, aplanados finos, colocación de piso cerámico, loseta e impermeabilización. Trabajo garantizado con evidencia real.",
                        fontSize = 14.sp,
                        color = HabilioTheme.TextoPrimario,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val etiquetas = listOf(
                        "Pisos y Loseta" to "Muros y Aplanados",
                        "Remodelación" to "Impermeabilización"
                    )
                    etiquetas.forEach { (izq, der) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EtiquetaEspecialidad(izq, Modifier.weight(1f))
                            EtiquetaEspecialidad(der, Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción
            BotonPrimarioHabilio(
                texto = "Solicitar Servicio ($450 - $1,200 MXN)",
                alClick = alSolicitarServicio
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "★ Calificar y Recomendar a Roberto",
                    color = HabilioTheme.AmarilloPrimario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Portafolio de Trabajos Reales
            Text(
                text = "Portafolio de Trabajos Reales",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.TextoPrimario
            )
            Spacer(modifier = Modifier.height(12.dp))

            val portafolio = listOf(
                "Colocación de piso cerámico en sala (24 m²)" to "✓ Octubre 2026 • Pénjamo Centro",
                "Colado de losa y estructura de varilla (60 m²)" to "✓ Septiembre 2026 • Col. Juárez",
                "Remodelación integral de barra de cocina" to "✓ Agosto 2026 • Pénjamo, Gto."
            )

            portafolio.forEach { (tituloObra, fechaUbicacion) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HabilioTheme.SuperficieTarjeta,
                    border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(HabilioTheme.SuperficieElevada),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = null,
                                    tint = HabilioTheme.TextoSecundario,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Evidencia fotográfica verificada",
                                    fontSize = 12.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = tituloObra,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fechaUbicacion,
                                fontSize = 12.sp,
                                color = HabilioTheme.VerdeExito,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recomendaciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recomendaciones (2)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HabilioTheme.TextoPrimario
                )
                TextButton(onClick = {}) {
                    Text(
                        text = "+ Escribir reseña",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HabilioTheme.AmarilloPrimario
                    )
                }
            }

            TarjetaResena(
                autor = "Laura G.",
                comentario = "\"Excelente trabajo de Don Roberto. Puso toda la loseta del patio muy rápido y dejó todo limpio.\"",
                fecha = "Hace 2 días • Servicio verificado"
            )
            Spacer(modifier = Modifier.height(10.dp))
            TarjetaResena(
                autor = "Carlos M.",
                comentario = "\"Su portafolio muestra tal cual lo que sabe hacer. Muy cumplido con los tiempos.\"",
                fecha = "Hace 2 semanas • Servicio verificado"
            )
        }
    }
}

@Composable
private fun CajaMetricaDetalle(
    valor: String,
    etiqueta: String,
    colorValor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = HabilioTheme.SuperficieInput,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = valor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = colorValor)
            Text(text = etiqueta, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
        }
    }
}

@Composable
private fun EtiquetaEspecialidad(texto: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = HabilioTheme.SuperficieInput,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = modifier
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            color = HabilioTheme.TextoPrimario,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        )
    }
}

@Composable
private fun TarjetaResena(autor: String, comentario: String, fecha: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HabilioTheme.SuperficieTarjeta,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = autor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HabilioTheme.TextoPrimario)
                Text(text = "★★★★★", color = HabilioTheme.AmarilloPrimario, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = comentario,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = HabilioTheme.TextoPrimario,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = fecha, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
        }
    }
}

// ============================================================================
// 11. PANTALLA 8: PERFIL DEL CLIENTE (Perfil-Cliente.jpg)
// ============================================================================
@Composable
fun PantallaPerfilCliente(
    alVerDetalleServicio: () -> Unit,
    alIrAExplorar: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var pagoLiberado by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = "Pénjamo, Gto.",
                alAccionDerecha = alCambiarRol
            )
        },
        bottomBar = {
            BarraInferiorCliente(
                enExplorar = false,
                alIrAExplorar = alIrAExplorar,
                alIrAMiPerfil = {}
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
            // Tarjeta de datos de Laura Gómez
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(HabilioTheme.SuperficieElevada),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = HabilioTheme.TextoPrimario,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(HabilioTheme.VerdeExito),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Cliente verificado",
                                    tint = HabilioTheme.TextoSobreAmarillo,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Laura Gómez",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabilioTheme.TextoPrimario
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HabilioBadge(
                                texto = "✓ Verificado",
                                colorTexto = HabilioTheme.VerdeExito,
                                colorFondo = HabilioTheme.FondoVerdeSuave
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Familia Ramírez • Miembro local",
                                fontSize = 13.sp,
                                color = HabilioTheme.TextoSecundario
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "📱 +52 469 102 3844  •  laura.gomez@email.com",
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 Zona Centro, Pénjamo, Gto.",
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Actividad
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = HabilioTheme.AmarilloPrimario,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Actividad",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = HabilioTheme.TextoPrimario
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CajaMetricaDetalle("6", "Servicios\nPedidos", HabilioTheme.TextoPrimario, Modifier.weight(1f))
                        CajaMetricaDetalle("5", "Trabajos\nHechos", HabilioTheme.VerdeExito, Modifier.weight(1f))
                        CajaMetricaDetalle(
                            valor = if (pagoLiberado) "0" else "1",
                            etiqueta = if (pagoLiberado) "$0 Custodia" else "$850 Custodia",
                            colorValor = HabilioTheme.AmarilloPrimario,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dirección y Zona
            Text(
                text = "Dirección y Zona",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.TextoPrimario
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF332910)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = HabilioTheme.AmarilloPrimario
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Casa Principal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HabilioBadge(
                                texto = "Frecuente",
                                colorTexto = HabilioTheme.AmarilloPrimario,
                                colorFondo = Color(0xFF332910)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calle Hidalgo #114, Zona Centro",
                            fontSize = 14.sp,
                            color = HabilioTheme.TextoPrimario
                        )
                        Text(
                            text = "Pénjamo, Guanajuato • CP 36900",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Servicios Contratados
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Servicios Contratados",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HabilioTheme.TextoPrimario
                )
                Spacer(modifier = Modifier.width(8.dp))
                HabilioBadge(
                    texto = "2 activos",
                    colorTexto = HabilioTheme.TextoSecundario,
                    colorFondo = HabilioTheme.SuperficieElevada
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Servicio 1: Roberto Méndez
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HabilioTheme.SuperficieElevada),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = HabilioTheme.TextoSecundario)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Roberto Méndez",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = HabilioTheme.TextoPrimario
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = HabilioTheme.VerdeExito,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "Albañilería y Acabados",
                                    fontSize = 12.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                            }
                        }

                        HabilioBadge(
                            texto = if (pagoLiberado) "★ Completado" else "● En ejecución",
                            colorTexto = if (pagoLiberado) HabilioTheme.VerdeExito else HabilioTheme.AmarilloPrimario,
                            colorFondo = if (pagoLiberado) HabilioTheme.FondoVerdeSuave else Color(0xFF332910)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HabilioTheme.SuperficieInput,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Colocación de piso cerámico en sala (18 m²)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (pagoLiberado) "✓ Pago liberado al especialista" else "🔒 Garantía en custodia",
                                    fontSize = 12.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                                Text(
                                    text = "$850.00 MXN",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = HabilioTheme.AmarilloPrimario
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = alVerDetalleServicio,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = HabilioTheme.TextoPrimario,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver detalle", color = HabilioTheme.TextoPrimario, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { pagoLiberado = true },
                            enabled = !pagoLiberado,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HabilioTheme.VerdeExito,
                                contentColor = HabilioTheme.TextoSobreAmarillo
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pagoLiberado) "Liberado" else "Liberar Pago",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Servicio 2: Juan Hernández (Corregido el truncamiento "Plomería y Gas Residencial")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HabilioTheme.SuperficieElevada),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = HabilioTheme.TextoSecundario)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Juan Hernández",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = HabilioTheme.TextoPrimario
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = HabilioTheme.VerdeExito,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "Plomería y Gas Residencial", // Texto completo sin truncar
                                    fontSize = 12.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                            }
                        }

                        HabilioBadge(
                            texto = "★ 5.0 • Completado",
                            colorTexto = HabilioTheme.VerdeExito,
                            colorFondo = HabilioTheme.FondoVerdeSuave
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HabilioTheme.SuperficieInput,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Reparación de fuga en cisterna",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Servicio concluido el 18 Oct",
                                    fontSize = 12.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                                Text(
                                    text = "$420.00 MXN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = HabilioTheme.TextoPrimario
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ Garantía de 30 días vigente",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                        TextButton(onClick = {}) {
                            Text(
                                text = "Recibo >",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 12. PANTALLA 9: SOLICITUDES DEL TRABAJADOR (Solicitudes.jpg)
// ============================================================================
@Composable
fun PantallaSolicitudesTrabajador(
    alIrAMiTaller: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var filtroSeleccionado by remember { mutableStateOf("Todas 3") }
    val filtros = listOf("Todas 3", "Nuevas 2", "En curso 1", "Completadas")

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = "Pénjamo, Gto.",
                modoTrabajador = true,
                alAccionDerecha = alCambiarRol,
                iconoDerecha = Icons.Default.History
            )
        },
        bottomBar = {
            BarraInferiorTrabajador(
                enSolicitudes = true,
                alIrASolicitudes = {},
                alIrAMiTaller = alIrAMiTaller
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
            Text(
                text = "Solicitudes de Trabajo",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "● 3 pendientes de respuesta • 1 en curso",
                fontSize = 13.sp,
                color = HabilioTheme.AmarilloPrimario,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Chips de estado de solicitudes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtros.forEach { filtro ->
                    val activo = filtroSeleccionado == filtro
                    FilterChip(
                        selected = activo,
                        onClick = { filtroSeleccionado = filtro },
                        label = { Text(filtro, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HabilioTheme.AmarilloPrimario,
                            selectedLabelColor = HabilioTheme.TextoSobreAmarillo,
                            containerColor = HabilioTheme.SuperficieTarjeta,
                            labelColor = HabilioTheme.TextoPrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activo,
                            borderColor = HabilioTheme.BordeTarjeta
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Solicitud 1: Elena Morales (Corregida alineación del botón Rechazar/Aceptar y ubicación completa)
            TarjetaSolicitudNueva(
                cliente = "Elena Morales",
                ubicacion = "Col. Lindavista • A 1.4 km",
                tiempo = "Hace 20 min",
                esUrgente = true,
                etiquetaOficio = "⚠ Plomería / Urgente",
                etiquetaGarantia = "✓ Pago Garantizado",
                descripcion = "Reparación de fuga en tubería de cobre y cambio de llave de paso principal en patio.",
                etiquetaMonto = "PRESUPUESTO OFRECIDO",
                monto = "$650 – $900 MXN",
                colorMonto = HabilioTheme.AmarilloPrimario,
                estadoFondos = "🔒 Depósito verificado",
                alIrAMiTaller = alIrAMiTaller
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Solicitud 2: Carlos Mendoza
            TarjetaSolicitudNueva(
                cliente = "Carlos Mendoza",
                ubicacion = "Col. Centro, Calle Hidalgo #114",
                tiempo = "Hace 1 hora",
                esUrgente = false,
                etiquetaOficio = "🛠️ Albañilería y Pisos",
                etiquetaGarantia = "🛡️ Custodia Habilio",
                descripcion = "Colocación de piso cerámico en sala (18 m²) y resane de muro con acabado fino.",
                etiquetaMonto = "MONTO ACORDADO",
                monto = "$850.00 MXN",
                colorMonto = HabilioTheme.TextoPrimario,
                estadoFondos = "✓ Fondos listos",
                alIrAMiTaller = alIrAMiTaller
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Solicitud 3: Erica Ramírez (En Ejecución con borde verde)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.5.dp, HabilioTheme.VerdeExito),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HabilioTheme.FondoVerdeSuave),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WorkOutline,
                                    contentDescription = null,
                                    tint = HabilioTheme.VerdeExito
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Erica Ramírez",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = HabilioTheme.TextoPrimario
                                )
                                Text(
                                    text = "📍 Col. Juárez • Hoy",
                                    fontSize = 12.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                            }
                        }

                        HabilioBadge(
                            texto = "● En Ejecución",
                            colorTexto = HabilioTheme.VerdeExito,
                            colorFondo = HabilioTheme.FondoVerdeSuave
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Aplanado fino en fachada y pintura vinílica exterior (25 m²).",
                        fontSize = 14.sp,
                        color = HabilioTheme.TextoPrimario,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📅 Iniciado ayer • Fecha de entrega acordada: Hoy",
                        fontSize = 12.sp,
                        color = HabilioTheme.TextoSecundario
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HabilioTheme.SuperficieInput,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Protegido por Habilio",
                                    fontSize = 11.sp,
                                    color = HabilioTheme.TextoSecundario
                                )
                                Text(
                                    text = "$1,600.00 MXN",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = HabilioTheme.VerdeExito
                                )
                            }
                            Text(
                                text = "📷 2 fotos de avance",
                                fontSize = 12.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = alIrAMiTaller,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HabilioTheme.VerdeExito,
                            contentColor = HabilioTheme.TextoSobreAmarillo
                        )
                    ) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Marcar como Terminado y Solicitar Pago",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitudNueva(
    cliente: String,
    ubicacion: String,
    tiempo: String,
    esUrgente: Boolean,
    etiquetaOficio: String,
    etiquetaGarantia: String,
    descripcion: String,
    etiquetaMonto: String,
    monto: String,
    colorMonto: Color,
    estadoFondos: String,
    alIrAMiTaller: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HabilioTheme.SuperficieTarjeta,
        border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieElevada),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = HabilioTheme.TextoSecundario)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = cliente,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = HabilioTheme.TextoPrimario
                        )
                        Text(
                            text = "📍 $ubicacion",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }

                HabilioBadge(
                    texto = "⏱ $tiempo",
                    colorTexto = if (esUrgente) HabilioTheme.RojoUrgente else HabilioTheme.TextoSecundario,
                    colorFondo = if (esUrgente) HabilioTheme.FondoRojoSuave else HabilioTheme.SuperficieElevada
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HabilioBadge(
                    texto = etiquetaOficio,
                    colorTexto = if (esUrgente) HabilioTheme.RojoUrgente else HabilioTheme.TextoPrimario,
                    colorFondo = if (esUrgente) HabilioTheme.FondoRojoSuave else HabilioTheme.SuperficieElevada
                )
                HabilioBadge(
                    texto = etiquetaGarantia,
                    colorTexto = HabilioTheme.VerdeExito,
                    colorFondo = HabilioTheme.FondoVerdeSuave
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = descripcion,
                fontSize = 14.sp,
                color = HabilioTheme.TextoPrimario,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HabilioTheme.SuperficieInput,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = etiquetaMonto,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HabilioTheme.TextoSecundario
                        )
                        Text(
                            text = monto,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = colorMonto
                        )
                    }
                    Text(
                        text = estadoFondos,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HabilioTheme.VerdeExito
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MEJORA DE ACCESIBILIDAD Y LAYOUT:
            // En el mockup original, "Rechazar" tenía un ancho incompleto (~75%) sobre "Aceptar".
            // Ahora se disponen de forma simétrica y accesible con altura mínima de 48dp.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .weight(0.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = HabilioTheme.SuperficieElevada,
                        contentColor = HabilioTheme.TextoPrimario
                    )
                ) {
                    Text("Rechazar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = alIrAMiTaller,
                    modifier = Modifier
                        .weight(0.6f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabilioTheme.AmarilloPrimario,
                        contentColor = HabilioTheme.TextoSobreAmarillo
                    )
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aceptar Solicitud", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ============================================================================
// 13. PANTALLA 10: PERFIL / MI TALLER DEL TRABAJADOR (Trabajador perfil.jpg)
// ============================================================================
@Composable
fun PantallaTallerTrabajador(
    alIrASolicitudes: () -> Unit,
    alCambiarRol: () -> Unit
) {
    var tituloNuevaEvidencia by remember { mutableStateOf("") }

    val evidenciasPublicadas = listOf(
        "Acabado de cantera y loseta en patio",
        "Colocación de piso cerámico en sala",
        "Levantamiento de muro perimetral",
        "Remodelación integral de interiores"
    )

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = "Pénjamo, Gto.",
                modoTrabajador = true,
                alAccionDerecha = alCambiarRol
            )
        },
        bottomBar = {
            BarraInferiorTrabajador(
                enSolicitudes = false,
                alIrASolicitudes = alIrASolicitudes,
                alIrAMiTaller = {}
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
            // Notificación de Pago Liberado
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.FondoVerdeSuave,
                border = BorderStroke(1.dp, HabilioTheme.VerdeExito.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HabilioBadge(
                            texto = "PAGO LIBERADO",
                            colorTexto = HabilioTheme.VerdeExito,
                            colorFondo = Color(0xFF0B1C16)
                        )
                        Text(
                            text = "Hoy, 9:30 AM",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Colocación de loseta • Cliente Laura G.",
                        fontSize = 14.sp,
                        color = HabilioTheme.TextoPrimario
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+$850.00 MXN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabilioTheme.VerdeExito
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resumen de Roberto Méndez
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(HabilioTheme.SuperficieElevada)
                                .border(2.dp, HabilioTheme.AmarilloPrimario, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = HabilioTheme.TextoPrimario,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Roberto Méndez",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabilioTheme.TextoPrimario
                            )
                            Text(
                                text = "Albañilería y Acabados",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HabilioTheme.AmarilloPrimario
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HabilioTheme.SuperficieInput,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Especialista en Albañilería y Pisos, Trabajo a domicilio",
                            fontSize = 13.sp,
                            color = HabilioTheme.TextoSecundario,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta Subir Foto de Trabajo Real
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subir Foto de Trabajo Real",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = HabilioTheme.TextoPrimario
                        )
                        Text(
                            text = "+ Evidencia",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = HabilioTheme.AmarilloPrimario
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Toma o sube una foto de un trabajo terminado para demostrar tu experiencia a nuevos clientes:",
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tituloNuevaEvidencia,
                        onValueChange = { tituloNuevaEvidencia = it },
                        placeholder = {
                            Text(
                                "Título (ej. Impermeabilización de azotea)",
                                color = HabilioTheme.TextoPlaceholder,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = HabilioTheme.SuperficieInput,
                            unfocusedContainerColor = HabilioTheme.SuperficieInput,
                            focusedBorderColor = HabilioTheme.AmarilloPrimario,
                            unfocusedBorderColor = HabilioTheme.BordeTarjeta
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { tituloNuevaEvidencia = "" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HabilioTheme.AmarilloPrimario,
                            contentColor = HabilioTheme.TextoSobreAmarillo
                        )
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargar Fotografía al Portafolio", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mis Evidencias Publicadas (4) - Títulos con 2 líneas para evitar cortes
            Text(
                text = "Mis Evidencias Publicadas (4)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.TextoPrimario
            )
            Spacer(modifier = Modifier.height(12.dp))

            evidenciasPublicadas.chunked(2).forEach { fila ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    fila.forEach { tituloEvidencia ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HabilioTheme.SuperficieTarjeta,
                            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(HabilioTheme.SuperficieElevada),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = null,
                                        tint = HabilioTheme.TextoSecundario,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = tituloEvidencia,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = HabilioTheme.TextoPrimario,
                                        maxLines = 2, // Permite 2 líneas completas en lugar de truncar a la mitad
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "✓ Publicada",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HabilioTheme.VerdeExito
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}