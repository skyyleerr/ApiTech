from django.contrib.auth.base_user import AbstractBaseUser, BaseUserManager
from django.contrib.auth.models import PermissionsMixin
from django.core.exceptions import ValidationError
from django.db import models
from django.db.models import Q


class Rol(models.Model):
    ADMIN_APITECH = "Admin_ApiTech"
    EMPLEADO_APITECH = "Empleado_ApiTech"
    ADMIN_CLIENTE = "Admin_Cliente"
    EMPLEADO_CLIENTE = "Empleado_Cliente"
    INTERNOS = (ADMIN_APITECH, EMPLEADO_APITECH)
    CLIENTES = (ADMIN_CLIENTE, EMPLEADO_CLIENTE)

    nombre = models.CharField(max_length=50, unique=True)
    descripcion = models.CharField(max_length=200, blank=True)
    permisos = models.ManyToManyField("Permiso", blank=True, related_name="roles")

    class Meta:
        db_table = "roles"

    def __str__(self):
        return self.nombre

    @property
    def es_interno(self):
        return self.nombre in self.INTERNOS


class Permiso(models.Model):
    codigo = models.CharField(max_length=60, unique=True)
    descripcion = models.CharField(max_length=200, blank=True)

    class Meta:
        db_table = "permisos"

    def __str__(self):
        return self.codigo


class UsuarioManager(BaseUserManager):
    use_in_migrations = True

    def create_user(self, correo, password, rol, empresa=None, **extra):
        if not correo:
            raise ValueError("El correo es obligatorio")
        if isinstance(rol, str):
            rol = Rol.objects.get(nombre=rol)
        usuario = self.model(correo=correo, rol=rol, empresa=empresa, **extra)
        usuario.set_password(password)
        usuario.save()
        return usuario

    def create_superuser(self, correo, password=None, **extra):
        rol, _ = Rol.objects.get_or_create(nombre=Rol.ADMIN_APITECH)
        extra.setdefault("nombre", "Administrador")
        extra.update(is_staff=True, is_superuser=True)
        return self.create_user(correo, password, rol, **extra)


class Usuario(AbstractBaseUser, PermissionsMixin):
    ACTIVO, INACTIVO = "activo", "inactivo"
    ESTADOS = [(ACTIVO, "Activo"), (INACTIVO, "Inactivo")]

    empresa = models.ForeignKey(
        "empresas.Empresa",
        null=True,
        blank=True,
        on_delete=models.PROTECT,
        related_name="usuarios",
    )
    rol = models.ForeignKey(Rol, on_delete=models.PROTECT, related_name="usuarios")
    # Derivado del rol; permite que la BD garantice "interno ⇔ sin empresa" (antes: triggers).
    es_interno = models.BooleanField(default=False, editable=False)
    nombre = models.CharField(max_length=100)
    apellido = models.CharField(max_length=100, blank=True)
    correo = models.EmailField(max_length=150, unique=True)
    estado = models.CharField(max_length=20, choices=ESTADOS, default=ACTIVO)
    is_staff = models.BooleanField(default=False)  # solo Django Admin técnico
    fecha_registro = models.DateTimeField(auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    USERNAME_FIELD = "correo"
    REQUIRED_FIELDS = ["nombre"]
    objects = UsuarioManager()

    class Meta:
        db_table = "usuarios"
        constraints = [
            models.CheckConstraint(
                condition=(
                    Q(es_interno=True, empresa__isnull=True)
                    | Q(es_interno=False, empresa__isnull=False)
                ),
                name="ck_usuario_rol_empresa",
            ),
        ]

    def __str__(self):
        return self.correo

    @property
    def is_active(self):
        return self.estado == self.ACTIVO

    @property
    def nombre_rol(self):
        return self.rol.nombre

    def validar_rol_empresa(self):
        """Réplica de trg_usuarios_empresa_ins/upd de MK5."""
        if self.rol.nombre in Rol.INTERNOS and self.empresa_id is not None:
            raise ValidationError(
                "Los usuarios de ApiTech no deben tener empresa asignada."
            )
        if self.rol.nombre in Rol.CLIENTES and self.empresa_id is None:
            raise ValidationError("Los usuarios de cliente requieren una empresa.")

    def save(self, *args, **kwargs):
        self.correo = (self.correo or "").strip().lower()
        self.validar_rol_empresa()
        self.es_interno = self.rol.nombre in Rol.INTERNOS
        super().save(*args, **kwargs)
