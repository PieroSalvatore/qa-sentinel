# QA Sentinel – Informe Final Consolidado

Este informe integra las preguntas iniciales, las respuestas previas de los agentes y hallazgos de investigación recientes. Todas las decisiones clave ya están validadas: **JourneyOrchestrator** será el núcleo del framework y **SessionContext** la fuente de la sesión compartida. A partir de allí diseñamos la arquitectura para cada épica.

## 1. Núcleo del Framework: JourneyOrchestrator y SessionContext

- **JourneyOrchestrator = núcleo del sistema:** Controlará el flujo de un viaje de usuario secuencial (web → mobile → web). No contendrá lógica específica de Playwright o Appium, sino una interfaz genérica para “actores” multicanal. Esto permite sustituir Playwright/Appium por otras herramientas en el futuro sin reescribir la lógica de negocio.  
- **SessionContext = dueño del estado compartido:** Guardará datos de negocio clave (token, customerId, email, cookies críticas) en forma de mapa/clases Java. Cuando el viaje cambia de canal, el SessionContext exporta/importa estos datos. Así, Playwright solo escribe/lee cookies o localStorage en web, y Appium inyecta el token en el almacenamiento seguro del app (Keychain/Keystore) o mediante un deep link. **Nunca hay sincronización directa Playwright↔Appium**; siempre pasa por el SessionContext como intermediario.  
- **Implementación práctica:** Por ejemplo, el proceso de login puede hacerse con REST Assured contra la API de autenticación (OAuth2): se obtiene un JWT o cookie HTTP del backend que se almacena en SessionContext. Luego, el orquestador inyecta ese JWT/cookie en el contexto del navegador Playwright (`browserContext.addCookies(...)`) y, para la app móvil, puede lanzar un *deep link* con dicho token, o llamar al backend `/auth/webview-session` para generar una cookie WebView (ver abajo). Todo esto evita repetir el login manual en UI.  

## 2. Transferencia de Sesión Web↔Mobile (Épica-01)

Para pasar de la web nativa al app móvil sin re-login manual, combinamos estas técnicas probadas:

- **Puente token→cookie en backend:** La arquitectura propuesta aprovecha un punto intermedio en el servidor. El app móvil (o nuestro test) envía el token de identidad al endpoint `/auth/webview-session`, que lo valida y emite una cookie de sesión web de primer partido. Con esto, al cargar luego la web dentro del app (WebView) la sesión ya está activa. En QA Sentinel usamos este patrón: obtenemos el token (p.ej. con REST Assured), lo publicamos en `/auth/webview-session`, y leemos la cookie resultante. Este método refleja la solución SSO “bridge” descrita en, y respeta las barreras de seguridad móvil.  
- **Deep Links en Appium:** Para el salto inverso (web→app), se usan enlaces profundos. Registramos en la app un URL scheme propio (por ejemplo, `qa-sentinel://auth?token=...`). Al terminar la parte web con Playwright, el orquestador puede ejecutar `driver.get("qa-sentinel://auth?token="+jwt)`. El OS abre la app y esta lee el token para autenticarse internamente. Esta técnica acelera el test (bypassea UI) y evita exponer credenciales en la interfaz. Un riesgo potencial es interceptar el token en el URL, por lo que recomendamos adoptar el **Nonce Pattern** de seguridad: en lugar de pasar el token crudo, el backend genera un valor nonce de corta vida que la web usará para login. Así los tokens de largo plazo nunca viajan en la URL.  
- **Seguridad de la transferencia:** En cualquier caso, el token inyectado debe expirar rápido (ej. 5–10 min) y validarse estrictamente. Los enlaces profundos actúan como “passwords desechables”. El SessionContext mantiene todos los secretos en memoria/SQLite local, nunca en texto plano en scripts. El backend sanitiza datos sensibles antes de generar reportes o logs.

## 3. Algoritmo Inicial de Self-Healing (Épica-02)

Basado en las mejores prácticas actuales, proponemos este flujo:

- **Pipeline detect/analizar/reparar:** Al fallar un localizador durante un test (detect), se captura un *snapshot* ligero del DOM. Luego (analyze) se buscan elementos candidatos en la página actual que sean “similares” al original, usando varias heurísticas. Ejemplos de atributos considerados: 
  - **ID/nombre:** coincidencia exacta o prefijos comunes.  
  - **Texto visible y aria-label:** calcular similitud (Levenshtein) con el texto original.  
  - **Otras propiedades:** coincidencia parcial de `class`, `data-*`, posición relativa en DOM, etc. 
  Cada candidato recibe un *puntaje* sumando pesos por cada atributo coincidente (p.ej. +2 por ID exacto, +1 si el texto coincide en un 80%, etc.).  
