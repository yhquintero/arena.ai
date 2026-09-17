package com.gis.supermercados.di

import javax.inject.Qualifier

/** Calificadores de Hilt para distinguir dispatchers y ambitos de corrutinas. */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/** Ambito de la aplicacion: vive mientras el proceso (log, backups, semillas). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
