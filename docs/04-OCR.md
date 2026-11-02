# OCR local

## Qué resuelve

Un PDF puede no tener texto por dos motivos muy distintos:

1. **Escaneado**: las páginas son imágenes. El texto extraído está vacío.
2. **Capa de texto dañada**: el PDF tiene texto, pero su tabla `ToUnicode` está rota y
   PDFBox devuelve identificadores de glifo sin sentido (ideogramas, kana, caracteres de
   la zona privada de Unicode, `U+FFFD`).

El segundo caso es el peligroso: la app «veía» texto, así que no lanzaba el OCR y la
narración leía basura. Desde 4.0-f5 ambos casos se tratan igual.

## Cómo se decide

`PdfTextQuality` evalúa cada página con una heurística sin dependencias:

- proporción de caracteres fuera del alfabeto esperado (letras latinas, cifras y
  puntuación normal, incluidas tildes, `¡¿`, comillas angulares y rayas);
- presencia de caracteres de la zona privada de Unicode;
- controles C1 y `U+FFFD`;
- páginas con otro alfabeto y ninguna letra latina.

El texto español normal pasa limpio; hay tests que fijan ese límite.

## Cómo se lanza

**☰ → Reconocer texto (OCR)**

- El diálogo indica cuántas páginas no tienen texto o lo tienen ilegible.
- **Reconocer** procesa solo esas páginas; un PDF mixto conserva su texto original.
- Muestra `12 de 340 páginas…` y se puede cancelar sin perder lo ya hecho.
- **«Forzar OCR en todas las páginas»** queda marcado por libro y manda sobre la
  detección cuando esta se equivoque.

## Cómo funciona por dentro

1. Cada página se renderiza a bitmap con `PdfRenderer`.
2. ML Kit (`text-recognition`, modelo latino incluido en el APK) reconoce el texto.
3. Se reconstruye el orden de lectura, se unen palabras cortadas por guion y se
   descartan líneas sin contenido útil.
4. Se guarda en `files/ocr/<id-del-libro>.json`, **cada 8 páginas**, para no perder
   trabajo si se interrumpe.
5. Al terminar, el lector recarga el PDF y ya se puede escuchar y buscar.

## Consumidores del texto

El OCR no es una capa aparte: alimenta a la vez

- la **narración** (`BookTextLoader`),
- la **búsqueda** (`BookSearch`),
- el **subrayado** (`PdfReader.locateLine` y `extractPageWords`).

Si una página ilegible aún no tiene OCR, el texto se devuelve **vacío** en lugar de
narrar basura.

## Coste y límites

- Unos segundos por página la primera vez; después es instantáneo (caché).
- Un escaneado de 300 páginas puede tardar varios minutos. Hazlo con el teléfono
  cargando y la app en primer plano.
- Calidad dependiente del escaneo: un PDF torcido o con letra diminuta reconoce peor.
  El zoom al 300 % sirve para comprobarlo.
- Solo PDF. Las imágenes dentro de un EPUB no se reconocen.
- La caché se pierde al desinstalar la app.
- Todo ocurre en el dispositivo: sin Internet y sin Play Services.
