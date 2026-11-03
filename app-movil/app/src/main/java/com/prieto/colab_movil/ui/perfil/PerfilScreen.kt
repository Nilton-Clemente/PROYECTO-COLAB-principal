package com.prieto.colab_movil.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.HabilidadDto

// ---------------------------------------------------------
// COLORES INSPIRADOS EN EL DISEÑO ORIGINAL DE STITCH
// ---------------------------------------------------------

private val Fondo = Color(0xFFFAF8FF)
private val Superficie = Color(0xFFFFFFFF)
private val Primario = Color(0xFF3525CD)
private val PrimarioContainer = Color(0xFF4F46E5)
private val Lavanda = Color(0xFFE2E7FF)
private val TextoPrincipal = Color(0xFF131B2E)
private val TextoSecundario = Color(0xFF64647A)
private val Verde = Color(0xFF006C49)
private val VerdeSuave = Color(0xFF6CF8BB)
private val Rojo = Color(0xFF93000A)
private val RojoSuave = Color(0xFFFFDAD6)

private fun iniciales(nombre: String): String {
    val partes = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes.first().take(1).uppercase()
        else -> (partes.first().take(1) + partes.last().take(1)).uppercase()
    }
}

// ---------------------------------------------------------
// PANTALLA PRINCIPAL "MI PERFIL"
// ---------------------------------------------------------

@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel = viewModel(),
    onProyectosClick: () -> Unit = {},
    onInvitacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Fondo,
        topBar = {
            EncabezadoColab(
                onNotificaciones = onNotificacionesClick,
                onPerfil = onPerfilClick
            )
        },
        bottomBar = {
            BarraNavegacion(
                onProyectosClick = onProyectosClick,
                onInvitacionesClick = onInvitacionesClick,
                onPerfilClick = onPerfilClick
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PerfilUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primario)
                }
            }

            is PerfilUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        color = TextoSecundario,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.cargar() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primario)
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            is PerfilUiState.Success -> {
                ContenidoPerfil(
                    state = state,
                    onAgregar = { viewModel.agregarHabilidad(it) },
                    onEliminar = { viewModel.eliminarHabilidad(it) },
                    paddingValues = paddingValues
                )
            }
        }
    }
}

@Composable
private fun ContenidoPerfil(
    state: PerfilUiState.Success,
    onAgregar: (Long) -> Unit,
    onEliminar: (Long) -> Unit,
    paddingValues: PaddingValues
) {
    var busqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todos") }

    val idsHabilidades = state.habilidades.map { it.id }.toSet()
    val catalogoDisponible = state.catalogo.filter { it.id !in idsHabilidades }

    val categorias = remember(catalogoDisponible) {
        listOf("Todos") + catalogoDisponible.map { it.area ?: "General" }.distinct()
    }

    val tecnologiasFiltradas = catalogoDisponible.filter { tecnologia ->
        val coincideBusqueda = tecnologia.nombre.contains(
            busqueda.trim(),
            ignoreCase = true
        )
        val coincideCategoria = categoriaSeleccionada == "Todos" ||
            (tecnologia.area ?: "General") == categoriaSeleccionada
        coincideBusqueda && coincideCategoria
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            PerfilEstudiante(nombre = state.nombre, email = state.email)
        }

        item {
            EncabezadoSeccion(
                titulo = "Mis habilidades",
                subtitulo = "Tecnologías que forman parte de tu perfil",
                contador = "${state.habilidades.size}"
            )
        }

        item {
            if (state.habilidades.isEmpty()) {
                MensajeVacio("Todavía no tienes habilidades agregadas.")
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.habilidades.chunked(2).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            fila.forEach { habilidad ->
                                Box(modifier = Modifier.weight(1f)) {
                                    TarjetaHabilidad(
                                        habilidad = habilidad,
                                        onEliminar = { onEliminar(habilidad.id) }
                                    )
                                }
                            }

                            if (fila.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = Lavanda.copy(alpha = 0.8f))
        }

        item {
            EncabezadoSeccion(
                titulo = "Catálogo disponible",
                subtitulo = "Explora y añade tecnologías a tu perfil",
                contador = "${catalogoDisponible.size}"
            )
        }

        item {
            Buscador(
                texto = busqueda,
                onTextoCambio = { busqueda = it }
            )
        }

        item {
            FiltrosCategorias(
                categorias = categorias,
                seleccionada = categoriaSeleccionada,
                onSeleccionar = { categoriaSeleccionada = it }
            )
        }

        if (tecnologiasFiltradas.isEmpty()) {
            item {
                MensajeVacio("No se encontraron tecnologías con esos criterios.")
            }
        } else {
            items(
                items = tecnologiasFiltradas,
                key = { it.id }
            ) { tecnologia ->
                TarjetaCatalogo(
                    habilidad = tecnologia,
                    onAgregar = { onAgregar(tecnologia.id) }
                )
            }
        }
    }
}

// ---------------------------------------------------------
// ENCABEZADO SUPERIOR
// ---------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncabezadoColab(onNotificaciones: () -> Unit, onPerfil: () -> Unit) {
    TopAppBar(
        title = {
            Text("Colab", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        },
        actions = {
            IconButton(onClick = onNotificaciones) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = TextoSecundario
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Primario)
                    .clickable(onClick = onPerfil),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Mi perfil",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Fondo)
    )
}

