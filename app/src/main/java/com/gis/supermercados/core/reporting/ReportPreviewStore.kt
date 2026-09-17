package com.gis.supermercados.core.reporting

import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.domain.model.ExportedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deposito en memoria del ultimo informe construido, para que la pantalla de previsualizacion
 * pueda mostrarlo sin serializar el documento a traves de la navegacion.
 *
 * Se limpia al salir de la previsualizacion para no retener memoria innecesaria.
 */
@Singleton
class ReportPreviewStore @Inject constructor() {

    private val _document = MutableStateFlow<ReportDocument?>(null)
    val document: StateFlow<ReportDocument?> = _document.asStateFlow()

    private val _exportedFile = MutableStateFlow<ExportedFile?>(null)
    val exportedFile: StateFlow<ExportedFile?> = _exportedFile.asStateFlow()

    fun publish(document: ReportDocument?) {
        _document.value = document
    }

    fun publishExported(file: ExportedFile?) {
        _exportedFile.value = file
    }

    fun clear() {
        _document.value = null
        _exportedFile.value = null
    }
}
