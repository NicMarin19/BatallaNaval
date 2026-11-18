# 🚢 Batalla Naval 3D - jMonkeyEngine (Java)

Simulador táctico naval completo de **Batalla Naval (Battleship)** con interfaz gráfica 3D desarrollada en **Java** sobre **jMonkeyEngine 3 (jME3)**.

El juego incluye una pantalla de inicio con escena marina animada, un HUD táctico limpio, un menú de pausa/resumen con datos de la partida y ajustes compartidos entre menús.

---

## 🌟 Novedades del Rediseño Visual y Arquitectura

### 🎨 Modelos 3D Rediseñados (Bocetos de Alta Fidelidad)
Todos los navíos han sido rediseñados desde cero con proas afiladas en cuña (*Wedge Cutwater Bow*), acabados militares y detalles de escala:
1. 🛳️ **Portaviones Nuclear (5 celdas)**:
   - Proa lanzada con cubierta de vuelo angular orientada a 9 grados.
   - Pista central balizada, líneas transversales de cables de frenado de apontaje (*arresting wires*).
   - Isla de mando moderna con paneles de radar plano AESA, puente de control *Pri-Fly* tintado en cian óptico, cúpula radomo y mástil de comunicaciones.
   - Montajes antimisil *Phalanx CIWS* en plataformas laterales (*sponsons*).
   - 3 aviones de combate navales a escala (estilo F-35C) con alas delta y timones verticales inclinados.
2. ⚓ **Acorazado Pesado (4 celdas)**:
   - Proa *clipper* afilada y ciudadela blindada escalonada con telémetro óptico de dirección de tiro.
   - Tres torretas de artillería pesada con cañones triples estriados de 16 pulgadas y manteletes protectores (dos a proa, una a popa).
   - Chimeneas gemelas inclinadas con deflectores de humo.
   - Baterías secundarias dobles de 5 pulgadas en las bandas.
3. 🚢 **Crucero de Misiles Guiados (3 celdas)**:
   - Proa aerodinámica furtiva.
   - Cañón rápido stealth en el castillo de proa.
   - Cuadrícula de celdas de lanzamiento vertical de misiles VLS (*Vertical Launch System*).
   - Paneles de radar de barrido electrónico de fase *Aegis SPY-1* en las superestructuras.
   - Lanzadores cuádruples de misiles antibuque *Harpoon* en ángulo en la zona media.
   - Helipuerto en popa con círculo de apontaje balizado.
4. 🌊 **Submarino de Ataque (3 celdas)**:
   - Casco hidrodinámico en gota de agua (*teardrop*) con revestimiento anecoico negro mate.
   - Domo de proa acústico semiesférico.
   - Vela con timones de inmersión en la torre (*fairwater dive planes*) y periscopios optrónicos retráctiles.
   - Popa cónica con propulsor *pump-jet* carenado y timones en cruz cruciforme.
5. 🚤 **Destructor Rápido (2 celdas)**:
   - Proa afilada de alta velocidad con líneas de bajo perfil de radar.
   - Cañón naval rápido cubierto con capota térmica.
   - Tubos lanzatorpedos triples en cubierta.
   - Mástil con radomo de combate.

---

### 🌐 Mejoras Visuales, Océano y Ambientación
- **Océano procedural multicapa**: textura base generada con ruido fBm anisotrópico y trenes de ola con fase deformada. Tres capas de detalle (oleaje medio, oleaje fino y espuma) se desplazan a velocidades distintas para dar sensación de mar vivo sin bandas repetidas.
- **Brillo solar dinámico**: destellos y charcos de luz sobre el agua alineados con la posición del sol.
- **Cúpula de cielo con degradado** y niebla atmosférica suave para fundir el horizonte.
- **Flota decorativa**: 20 buques de guerra navegan por los flancos y el horizonte, con estelas, luces de navegación, balanceo y deriva.
- **Retícula Holográfica 3D (`TacticalReticle3D`)**: al pasar el cursor sobre una casilla del radar enemigo se despliega una retícula militar tridimensional con anillo láser, corchetes tácticos y haz vertical pulsante.
- **Plataformas de Combate Offshore**: bordes con franjas de advertencia *hazard stripes* (amarillo y negro), torretas de amarre y balizas luminosas.
- **Marcadores 3D Vivos**:
  - 💧 **Agua (Fallo)**: boya de balizamiento blanca/naranja con luz pulsante y aro de ondulación concéntrica.
  - 🔥 **Tocado (Impacto)**: cráter carbonizado, lengua de fuego en dos capas con núcleo brillante y columna de humo.
  - 💥 **Hundido**: navío escorado 12 grados en el agua, envuelto en llamas críticas con cruz de peligro militar.

