package com.prieto.colab_movil.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prieto.colab_movil.data.session.SessionManager
import com.prieto.colab_movil.ui.invitaciones.InvitacionesScreen
import com.prieto.colab_movil.ui.login.LoginScreen
import com.prieto.colab_movil.ui.perfil.PerfilScreen
import com.prieto.colab_movil.ui.proyectos.CrearProyectoScreen
import com.prieto.colab_movil.ui.proyectos.DetalleProyectoScreen
import com.prieto.colab_movil.ui.proyectos.ProyectosScreen
import com.prieto.colab_movil.ui.register.RegisterScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Proyectos : Screen("proyectos")
    object Perfil : Screen("perfil")
    object DetalleProyecto : Screen("proyecto_detalle/{proyectoId}") {
        fun createRoute(proyectoId: Long) = "proyecto_detalle/$proyectoId"
    }
    object CrearProyecto : Screen("crear_proyecto")
    object Invitaciones : Screen("invitaciones")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { token ->
                    SessionManager.token = token
                    navController.navigate(Screen.Proyectos.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPasswordClick = {
                    Toast.makeText(context, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { token ->
                    Toast.makeText(context, "¡Cuenta creada exitosamente!", Toast.LENGTH_LONG).show()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                onRecoverPasswordClick = {
                    Toast.makeText(context, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
                },
                onTermsClick = {
                    Toast.makeText(context, "Términos y condiciones TECSUP", Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(Screen.Proyectos.route) {
            ProyectosScreen(
                onProyectoClick = { proyecto ->
                    navController.navigate(Screen.DetalleProyecto.createRoute(proyecto.id))
                },
                onPerfilClick = {
                    navController.navigate(Screen.Perfil.route)
                },
                onInvitacionesClick = {
                    navController.navigate(Screen.Invitaciones.route) { launchSingleTop = true }
                },
                onCrearClick = {
                    navController.navigate(Screen.CrearProyecto.route)
                },
                onNotificacionesClick = {
                    Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(Screen.CrearProyecto.route) {
            CrearProyectoScreen(
                onGuardar = {
                    Toast.makeText(context, "Proyecto guardado correctamente", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onCancelar = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.DetalleProyecto.route,
            arguments = listOf(navArgument("proyectoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val proyectoId = backStackEntry.arguments?.getLong("proyectoId") ?: 1L
            DetalleProyectoScreen(
                proyectoId = proyectoId,
                onBack = { navController.popBackStack() },
                onPerfil = { navController.navigate(Screen.Perfil.route) },
                onEditar = { Toast.makeText(context, "Editar proyecto", Toast.LENGTH_SHORT).show() },
                onGenerarPlanificacion = { Toast.makeText(context, "Generar planificación con IA", Toast.LENGTH_SHORT).show() }
            )
        }

        composable(Screen.Perfil.route) {
            PerfilScreen(
                onProyectosClick = {
                    navController.popBackStack(Screen.Proyectos.route, inclusive = false)
                },
                onInvitacionesClick = {
                    navController.navigate(Screen.Invitaciones.route) { launchSingleTop = true }
                },
                onPerfilClick = {},
                onNotificacionesClick = {
                    Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable(Screen.Invitaciones.route) {
            InvitacionesScreen(
                onProyectosClick = {
                    navController.navigate(Screen.Proyectos.route) {
                        popUpTo(Screen.Proyectos.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPerfilClick = {
                    navController.navigate(Screen.Perfil.route) { launchSingleTop = true }
                },
                onNotificacionesClick = {
                    Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
