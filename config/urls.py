from django.contrib import admin
from django.urls import include, path

urlpatterns = [
    path("admin-tecnico/", admin.site.urls),
    path("", include("apps.web.urls")),
]
