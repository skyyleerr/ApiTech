from django.urls import path

from . import views, portal

urlpatterns = [
    path("", views.inicio, name="inicio"),
    path("planes/", views.planes, name="planes"),
    path("solicitud-suscripcion/", views.solicitud, name="solicitud"),
    path("login/empresa/", views.login_empresa, name="login_empresa"),
    path("login/", views.login_vista, name="login"),
    path("logout/", views.logout_vista, name="logout"),
    # Cliente
    path("app/dashboard/", portal.dashboard, name="app_dashboard"),
    path("app/colmenas/", portal.colmenas, name="colmenas"),
    path("app/colmenas/nueva/", portal.colmena_form, name="colmena_nueva"),
    path("app/colmenas/<int:pk>/editar/", portal.colmena_form, name="colmena_editar"),
    # Equipo ApiTech
    path("admin/dashboard/", views.admin_dashboard, name="admin_dashboard"),
    path("admin/empresas/", views.admin_empresas, name="admin_empresas"),
    path(
        "admin/empresas/<int:pk>/estado/",
        views.admin_empresa_estado,
        name="admin_empresa_estado",
    ),
    path("admin/solicitudes/", views.admin_solicitudes, name="admin_solicitudes"),
    path(
        "admin/solicitudes/<int:pk>/",
        views.admin_solicitud_detalle,
        name="admin_solicitud",
    ),
    path("admin/pagos/", views.admin_pagos, name="admin_pagos"),
    path("app/colmenas/<int:pk>/", portal.detalle, name="colmena_detalle"),
    path(
        "app/colmenas/<int:pk>/eliminar/",
        portal.colmena_archivar,
        name="colmena_archivar",
    ),
    path("app/telemetria/", portal.telemetria, name="telemetria"),
    path(
        "app/colmenas/<int:pk>/telemetria/",
        portal.telemetria,
        name="telemetria_colmena",
    ),
    path("app/sensores/", portal.sensores, name="sensores"),
    path("app/sensores/nuevo/", portal.sensor_form, name="sensor_nuevo"),
    path("app/sensores/<int:pk>/editar/", portal.sensor_form, name="sensor_editar"),
    path("app/sensores/<int:pk>/accion/", portal.sensor_accion, name="sensor_accion"),
    path("app/produccion/", portal.produccion, name="produccion"),
    path("app/produccion/nueva/", portal.produccion_nueva, name="produccion_nueva"),
    path("app/alertas/", portal.alertas, name="alertas"),
    path(
        "app/alertas/<int:pk>/resolver/", portal.alerta_resolver, name="alerta_resolver"
    ),
    path("notificaciones/", portal.notificaciones, name="notificaciones"),
    path("perfil/", portal.perfil, name="perfil"),
    path("usuarios/", portal.usuarios, name="usuarios"),
    path("usuarios/nuevo/", portal.usuario_form, name="usuario_nuevo"),
    path("usuarios/<int:pk>/editar/", portal.usuario_form, name="usuario_editar"),
    path("app/umbrales/", portal.umbrales, name="umbrales"),
    path("app/conexion-api/", portal.conexion, name="conexion"),
    path("app/pagos/", portal.pagos, name="pagos"),
    path("app/reportes/", portal.reportes, name="reportes"),
    path("app/exportar/<str:formato>/", portal.exportar, name="exportar"),
    path("app/importar/", portal.importar, name="importar"),
    path(
        "app/importar/<int:pk>/", portal.importacion_detalle, name="importacion_detalle"
    ),
    path(
        "app/importar/<int:pk>/errores/",
        portal.importacion_errores,
        name="importacion_errores",
    ),
    path("app/plantilla/<str:formato>/", portal.plantilla, name="plantilla"),
    path("admin/empresas/nueva/", portal.empresa_form, name="empresa_nueva"),
    path("admin/empresas/<int:pk>/editar/", portal.empresa_form, name="empresa_editar"),
    path(
        "admin/empresas/<int:pk>/administrador/",
        portal.empresa_admin,
        name="empresa_admin",
    ),
    path("admin/planes/", portal.planes_admin, name="planes_admin"),
    path("admin/planes/<int:pk>/", portal.planes_admin, name="plan_editar"),
    path("admin/auditoria/", portal.auditoria, name="auditoria"),
    path(
        "admin/suscripciones/<int:pk>/estado/",
        portal.suscripcion_estado,
        name="suscripcion_estado",
    ),
    path("ayuda/", portal.ayuda, name="ayuda"),
]
