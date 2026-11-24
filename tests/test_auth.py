import bcrypt
from django.test import TestCase

from apps.usuarios.models import Rol, Usuario

from .base import CLAVE, BaseTest, crear_usuario


class LoginTests(BaseTest):
    def test_login_por_cada_rol_redirige_a_su_dashboard(self):
        casos = [
            (self.admin_api, "/admin/dashboard/"),
            (self.emp_api, "/admin/dashboard/"),
            (self.admin_a, "/app/dashboard/"),
            (self.emp_cli_a, "/app/dashboard/"),
        ]
        for usuario, destino in casos:
            with self.subTest(rol=usuario.rol.nombre):
                self.client.logout()
                self.assertEqual(self.entrar(usuario)["Location"], destino)
                self.assertEqual(self.client.get(destino).status_code, 200)

    def test_usuario_inactivo_no_entra(self):
        u = crear_usuario(Rol.ADMIN_CLIENTE, self.emp_a, estado=Usuario.INACTIVO)
        self.client.post("/login/empresa/", {"nit": self.emp_a.nit})
        r = self.client.post("/login/", {"correo": u.correo, "password": CLAVE})
        self.assertEqual(r.status_code, 200)
        self.assertNotIn("_auth_user_id", self.client.session)

    def test_nit_inexistente_se_rechaza(self):
        r = self.client.post("/login/empresa/", {"nit": "000-0"})
        self.assertContains(r, "No encontramos una empresa")

    def test_cliente_sin_nit_no_entra(self):
        r = self.client.post(
            "/login/", {"correo": self.admin_a.correo, "password": CLAVE}
        )
        self.assertEqual(r.status_code, 200)
        self.assertNotIn("_auth_user_id", self.client.session)

    def test_nit_de_otra_empresa_no_sirve(self):
        self.client.post("/login/empresa/", {"nit": self.emp_b.nit})
        r = self.client.post(
            "/login/", {"correo": self.admin_a.correo, "password": CLAVE}
        )
        self.assertEqual(r.status_code, 200)
        self.assertNotIn("_auth_user_id", self.client.session)

    def test_contrasena_incorrecta(self):
        self.client.post("/login/empresa/", {"nit": self.emp_a.nit})
        r = self.client.post(
            "/login/", {"correo": self.admin_a.correo, "password": "otra"}
        )
        self.assertEqual(r.status_code, 200)

    def test_logout_solo_por_post(self):
        self.entrar(self.admin_a)
        self.assertEqual(self.client.get("/logout/").status_code, 405)
        self.assertEqual(self.client.post("/logout/").status_code, 302)
        self.assertEqual(self.client.get("/app/dashboard/").status_code, 302)

    def test_rutas_protegidas_redirigen_sin_sesion(self):
        for url in (
            "/app/dashboard/",
            "/app/colmenas/",
            "/admin/dashboard/",
            "/admin/pagos/",
        ):
            self.assertEqual(self.client.get(url).status_code, 302, url)


class HashesMk5Tests(TestCase):
    """Los hashes BCrypt de Spring ($2a$10$...) se guardan como `bcrypt$<hash>` al migrar."""

    def test_hash_bcrypt_de_mk5_valida_y_se_actualiza(self):
        empresa_rol = Rol.objects.get(nombre=Rol.ADMIN_APITECH)
        legado = bcrypt.hashpw(
            b"ClaveAntigua#1", bcrypt.gensalt(10, prefix=b"2a")
        ).decode()
        u = Usuario.objects.create_user(
            "viejo@test.co", "x", empresa_rol, nombre="Viejo"
        )
        Usuario.objects.filter(pk=u.pk).update(password="bcrypt$" + legado)
        self.assertTrue(
            self.client.login(correo="viejo@test.co", password="ClaveAntigua#1")
        )
        u.refresh_from_db()
        self.assertTrue(
            u.password.startswith("pbkdf2_sha256$"), "debe actualizarse al hash moderno"
        )
        self.assertFalse(
            self.client.login(correo="viejo@test.co", password="incorrecta")
        )
