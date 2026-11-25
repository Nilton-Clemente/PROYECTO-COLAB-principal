package com.prieto.colab_movil.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColabActualizar(
    actualizando: Boolean,
    habilitado: Boolean,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable BoxScope.() -> Unit
) {
    PullToRefreshBox(
        isRefreshing = actualizando,
        onRefresh = { if (habilitado) onActualizar() },
        modifier = modifier.fillMaxSize().semantics {
            if (habilitado) customActions = listOf(CustomAccessibilityAction("Actualizar") {
                onActualizar()
                true
            })
        },
        content = contenido
    )
}