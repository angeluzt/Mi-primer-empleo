# Mi Primer Empleo

Guía gamificada de empleabilidad para estudiantes y recién egresados en México y
Latinoamérica. App Android (Kotlin + Jetpack Compose) con generador de CV asistido por IA.

**Sin suscripciones. Sin publicidad.** Un solo pago desbloquea todo para siempre.

---

## Cómo está construido

```
app/src/main/assets/contenido/   Los 9 módulos en JSON (fuente única de contenido)
app/src/main/java/.../model/     Modelos de contenido y CV
app/src/main/java/.../data/      Carga de contenido y progreso local (DataStore)
app/src/main/java/.../billing/   Play Billing — SOLO productos de pago único
app/src/main/java/.../cv/        Generador de CV: modelo, cliente y armado del PDF
app/src/main/java/.../ui/        Pantallas Compose y renderizador de bloques
backend/                         Cloud Function: proxy de OpenAI + verificación de compra
tools/                           Render de pantallas para revisión y Play Store
docs/capturas/                   PNGs de las pantallas
```

### La guía funciona sin internet

Todo el contenido viaja dentro del APK como JSON. No hay costo de servidor por lectura y
la app sirve aunque la persona no tenga datos. Solo el generador de CV usa red.

### El contenido es fuente única

Los bloques (`parrafo`, `cita`, `alerta`, `tabla`, `banderas`, `recursos`, `accion`…) son
neutrales respecto al medio. El mismo JSON puede alimentar la app y, más adelante, un libro
en PDF sin reescribir nada.

---

## Modelo de negocio

| Producto | Tipo | Qué incluye |
|---|---|---|
| `pase_completo` | Pago único, no consumible | Los 9 módulos, generador de CV, 15 generaciones, todas las herramientas |
| `recarga_cv_10` | Pago único, consumible | 10 generaciones más de CV |
| `plantillas_extra` | Pago único, consumible | Plantillas adicionales de CV |

**No hay suscripciones.** `billing/Billing.kt` consulta únicamente `ProductType.INAPP`;
`SUBS` no aparece en el código. El público son estudiantes sin dinero: un cobro recurrente
genera cancelaciones, reembolsos y malas reseñas.

### Qué es gratis

- La tesis inicial y la elección de ruta
- El módulo 4 (*Dónde buscar*) **completo**
- El primer capítulo de cada módulo
- **Armar el CV entero y verlo** — se paga al exportar el PDF

El muro está donde la persona ya invirtió su trabajo, no antes de que vea el valor.

### Afiliados

Los enlaces a cursos de pago están marcados con la etiqueta `AFILIADO` en la interfaz.
La transparencia es obligatoria y no opcional: solo se recomienda lo que sirve.

---

## Configuración

### 1. La app

```bash
# Requiere Android Studio / SDK. Apunta al backend desplegado:
./gradlew assembleRelease -PbackendUrl=https://TU-REGION-TU-PROYECTO.cloudfunctions.net
```

### 2. El backend

> La API key de OpenAI **nunca** va en el APK. Un APK se descompila en minutos.
> El backend existe para eso y para verificar la compra antes de gastar tokens.

```bash
cd backend
npm install
firebase functions:secrets:set OPENAI_API_KEY
firebase deploy --only functions
```

Requiere además una cuenta de servicio con acceso a la **Google Play Developer API**
para que `verificarCompra()` pueda validar los tokens de compra.

### 3. Productos en Play Console

Crea los tres productos de la tabla de arriba como **productos integrados en la aplicación**
(no suscripciones), con los IDs exactos.

### 4. Regenerar las capturas

```bash
node tools/capturas.js
```

---

## Reglas del producto que no se negocian

1. **El CV no inventa nada.** La IA solo reformula lo que la persona dijo. Un CV inflado
   se cae en la primera entrevista. La regla vive en `backend/index.js`.
2. **No se promete empleo.** Se promete preparación. Prometer trabajo viola las políticas
   de Play y es mentira.
3. **Los afiliados se declaran.** Siempre visibles en la interfaz.
4. **La seguridad de la persona va primero.** El módulo 5 cubre estafas, robo de identidad
   y riesgo físico en entrevistas, e incluye a dónde acudir si ya ocurrió.

---

## Alcance

El contenido está escrito para **México** (IMSS, PROFEDET, CONDUSEF, bolsas locales).
Expandir a otros países es cambiar JSON, no código.
