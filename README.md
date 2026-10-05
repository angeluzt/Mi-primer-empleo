# Conseguir Trabajo: Mi primer empleo

Guía gamificada de empleabilidad para estudiantes y recién egresados de Latinoamérica.
App Android (Kotlin + Jetpack Compose) con generador de CV asistido por IA, revisión de
vacantes (¿es estafa?, ¿qué me falta?, mi CV adaptado) y una bitácora de entrevistas.

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
app/src/main/java/.../cv/        CV: entrevista, cliente de IA, protección de datos,
                                 verificador de cifras, detector de estafas y PDF
app/src/main/java/.../bitacora/  Bitácora de entrevistas y su lista de estudio
app/src/main/java/.../ui/        Pantallas Compose, tema y componentes de diseño
app/src/test/                    Pruebas (corren en CI antes del APK) y capturas
backend/                         Cloud Function: proxy de OpenAI + verificación de compra
prompts/                         Prompts, esquemas y modelos: compartidos por app, backend y pruebas
tools/                           Revisión de contenido y pruebas de la IA
```

### El generador de CV

Once preguntas fijas (`cv/GuionEntrevista.kt`), una sola llamada a la IA para redactar
y otra, mucho más barata, para revisar el resultado. La IA devuelve JSON, nunca un PDF:
el PDF lo dibuja el teléfono, así que cambiar de formato o corregir un dato no cuesta
otra generación. Lo que la IA redactó se puede corregir a mano en el **editor**, gratis.

**Esquema garantizado, y una red debajo.** Las llamadas usan salidas estructuradas de
OpenAI con esquema estricto (`prompts/esquemas/`), así que el modelo no puede devolver
otra forma. Aun así `cv/NormalizadorCv.kt` lee el JSON campo por campo y aguanta objetos
donde iba texto, números donde iba cadena o secciones ausentes: un modelo de respaldo o un
cambio del proveedor no deben tirar una generación pagada. Los modelos y su orden viven en
`prompts/modelos.json` (`gpt-4.1-mini`, con `gpt-4o-mini` de respaldo).

**Lo que protege a la persona no depende de que la IA obedezca:**

| Capa | Dónde | Qué hace |
|---|---|---|
| Identificaciones | `cv/ProteccionDatos.kt` | Tacha CURP, RFC, NSS, DNI, RUT, cédula, pasaporte y fecha de nacimiento **antes** de mandar nada |
| Minimización | `cv/ProteccionDatos.kt` | Nombre, teléfono, correo y enlaces no viajan a la IA (`[omitido]`); la app los repone al volver |
| Inyección | `cv/Adaptacion.kt` (`Delimitador`) | El texto de la persona y el de la vacante van delimitados como datos; los prompts dicen que no son órdenes |
| Inventos | `cv/VerificadorCv.kt` | Señala cifras y habilidades del CV que la persona nunca dijo |
| Datos de más | `cv/ProteccionDatos.kt` | Avisa si el CV terminado trae estado civil, edad o dirección exacta |
| Estafas | `cv/DetectorEstafas.kt` | Revisa la vacante en el teléfono, gratis y sin IA: cobros, depósitos, INE por WhatsApp, multinivel… |

### Herramientas para la búsqueda

- **Revisar una vacante.** Se pega el texto de la vacante. Al instante, sin IA, dice si
  tiene señales de fraude. Con la IA (gasta una generación) dice qué tanto la cubre, qué le
  falta y cómo cubrirlo, y reordena el CV para ese puesto **sin agregar nada que no esté en
  el CV**, más un mensaje para postularse. Si la vacante huele a estafa, adaptar deja de ser
  el botón principal.
- **Bitácora de entrevistas.** Qué preguntaron y cuáles no supo contestar. Agrupa las
  preguntas que se repiten entre entrevistas (`bitacora/Bitacora.kt`) y arma la lista de
  estudio sola. Nunca sale del teléfono.

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
| `pase_completo` | Pago único, no consumible | Los 10 módulos, generador de CV, 15 generaciones (generar o adaptar a una vacante), revisión, exportar PDF, bitácora, sin anuncios |
| `pase_lectura` | Pago único, no consumible | Solo la lectura completa, más barato |
| `recarga_cv_10` | Pago único, consumible | 10 generaciones más, para generar o adaptar el CV a vacantes |

> Ya no existe `plantillas_extra`: los formatos van todos incluidos, y un producto que
> se puede comprar y no entrega nada es tomarle el dinero a alguien. Si lo creaste en Play
> Console, desactívalo.

**No hay suscripciones.** `billing/Billing.kt` consulta únicamente `ProductType.INAPP`;
`SUBS` no aparece en el código. El público son estudiantes sin dinero: un cobro recurrente
genera cancelaciones, reembolsos y malas reseñas.

### Qué es gratis

- La tesis inicial y la elección de ruta
- El módulo 4 (*Dónde buscar*) **completo**
- El primer capítulo de cada módulo
- Cualquier capítulo cerrado, a cambio de ver un anuncio (hasta 3 al día)
- **Armar el CV entero, verlo y que la IA lo revise** — se paga al exportar el PDF
- **Revisar si una vacante tiene señales de fraude** — corre en el teléfono, sin IA

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

Despliega cuatro funciones: `generarCv`, `evaluarCv`, `adaptarCv` y `acreditarRecarga`.
**Vuelve a desplegar cada vez que cambie algo en `prompts/`**: la app trae su propia copia
dentro del APK, pero en producción manda lo que tenga el backend.

Requiere además una cuenta de servicio con acceso a la **Google Play Developer API**
para que `verificarCompra()` pueda validar los tokens de compra.

En Firestore solo se guardan contadores por compra (generaciones y revisiones usadas,
recargas acreditadas). Ni las respuestas ni el CV se guardan en el servidor.

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

### 5. Ver las pantallas sin emulador

CI dibuja las pantallas reales de la app en la JVM (Robolectric + Roborazzi,
`app/src/test/.../capturas/`) y las sube a la rama **`capturas-ci`**, que se sobrescribe en
cada ejecución. Para verlas: abre esa rama en GitHub, o

```bash
git fetch origin capturas-ci && git checkout origin/capturas-ci -- .   # en otra carpeta
```

Las pruebas normales corren sin las capturas con `./gradlew testDebugUnitTest -PsinCapturas`.

### 6. Antes de publicar en Play

- **Política de privacidad.** La app usa anuncios, IA y datos personales del CV: Play exige
  una URL pública. Lo que debe decir ya está escrito, sin letra chica, en
  *Ajustes → Tu privacidad*.
- **Seguridad de los datos (Data safety).** Datos personales (nombre, correo, teléfono,
  historial laboral) que se procesan para la función del CV; se mandan cifrados a nuestro
  backend y a OpenAI; no se venden ni se comparten para publicidad; la persona los borra
  desde *Ajustes → Borrar mi CV y mis notas*. AdMob recopila el identificador de publicidad.
- **AdMob** con tus identificadores reales (ver arriba) y **desactiva `plantillas_extra`** si
  lo llegaste a crear.

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
8. **A la IA solo va lo necesario.** Las identificaciones se tachan y el contacto se omite
   antes de salir del teléfono. La bitácora nunca sale. La persona puede borrar todo lo
   suyo desde Ajustes.

---

## Alcance

El contenido se adapta al país que elija la persona: **México, Colombia, Argentina,
Chile, Perú, Ecuador** y una versión genérica para el resto de Latinoamérica. Los datos
que cambian por país (instituciones, bolsas de trabajo, trámites) viven en bloques
`regional` dentro del JSON, con `generico` como respaldo obligatorio. Agregar un país es
cambiar JSON, no código; `tools/revisar_contenido.js` falla si algún bloque regional se
queda sin respaldo.