// ---------------------------------------------------------
// PERFIL DEL ESTUDIANTE (datos reales del usuario)
// ---------------------------------------------------------

@Composable
fun PerfilEstudiante(
    nombre: String,
    email: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Superficie)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Lavanda),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iniciales(nombre),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primario
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = VerdeSuave,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Activo",
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        color = Color(0xFF00452E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = nombre,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "✉  $email",
                    fontSize = 11.sp,
                    color = TextoSecundario
                )
            }
        }
    }
}

// ---------------------------------------------------------
// ENCABEZADOS DE SECCIÓN
// ---------------------------------------------------------

@Composable
fun EncabezadoSeccion(
    titulo: String,
    subtitulo: String,
    contador: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = titulo,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Surface(
                color = if (titulo == "Mis habilidades") Lavanda else VerdeSuave,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = contador,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 5.dp
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (titulo == "Mis habilidades") Primario else Verde
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = subtitulo,
            color = TextoSecundario,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

// ---------------------------------------------------------
// TARJETA DE HABILIDAD DEL USUARIO
// ---------------------------------------------------------

@Composable
fun TarjetaHabilidad(
    habilidad: HabilidadDto,
    onEliminar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Superficie)
            .padding(12.dp)
    ) {
        Surface(
            color = Lavanda,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = habilidad.area ?: "General",
                modifier = Modifier.padding(
                    horizontal = 7.dp,
                    vertical = 4.dp
                ),
                color = Primario,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = habilidad.nombre,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onEliminar,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RojoSuave,
                contentColor = Rojo
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar habilidad",
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = "Eliminar",
                fontSize = 11.sp
            )
        }
    }
}

// ---------------------------------------------------------
// BUSCADOR DEL CATÁLOGO
// ---------------------------------------------------------

@Composable
fun Buscador(
    texto: String,
    onTextoCambio: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Superficie)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Buscar",
            tint = TextoSecundario
        )

        Spacer(Modifier.width(10.dp))

        BasicTextField(
            value = texto,
            onValueChange = onTextoCambio,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextoPrincipal,
                fontSize = 14.sp
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (texto.isEmpty()) {
                        Text(
                            text = "Buscar en catálogo...",
                            color = TextoSecundario,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (texto.isNotEmpty()) {
            IconButton(
                onClick = { onTextoCambio("") },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpiar búsqueda",
                    tint = TextoSecundario,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------
// FILTROS POR CATEGORÍA (dinámicos según el catálogo)
// ---------------------------------------------------------

@Composable
fun FiltrosCategorias(
    categorias: List<String>,
    seleccionada: String,
    onSeleccionar: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        categorias.forEach { categoria ->
            val activo = categoria == seleccionada

            Surface(
                onClick = { onSeleccionar(categoria) },
                shape = RoundedCornerShape(50),
                color = if (activo) Primario else Lavanda,
                modifier = Modifier
            ) {
                Text(
                    text = categoria,
                    modifier = Modifier.padding(
                        horizontal = 11.dp,
                        vertical = 9.dp
                    ),
                    color = if (activo) Color.White else TextoSecundario,
                    fontSize = 10.sp,
                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------
// TARJETA DEL CATÁLOGO DE TECNOLOGÍAS
// ---------------------------------------------------------

@Composable
fun TarjetaCatalogo(
    habilidad: HabilidadDto,
    onAgregar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Superficie)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Lavanda),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = habilidad.nombre.take(1).uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Primario
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = habilidad.nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = habilidad.area ?: "General",
                fontSize = 11.sp,
                color = TextoSecundario
            )
        }

        Button(
            onClick = onAgregar,
            shape = RoundedCornerShape(9.dp),
            contentPadding = PaddingValues(
                horizontal = 10.dp,
                vertical = 5.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Verde,
                contentColor = Color.White
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar tecnología",
                modifier = Modifier.size(16.dp)
            )

            Spacer(Modifier.width(3.dp))

            Text(
                text = "Agregar",
                fontSize = 11.sp
            )
        }
    }
}

// ---------------------------------------------------------
// ESTADO VACÍO
// ---------------------------------------------------------

@Composable
fun MensajeVacio(mensaje: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Superficie,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = mensaje,
            modifier = Modifier.padding(20.dp),
            color = TextoSecundario,
            fontSize = 13.sp
        )
    }
}

// ---------------------------------------------------------
// BARRA DE NAVEGACIÓN INFERIOR
// (por ahora solo muestra la opción "Mi Perfil")
// ---------------------------------------------------------

@Composable
fun BarraNavegacion(
    onProyectosClick: () -> Unit = {},
    onInvitacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {}
) {
    NavigationBar(containerColor = Superficie) {
        val colores = NavigationBarItemDefaults.colors(
            selectedIconColor = Primario,
            selectedTextColor = Primario,
            indicatorColor = Color.Transparent,
            unselectedIconColor = TextoSecundario,
            unselectedTextColor = TextoSecundario
        )
        NavigationBarItem(
            selected = false,
            onClick = onProyectosClick,
            colors = colores,
            icon = { Icon(Icons.Outlined.Folder, null) },
            label = { Text("Proyectos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onInvitacionesClick,
            colors = colores,
            icon = { Icon(Icons.Outlined.Mail, null) },
            label = { Text("Invitaciones", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = true,
            onClick = onPerfilClick,
            colors = colores,
            icon = { Icon(Icons.Outlined.Person, null) },
            label = { Text("Mi Perfil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
    }
}