---

### 🎯 Mecánicas de Combate

#### 💬 Comunicado de guerra: frases al hundir un barco
- Al hundir completamente un navío enemigo aparece una **tarjeta de comunicaciones** con una frase española aleatoria, al estilo de *"aparentemente 🗣️"*.
- El repertorio (`PhraseBank`) contiene **150 frases únicas** de humor, jerga marinera y guiños de internet.
- La tarjeta incluye un icono de voz procedural, una franja dorada y se oculta automáticamente a los 5,5 segundos.
- La frase se ajusta al ancho de la tarjeta y los emojis no soportados por la fuente bitmap se filtran; el icono de voz mantiene el guiño visual.

#### 🧭 Sistema de ayudas: pista luminosa
- Tras **más de 3 fallos consecutivos** (es decir, en el cuarto fallo seguido), el sistema elige una casilla de un **barco enemigo todavía intacto** y despliega una baliza 3D pulsante sobre ella.
- La pista permanece visible **8 segundos** o hasta que el jugador dispara sobre la casilla marcada.
- El contador de fallos consecutivos se reinicia al acertar, al hundir un barco y después de entregar una pista.
- La baliza (`HintGlow3D`) combina anillos, haz vertical y núcleo flotante para destacar sobre el radar enemigo.

#### 🎲 Habilidad especial: dados de guerra
- Cada **8 rondas completas** se habilita la habilidad táctica de **Dados de Guerra** (`[LANZAR DADOS]` o tecla `X`).
- **Cámara automática en primer plano**: al lanzar los dados, la cámara se desplaza suave y automáticamente hacia el centro del mar entre ambos tableros (`X=23, Y=4.2, Z=15.2`), colocándose cerca de los dados para capturar el vuelo parabólico, los giros 3D y el chapoteo en el agua.
- **Tabla táctica de telemetría y probabilidades aumentadas**: al asentarse los dados se despliega una tabla militar que detalla:
  - Los números obtenidos en cada dado (**Dado 1**, **Dado 2** y suma total sobre 12).
  - La probabilidad base previa (0%) frente a la nueva probabilidad calculada (`(Dado 1 + Dado 2) / 12`).
  - El **incremento porcentual exacto** de probabilidad ganado.
  - La activación del **Doble Disparo** para el turno.
- Al pulsar **[CONTINUAR AL COMBATE]**, la cámara regresa fluidamente a la perspectiva táctica previa y el turno continúa con la mejora armada.

#### 💾 Sistema de partidas guardadas en carpeta y reanudar
- Todas las partidas se almacenan como archivos `.sav` estructurados dentro de la carpeta dedicada **`partidas/`**.
- Desde el menú principal, al pulsar **REANUDAR (CARPETA)** se abre un explorador modal con todas las partidas guardadas en la carpeta:
  - Muestra fecha/hora de guardado, ronda actual, barcos aliados operativos, contactos enemigos destruidos, precisión y tiempo transcurrido.
  - Permite hacer clic en **[JUGAR]** en cualquiera de las partidas para reanudar el combate de inmediato.
  - Permite **[BORRAR]** partidas obsoletas y actualizar la lista en tiempo real.
- Durante el combate, abriendo el menú de pausa (`ESC`), el jugador puede pulsar **[GUARDAR PARTIDA]** para almacenar el estado en `partidas/` en cualquier momento, o pulsar **[REANUDAR CARPETA]** para cambiar a otra partida disponible.

---

### 🖥️ Interfaz de Usuario

#### Pantalla de inicio
- **NUEVA PARTIDA**: inicia una partida desde cero.
- **REANUDAR PARTIDA**: vuelve a la partida en curso (se habilita cuando existe una).
- **AJUSTES**: panel modal con las opciones del sistema.
- **CRÉDITOS**: información del proyecto y tecnologías usadas.
- **SALIR DEL JUEGO**.

La pantalla de inicio muestra la escena 3D en movimiento (mar, barcos y radar decorativo) y un panel de comando con estado de la partida.

