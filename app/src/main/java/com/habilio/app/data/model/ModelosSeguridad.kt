package com.habilio.app

/**
 * ACTA #25: a quién se reporta o bloquea.
 * [solicitudId] vacío cuando el reporte se hace desde un perfil y no desde un servicio.
 * [esEspecialista] true si la persona reportada ofrece servicios.
 */
data class ObjetivoReporte(
    val usuarioId: String,
    val nombre: String,
    val solicitudId: String,
    val esEspecialista: Boolean
)

/** Persona bloqueada por la cuenta actual. */
data class UsuarioBloqueado(
    val id: String,
    val nombre: String = "Persona bloqueada"
)
