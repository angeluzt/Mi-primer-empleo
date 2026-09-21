# Conseguir Trabajo: Mi primer empleo

Guía gamificada de empleabilidad para estudiantes y recién egresados de Latinoamérica.
App Android (Kotlin + Jetpack Compose) con generador de CV asistido por IA.

**Sin suscripciones.** Un solo pago desbloquea todo para siempre. Los únicos anuncios
son con recompensa, máximo tres al día, y solo si la persona toca el botón: nunca hay
uno encima de algo que ya pidió.

---

## Cómo está construido

```
app/src/main/assets/contenido/   Los 10 módulos en JSON (fuente única de contenido)
app/src/main/java/.../model/     Modelos de contenido y CV
app/src/main/java/.../data/      Contenido, progreso local (DataStore) y anuncios
app/src/main/java/.../billing/   Play Billing — SOLO productos de pago único
app/src/main/java/.../cv/        Generador de CV: modelo, cliente y armado del PDF
app/src/main/java/.../ui/        Pantallas Compose y renderizador de bloques
app/src/test/                    Pruebas del normalizador (corren en CI antes del APK)
backend/                         Cloud Function: proxy de OpenAI + verificación de compra
prompts/                         Los prompts, compartidos por app, backend y pruebas
tools/                           Revisión de contenido, capturas y pruebas de la IA
docs/capturas/                   PNGs de las pantallas
```

### El generador de CV

Once preguntas fijas (`cv/GuionEntrevista.kt`), una sola llamada a la IA para redactar
y otra, mucho más barata, para revisar el resultado. La IA devuelve JSON, nunca un PDF:
el PDF lo dibuja el teléfono, así que cambiar de formato o corregir un dato no cuesta
otra generación.

**La IA no garantiza el esquema.** Un mismo prompt a veces devuelve `"logros": ["texto"]`
y a veces `"logros": [{"accion": "texto"}]`; lo segundo ya tiró una generación pagada en
un teléfono real. Por eso `cv/NormalizadorCv.kt` lee el JSON campo por campo y aguanta
objetos donde iba texto, números donde iba cadena, secciones ausentes y nombres de campo
en inglés. Nunca inventa contenido: lo único que rellena son los datos de contacto, y
solo copiándolos de lo que la propia persona escribió. `app/src/test/` cubre cada forma
torcida que hemos visto.

**Diez diseños de PDF**, con 8 colores y 4 tipos de letra: 640 combinaciones. No son
diez tonos del mismo papel; cambian el encabezado, cómo se separan las secciones y
cuánto aire queda. Los dos diseños con barra lateral avisan que los filtros automáticos
(ATS) los revuelven, en vez de esconderlo.

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
| `pase_completo` | Pago único, no consumible | Los 10 módulos, generador de CV, 15 generaciones, exportar PDF, sin anuncios |
| `pase_lectura` | Pago único, no consumible | Solo la lectura completa, más barato |
| `recarga_cv_10` | Pago único, consumible | 10 generaciones más de CV |
| `plantillas_extra` | Pago único, consumible | Sin uso hoy: las 640 combinaciones van incluidas |

**No hay suscripciones.** `billing/Billing.kt` consulta únicamente `ProductType.INAPP`;
`SUBS` no aparece en el código. El público son estudiantes sin dinero: un cobro recurrente
genera cancelaciones, reembolsos y malas reseñas.

### Qué es gratis

- La tesis inicial y la elección de ruta
- El módulo 4 (*Dónde buscar*) **completo**
- El primer capítulo de cada módulo
- Cualquier capítulo cerrado, a cambio de ver un anuncio (hasta 3 al día)
- **Armar el CV entero, verlo y que la IA lo revise** — se paga al exportar el PDF

El muro está donde la persona ya invirtió su trabajo, no antes de que vea el valor.

### Afiliados

Los enlaces a cursos de pago están marcados con la etiqueta `AFILIADO` en la interfaz.
La transparencia es obligatoria y no opcional: solo se recomienda lo que sirve.

---

## Probar la app en tu teléfono

No necesitas instalar nada. Cada push dispara el workflow **APK de prueba**:

