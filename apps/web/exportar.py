import csv, io
from numbers import Number
from datetime import date
from django.http import HttpResponse
from openpyxl import Workbook
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet
from xml.sax.saxutils import escape


def seguro(value):
    if isinstance(value, (Number, date)):
        return value
    s = str(value) if value is not None else ""
    return "'" + s if s.lstrip().startswith(("=", "+", "-", "@", "\t", "\r")) else s


def generar_archivo(nombre, headers, rows, formato, descripcion=""):
    rows = [[seguro(v) for v in r] for r in rows]
    if formato == "csv":
        out = io.StringIO()
        out.write("\ufeff")
        w = csv.writer(out)
        w.writerow(headers)
        w.writerows(rows)
        response = HttpResponse(out.getvalue(), content_type="text/csv; charset=utf-8")
    elif formato == "xlsx":
        b = Workbook()
        s = b.active
        s.title = "ApiTech"
        s.append(headers)
        for r in rows:
            s.append(r)
        s.freeze_panes = "A2"
        s.auto_filter.ref = s.dimensions
        for c in s.columns:
            s.column_dimensions[c[0].column_letter].width = 24
        out = io.BytesIO()
        b.save(out)
        response = HttpResponse(
            out.getvalue(),
            content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )
    elif formato == "pdf":
        out = io.BytesIO()
        styles = getSampleStyleSheet()
        doc = SimpleDocTemplate(
            out, pagesize=landscape(A4), rightMargin=24, leftMargin=24
        )
        data = [
            [Paragraph(escape(str(v)), styles["BodyText"]) for v in r]
            for r in [headers] + rows
        ]
        t = Table(data, repeatRows=1, colWidths=[790 / len(headers)] * len(headers))
        t.setStyle(
            TableStyle(
                [
                    ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#FFC72C")),
                    ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#dddddd")),
                    ("VALIGN", (0, 0), (-1, -1), "TOP"),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                ]
            )
        )
        doc.build(
            [
                Paragraph(
                    "ApiTech · " + escape(nombre.replace("_", " ")), styles["Title"]
                ),
                Paragraph(escape(descripcion), styles["BodyText"]),
                Spacer(1, 12),
                t,
            ]
        )
        response = HttpResponse(out.getvalue(), content_type="application/pdf")
    else:
        return HttpResponse("Formato no admitido", status=400)
    response["Content-Disposition"] = f'attachment; filename="{nombre}.{formato}"'
    return response