- **Umbral de confiabilidad:** Normalizamos el puntaje y requerimos que supere un umbral inicial (~75%). Este valor sigue la lógica de gate de confianza descrita por Wibisono. Si ningún candidato alcanza el umbral, el error se propaga y el test falla (no hay “pasar silencioso”). Este umbral evita falsos positivos (e.g. confundir un botón “Cancelar” con “Enviar”).  
- **Validación semántica:** Adicionalmente, imponemos reglas de integridad semántica. Por ejemplo, si el texto del candidato difiere más de un 30% (distancia de Levenshtein) del original, se descarta automáticamente. Esto asegura que el motor no elija un elemento funcionalmente distinto.  
- **Genérico y explicable:** El algoritmo inicial será determinista (no ML) pero modular. Registra una traza de decisiones: el selector roto, candidatos encontrados, puntajes y razón de la selección. Luego se **genera un parche (pull request)** sugerido con el nuevo selector en el test (si se configura); pero en ejecución normal el cambio sólo se aplica a ese run. Esta métrica de “asertividad del healing” se monitoriza (p.ej. % de heals válidos) para evolucionar heurísticas.

## 4. Mapa Modular Maven y Artefactos (Versión 1.0)

QA Sentinel será un proyecto Maven multi-módulo. El **POM padre** (packaging `pom`) listará submódulos dedicados a cada responsabilidad. Inicialmente planificamos: 

- `sentinel-parent/pom.xml` (padre, sin código, solo modules y dependencyManagement)  
  - **`sentinel-core`** (artefacto JAR). Contiene el **JourneyOrchestrator**, **SessionContext**, API de definición de journeys (DSL fluida) y adaptadores de reporte Allure. No depende de Playwright ni Appium directamente.  
  - **`sentinel-playwright`** (JAR). Envoltorio sobre la API de Playwright Java. Proporciona acciones Web (clicks, fills) compatibles con SessionContext. Incluye listeners custom que alertan a Self-Healing (cuando esté activo).  
  - **`sentinel-appium`** (JAR). Envoltorio sobre Appium Java Client para Android/iOS. Ofrece acciones móviles (tap, swipe, deep link), también integradas con SessionContext.  
  - **`sentinel-samples`** (JAR o ejecutable). Ejemplos de test drive: un proyecto de prueba con Saucedemo (web) y App de muestra (Android), mostrando un Journey cross-channel. Incluye configuración de GitHub Actions.  
- En versiones futuras se añadirán módulos: `sentinel-healing` (lógica de reparación avanzada) y `sentinel-risk` (motor de selección por riesgos). Cada módulo tendrá su propio `pom.xml` y podrá declarar dependencias específicas. El padre mantiene las versiones Java 17+ y dependencias comunes (TestNG, Allure, etc.) centralizadas.  

Este esquema refleja la guía de proyectos multi-módulo: un POM padre con `<modules>` listando módulos (core, web, etc). Así garantizamos separación de preocupaciones, compilar independientemente el core y los adaptadores, y facilitar la extensión con nuevas épicas.

## 5. Conclusiones y Próximos Pasos

En resumen, el diseño consolidado asume que **JourneyOrchestrator + SessionContext** son la base de QA Sentinel. Sobre ellos se apoyan las épicas subsecuentes. Las técnicas investigadas garantizan una solución práctica y segura:

- **Persistencia de identidad:** Siguiendo patrones de SSO modernos, la sesión se propaga sin hacks frágiles.  
- **Self-healing seguro:** Con heurísticas multi-atributo y umbral de confianza (inspirado en [18]) evitamos cambios silenciosos indeseados.  
- **Arquitectura modular:** El proyecto Maven se configura como un parent POM con módulos específicos, permitiendo iterar la primera versión (E1) y escalar a E2/E3 sin reestructuración.  

Los siguientes pasos son prototipar la **épica 1** (Journey cross-channel) según este diseño, escribiendo un ejemplo de código (Java) del `JourneyOrchestrator` y validando en CI que un solo test maneja web y app con estado compartido. Luego se comenzará E2/E3. Con esta base sólida, QA Sentinel ofrecerá trazabilidad completa (Allure multicanal) y podrá evolucionar hacia las capacidades avanzadas planeadas.  

**Fuentes:** Patrones SSO en móvil; Deep links en Appium; Principios de self-healing; Guía Maven multi-módulo.