1. Entra a la pestaña **Actions** del repositorio en GitHub.
2. Abre la ejecución más reciente de *APK de prueba*.
3. Descarga el artefacto **`mi-primer-empleo-debug`** (es un .zip con el APK dentro).
4. Pásalo a tu Android, descomprime e instala. Tendrás que permitir
   *Instalar apps de fuentes desconocidas* para tu gestor de archivos.

> **El APK de depuración desbloquea todo el contenido.** Play Billing no funciona en
> una app instalada a mano, así que sin eso no habría forma de probar lo de paga.
> La bandera `DESBLOQUEO_PRUEBA` vale `true` solo en `debug` y `false` en `release`.

Qué funciona y qué no en ese APK:

| | Estado |
|---|---|
| Los 10 módulos y toda la lectura | Funciona, incluso sin internet |
| Nivel de empleabilidad y progreso | Funciona |
| Enlaces a cursos y recursos | Funciona |
| Exportar el CV a PDF y compartirlo | Funciona |
| Anuncios con recompensa | Funciona, con los anuncios de prueba de Google |
| Pantallas de compra | Se ven, pero no cobran |
| Generador de CV con IA | **Necesita el backend, o pegar una llave en Ajustes** |

> Para probar la IA sin desplegar nada: **Ajustes → pega tu llave de OpenAI**. Ese camino
> existe solo en `debug` (`LLAVE_LOCAL_PERMITIDA`); en `release` el compilador borra la
> rama entera y la llave vive únicamente en el backend.

## Configuración

### 1. La app

```bash
# Requiere Android Studio / SDK. Apunta al backend desplegado:
./gradlew assembleRelease \
  -PbackendUrl=https://TU-REGION-TU-PROYECTO.cloudfunctions.net \
  -PadmobAppId=ca-app-pub-XXXXXXXX~YYYYYYYY \
  -PadmobRecompensado=ca-app-pub-XXXXXXXX/ZZZZZZZZ
```

> **Los identificadores de AdMob son obligatorios para publicar.** Sin `-PadmobAppId` se
> compila con el identificador **de prueba** de Google: los anuncios se ven, pero no
> pagan un centavo. Nunca pruebes con tus identificadores reales: Google suspende cuentas
> por los clics del propio desarrollador.

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

### 4. Probar la IA sin Android

```bash
export OPENAI_API_KEY=sk-...
node tools/probar_cv.js                  # genera un CV y luego pide su revisión
node tools/probar_cv.js mis_datos.json   # con tus propias respuestas
node tools/probar_cv.js --simular        # sin gastar tokens
node tools/probar_cv.js --simular-roto   # con una respuesta fuera de esquema
```

Usa los mismos prompts que la Cloud Function, reporta lo que costó en tokens, avisa si
la IA se salió del esquema y audita las cifras que no aparecen en las respuestas: es la
red contra un CV inflado.

### 5. Regenerar las capturas

```bash
node tools/capturas.js
```

---

## Reglas del producto que no se negocian

1. **El CV no inventa nada.** La IA solo reformula lo que la persona dijo. Un CV inflado
   se cae en la primera entrevista. La regla vive en `prompts/generar_cv.txt`, y
   `tools/probar_cv.js` audita las cifras que aparecen sin haber sido dichas.
2. **No se promete empleo.** Se promete preparación. Prometer trabajo viola las políticas
   de Play y es mentira.
3. **Los afiliados se declaran.** Siempre visibles en la interfaz.
4. **La seguridad de la persona va primero.** El módulo 5 cubre estafas, robo de identidad
   y riesgo físico en entrevistas, e incluye a dónde acudir si ya ocurrió.
5. **Nunca hay suscripción.** Solo `ProductType.INAPP`.
6. **La publicidad no interrumpe.** Solo anuncios con recompensa, que la persona pide
   tocando un botón, con tope diario. Nada de intersticiales.
7. **La llave de OpenAI no viaja en el APK.** Un APK se descompila en minutos.

---

## Alcance

El contenido se adapta al país que elija la persona: **México, Colombia, Argentina,
Chile, Perú, Ecuador** y una versión genérica para el resto de Latinoamérica. Los datos
que cambian por país (instituciones, bolsas de trabajo, trámites) viven en bloques
`regional` dentro del JSON, con `generico` como respaldo obligatorio. Agregar un país es
cambiar JSON, no código; `tools/revisar_contenido.js` falla si algún bloque regional se
queda sin respaldo.
