"""Casos adversos de límites de empresa, API y operaciones administrativas."""

from datetime import timedelta
from unittest.mock import patch, Mock
from django.db import DatabaseError, transaction
from django.urls import reverse
from django.utils import timezone
from django.test import override_settings
from apps.apiario.models import SensorColmena, Sensor, Colmena
from apps.apiario.services import crear_colmena, registrar_sensor
from apps.monitoreo.models import Medicion, Monitoreo
from apps.integraciones.models import ConexionAPI, ComandoSensor
from apps.integraciones.services import (
    Cliente,
    APIError,
    encolar,
    enviar_comandos,
    sincronizar,
)
from apps.importaciones.services import previsualizar, confirmar
from django.core.files.uploadedfile import SimpleUploadedFile
from apps.suscripciones.services import vencer_suscripciones
from .base import BaseTest, crear_sensor_con_monitoreo


class IntegridadSQLTests(BaseTest):
    def test_sql_no_cruza_asociacion_ni_mueve_empresa(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        cb = crear_colmena(self.admin_b, "Otra")
        with self.assertRaises(DatabaseError), transaction.atomic():
            SensorColmena.objects.filter(pk=a.pk).update(colmena=cb)
        with self.assertRaises(DatabaseError), transaction.atomic():
            Colmena.objects.filter(pk=c.pk).update(empresa=self.emp_b)
        with self.assertRaises(DatabaseError), transaction.atomic():
            Sensor.objects.filter(pk=s.pk).update(empresa=self.emp_b)

    def test_sql_no_inserta_lectura_de_otro_sensor(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        sb, ab, cb = crear_sensor_con_monitoreo(self.emp_b)
        with self.assertRaises(DatabaseError), transaction.atomic():
            Medicion.objects.bulk_create(
                [
                    Medicion(
                        asociacion=a,
                        sensor=sb,
                        tipo_medicion="TEMPERATURA",
                        valor=25,
                        unidad="°C",
                        fecha=timezone.now(),
                    )
                ]
            )


@override_settings(
    SENSOR_API_BASE_URL="https://api.example.test/v1", SENSOR_API_TOKEN="token-test"
)
class ContratoTests(BaseTest):
    def test_reintentos_timeout_y_http_idempotentes(self):
        import requests

        c = Cliente(self.emp_a.pk)
        ok = Mock(status_code=200)
        ok.json.return_value = {"ok": True}
        with patch(
            "apps.integraciones.services.requests.request",
            side_effect=[requests.Timeout(), Mock(status_code=503), ok],
        ) as send, patch("apps.integraciones.services.time.sleep"):
            self.assertTrue(
                c.request("POST", "/sensors", clave="estable", data={})["ok"]
            )
            self.assertEqual(send.call_count, 3)
            self.assertTrue(
                all(
                    x.kwargs["headers"]["Idempotency-Key"] == "estable"
                    for x in send.call_args_list
                )
            )

    def test_rechaza_redirect_y_no_expone_token(self):
        with patch(
            "apps.integraciones.services.requests.request",
            return_value=Mock(status_code=302),
        ):
            with self.assertRaisesMessage(APIError, "HTTP 302"):
                Cliente(self.emp_a.pk).request("GET", "/readings")

    def test_registro_y_activacion_con_identidad(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        cmd = encolar(s, "ACTIVATE", a)
        with patch("apps.integraciones.services.Cliente") as cls:
            cls.return_value.request.side_effect = [
                {"company_id": str(self.emp_a.pk), "sensor_id": "externo-1"},
                {
                    "company_id": str(self.emp_a.pk),
                    "sensor_id": "externo-1",
                    "status": "ACTIVE",
                },
            ]
            enviar_comandos(self.emp_a)
            calls = cls.return_value.request.call_args_list
            self.assertEqual(calls[1].kwargs["data"]["hive_id"], str(c.pk))
            self.assertEqual(str(calls[1].kwargs["clave"]), str(cmd.clave))
        cmd.refresh_from_db()
        s.refresh_from_db()
        self.assertTrue(cmd.completado)
        self.assertEqual(s.id_externo, "externo-1")

    def test_error_externo_conserva_orden_para_reintentar(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        cmd = encolar(s, "REGISTER")
        with patch("apps.integraciones.services.Cliente") as cls:
            cls.return_value.request.side_effect = APIError("Servicio no disponible")
            with self.assertRaises(APIError):
                enviar_comandos(self.emp_a)
        cmd.refresh_from_db()
        self.assertFalse(cmd.completado)
        self.assertEqual(cmd.intentos, 1)

    def test_api_no_acepta_lectura_incompleta(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        s.id_externo = "s"
        s.save()
        ConexionAPI.objects.create(empresa=self.emp_a, habilitada=True)
        with patch("apps.integraciones.services.Cliente") as cls:
            cls.return_value.request.return_value = {
                "items": [{"company_id": self.emp_a.pk}],
                "next_cursor": "1",
            }
            with self.assertRaises(APIError):
                sincronizar(self.emp_a)
        self.assertFalse(Medicion.objects.exists())


class OperacionTests(BaseTest):
    def test_pausar_empresa_cierra_monitoreo_y_encola_detencion(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        self.entrar(self.admin_api)
        r = self.client.post(
            reverse("admin_empresa_estado", args=[self.emp_a.pk]), {"estado": "pausada"}
        )
        self.assertEqual(r.status_code, 302)
        self.assertFalse(Monitoreo.objects.filter(estado="activo").exists())
        self.assertTrue(
            ComandoSensor.objects.filter(
                sensor=s, accion="DEACTIVATE", completado=False
            ).exists()
        )

    def test_vencimiento_cierra_monitoreo(self):
        s, a, c = crear_sensor_con_monitoreo(self.emp_a)
        self.emp_a.suscripciones.update(
            fecha_vencimiento=timezone.localdate() - timedelta(days=1)
        )
        self.assertEqual(vencer_suscripciones(), 1)
        self.assertFalse(Monitoreo.objects.filter(estado="activo").exists())

    def test_importar_sensores_encola_solo_en_confirmacion(self):
        data = b"codigo,tipo,modelo,fabricante\nNUEVO,PESO,M,F\n"
        lote = previsualizar(
            self.admin_a, SimpleUploadedFile("sensor.csv", data), "sensores"
        )
        self.assertFalse(ComandoSensor.objects.exists())
        confirmar(self.admin_a, lote)
        self.assertEqual(ComandoSensor.objects.count(), 1)

    def test_doble_pago_web_y_campos_invalidos(self):
        self.entrar(self.admin_a)
        plan = self.emp_a.suscripciones.first().plan_id
        for _ in range(2):
            self.client.post(reverse("pagos"), {"plan": plan})
        from apps.suscripciones.models import Pago

        self.assertEqual(
            Pago.objects.filter(
                estado="PENDIENTE", suscripcion__empresa=self.emp_a
            ).count(),
            1,
        )
        self.assertEqual(
            self.client.post(reverse("pagos"), {"plan": "abc"}).status_code, 200
        )


class ImportacionExistentesTests(BaseTest):
    def test_normaliza_lectura_existente_y_agrega_cosecha_una_vez(self):
        from apps.monitoreo.services import registrar_medicion
        from apps.apiario.models import ProduccionMiel

        c = crear_colmena(self.admin_a, "Importación")
        s, a, c = crear_sensor_con_monitoreo(self.emp_a, colmena=c)
        fecha = timezone.now() - timedelta(minutes=10)
        registrar_medicion(
            s, "26.000", fecha, id_externo="externa-1", fuente="API_EXTERNA"
        )
        data = f"fecha,colmena,asociacion,tipo,valor,unidad,origen,produccion,observaciones\n{fecha.isoformat()},1,{a.pk},TEMPERATURA,26.0,°C,SIMULADO,2.5,Cosecha\n"
        for _ in range(2):
            lote = previsualizar(
                self.admin_a, SimpleUploadedFile("hist.csv", data.encode()), "historico"
            )
            self.assertEqual(lote.errores, [])
            confirmar(self.admin_a, lote)
        self.assertEqual(Medicion.objects.count(), 1)
        self.assertEqual(ProduccionMiel.objects.count(), 1)
        self.assertEqual(Medicion.objects.get().fuente, "API_EXTERNA")

    def test_excel_conserva_numeros_y_neutraliza_formulas(self):
        from apps.web.exportar import generar_archivo
        from openpyxl import load_workbook
        from decimal import Decimal
        import io

        r = generar_archivo(
            "prueba", ["Texto", "Valor"], [["=1+1", Decimal("-2.50")]], "xlsx"
        )
        book = load_workbook(io.BytesIO(r.content))
        sheet = book.active
        self.assertEqual(sheet["A2"].data_type, "s")
        self.assertTrue(sheet["A2"].value.startswith("'="))
        self.assertEqual(sheet["B2"].data_type, "n")
        self.assertEqual(sheet["B2"].value, -2.5)


class AccesoTecnicoTests(BaseTest):
    def test_superusuario_entra_por_formulario_tecnico(self):
        from apps.usuarios.models import Usuario

        u = Usuario.objects.create_superuser(
            "tecnico@example.test", "Tecnica-Segura-2026"
        )
        r = self.client.post(
            "/admin-tecnico/login/?next=/admin-tecnico/",
            {
                "username": u.correo,
                "password": "Tecnica-Segura-2026",
                "next": "/admin-tecnico/",
            },
        )
        self.assertEqual(r.status_code, 302)
        self.assertEqual(int(self.client.session["_auth_user_id"]), u.pk)