#### HUD táctico limpio
- Barra superior con el estado de la partida, la cámara activa, la **ronda actual** y la cuenta regresiva para la habilidad de dados.
- **Panel de flota aliada** con el estado de cada buque y barras de salud `[####-]`.
- **Panel de radar enemigo** con contactos desconocidos/hundidos.
- **Telemetría de tiro**: disparos, impactos y precisión.
- **Tarjeta de comunicado de guerra** con la frase del último barco hundido.
- Barra inferior de despliegue: **AUTO-COLOCAR**, **ROTAR** e **INICIAR COMBATE**.
- Botón permanente **[ESC] MENÚ**.

#### Menú de pausa / resumen (ESC)
- **Resumen de partida**: estado, tiempo, disparos, impactos, precisión, flota aliada y flota enemiga.
- **Controles** completos.
- Acciones: **REANUDAR**, **REINICIAR PARTIDA**, **AJUSTES** y **MENÚ PRINCIPAL**.
- `ESC` cierra los ajustes o reanuda la partida.

#### Ajustes compartidos
Disponibles tanto en el menú de inicio como en el de pausa:
- **SONIDO: ACTIVADO / SILENCIO**.
- **AGUA: ALTA / MEDIA** (activa o desactiva espuma, reflejos y oleaje adicional).
- **AYUDAS: VISIBLES / OCULTAS** (textos de control del HUD).

---

### 🧪 Suite de Pruebas Automatizadas (SOLID / TDD)
- **`BoardTest`**: verificación de colocación horizontal, vertical, bordes del mapa, detección de colisiones, auto-colocación de 17 celdas, resolución de impactos y selección de celdas de barco aún disparables para las pistas.
- **`EnemyAITest`**: verificación de tiro único sin repeticiones de la IA y transición al algoritmo de búsqueda adyacente/eje (*hunt & target mode*).
- **`PhraseBankTest`**: verifica que el repertorio supere las 100 frases únicas, que las frases aleatorias pertenezcan al repertorio y que el texto visible conserve los caracteres españoles y filtre los emojis.
- **`GameSaveManagerTest`**: verifica la persistencia en la carpeta `partidas/`, serialización y deserialización completa de tableros, barcos, estado de IA, listado ordenado y eliminación de archivos.
- **`CameraControllerTest`**: verifica la transición cinemática a primer plano de dados y la restauración de la vista previa de combate.

---

## ⌨️ Controles

| Acción | Control |
|---|---|
| **Disparar / Colocar Barco** | Clic izquierdo del ratón |
| **Abrir / cerrar menú de pausa** | `ESC` o botón `[ESC] MENÚ` |
| **Rotar Barco** | Clic derecho del ratón o tecla `R` |
| **Auto-Colocar Flota** | Tecla `ESPACIO` o botón en pantalla |
| **Mover Cámara** | Teclas `W`, `A`, `S`, `D` o flechas |
| **Zoom Acercar / Alejar** | Rueda del ratón |
| **Cambiar Modo de Cámara** | Tecla `C` o botón en pantalla |
| **Captura de pantalla** | `F12` (se guarda en `screenshots/`) |

---

## 🚀 Cómo Ejecutar el Juego

### Requisitos
- **JDK 17 o superior** (recomendado **JDK 21**).
- Windows con drivers OpenGL actualizados.

### Opción 1: Lanzador directo (recomendado)
Haz doble clic sobre **`jugar.bat`** o **`run.bat`**. El lanzador detecta automáticamente un JDK 21/17 instalado, compila el proyecto si hace falta y arranca el juego.

### Opción 2: Desde la terminal con el ejecutable JAR
```powershell
java -jar target\batalla-naval-jme-1.0.0.jar
```

### Opción 3: Ejecutar con Maven
```powershell
.\apache-maven-3.9.6\bin\mvn.cmd exec:java
```

### Opción 4: Ejecutar la suite de pruebas unitarias
```powershell
.\apache-maven-3.9.6\bin\mvn.cmd test
```

### Opción 5: Compilar el JAR ejecutable
```powershell
.\apache-maven-3.9.6\bin\mvn.cmd -DskipTests package
```

---

## 📸 Capturas
Pulsa `F12` durante la partida o en los menús. Las imágenes se guardan como `screenshots/batalla1.png`, `batalla2.png`, etc.
