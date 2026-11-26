from django import forms
from django.contrib.auth.password_validation import validate_password
from apps.apiario.models import Colmena, Sensor, ProduccionMiel, UNIDADES
from apps.monitoreo.models import RangoUmbral
from apps.empresas.models import Empresa
from apps.suscripciones.models import Plan
from apps.usuarios.models import Usuario, Rol


class ColmenaForm(forms.ModelForm):
    class Meta:
        model = Colmena
        fields = ["nombre", "ubicacion", "estado"]


class SensorForm(forms.ModelForm):
    class Meta:
        model = Sensor
        fields = ["codigo", "tipo", "modelo", "fabricante"]


class ProduccionForm(forms.ModelForm):
    class Meta:
        model = ProduccionMiel
        fields = ["colmena", "cantidad", "fecha", "observaciones"]
        widgets = {
            "fecha": forms.DateInput(attrs={"type": "date"}),
            "observaciones": forms.Textarea(attrs={"rows": 3}),
        }

    def __init__(self, *args, empresa, **kw):
        super().__init__(*args, **kw)
        self.fields["colmena"].queryset = Colmena.objects.filter(
            empresa=empresa, archivada=False
        )

    def clean_cantidad(self):
        v = self.cleaned_data["cantidad"]
        if v <= 0:
            raise forms.ValidationError("La cantidad debe ser mayor que cero.")
        return v


class UmbralForm(forms.ModelForm):
    estado = forms.ChoiceField(choices=[("activo", "Activo"), ("inactivo", "Inactivo")])

    class Meta:
        model = RangoUmbral
        fields = ["tipo_medicion", "valor_min", "valor_max", "estado"]

    def clean(self):
        d = super().clean()
        if (
            d.get("valor_min") is not None
            and d.get("valor_max") is not None
            and d["valor_min"] >= d["valor_max"]
        ):
            raise forms.ValidationError("El mínimo debe ser menor que el máximo.")
        return d


class EmpresaForm(forms.ModelForm):
    class Meta:
        model = Empresa
        fields = ["nit", "nombre_empresa", "correo", "telefono", "direccion", "estado"]


class PlanForm(forms.ModelForm):
    estado = forms.ChoiceField(choices=[("activo", "Activo"), ("inactivo", "Inactivo")])

    class Meta:
        model = Plan
        fields = [
            "nombre_plan",
            "precio",
            "duracion_dias",
            "max_colmenas",
            "max_sensores",
            "estado",
        ]

    def clean(self):
        d = super().clean()
        for k in ("duracion_dias", "max_colmenas", "max_sensores"):
            if d.get(k) is not None and d[k] < 1:
                self.add_error(k, "Debe ser mayor que cero.")
        if d.get("precio") is not None and d["precio"] < 0:
            self.add_error("precio", "No puede ser negativo.")
        return d


class UsuarioForm(forms.ModelForm):
    clave = forms.CharField(
        label="Contraseña inicial (solo al crear)",
        required=False,
        widget=forms.PasswordInput,
    )

    class Meta:
        model = Usuario
        fields = ["nombre", "apellido", "correo", "rol", "estado"]

    def __init__(self, *args, internos=False, **kw):
        super().__init__(*args, **kw)
        self.fields["rol"].queryset = Rol.objects.filter(
            nombre__in=Rol.INTERNOS if internos else Rol.CLIENTES
        )
        if self.instance.pk:
            self.fields.pop("clave")

    def clean_correo(self):
        return self.cleaned_data["correo"].lower().strip()

    def clean(self):
        d = super().clean()
        if not self.instance.pk:
            if not d.get("clave"):
                self.add_error("clave", "La contraseña es obligatoria.")
            else:
                validate_password(d["clave"], self.instance)
        return d


class PerfilForm(forms.ModelForm):
    class Meta:
        model = Usuario
        fields = ["nombre", "apellido", "correo"]

    def clean_correo(self):
        return self.cleaned_data["correo"].lower().strip()
