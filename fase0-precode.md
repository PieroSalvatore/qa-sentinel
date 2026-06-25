# 🧠 QA SENTINEL — SPEC MAESTRO (MEMORIA GLOBAL DEL PROYECTO)

---

## 1. 🧭 Estado actual del proyecto

QA Sentinel ha evolucionado desde un concepto de framework de automatización hacia una hipótesis técnica crítica:

> ¿Es posible orquestar journeys de usuario cross-channel (Web ↔ Mobile ↔ Web) manteniendo continuidad de identidad en un entorno CI real?

Todo diseño previo (SessionContext avanzado, JourneyOrchestrator, módulos Maven, healing, risk engine) fue descartado o postergado para evitar abstracción prematura.

Actualmente el proyecto está en:

> **FASE 0 — SPIKE DE FACTIBILIDAD (EXECUTION-FIRST)**

No existe arquitectura final. Solo existe un experimento ejecutable.

---

## 2. 🎯 Hipótesis central del sistema

El sistema completo depende de una única hipótesis crítica:

> Un estado de usuario puede sobrevivir entre runtime Web (Playwright) y runtime Mobile (Appium) dentro de un pipeline CI sin intervención manual ni backend dedicado.

### Implicaciones:

- Si falla → QA Sentinel se reduce a automatización fragmentada.
- Si funciona → se habilita una nueva categoría:  
  **Cross-channel identity-aware orchestration**

---

## 3. ⚙️ Diseño actual (mínimo intencional)

### 🧪 Spike Fase 0

Flujo actual:

**WEB (Playwright)**
- Login en SauceDemo
- Extracción de cookies / estado de sesión

⬇

**TRANSFERENCIA (problema abierto)**
- Deep link con token (si existe soporte)
- ADB shell injection (experimental)
- SharedPreferences (fallback técnico)

⬇

**MOBILE (Appium)**
- Intento de reconstrucción de sesión desde estado Web

⬇

**WEB (Playwright)**
- Validación de persistencia de sesión

---

## 4. 🧪 Artefactos actuales del sistema

Solo existen dos artefactos reales:

- `CrossChannelSpikeTest.java`
- `spike-ci.yml`

### No existe aún:

- Arquitectura formal
- SessionContext
- JourneyOrchestrator
- DSL
- Modularización Maven
- Self-healing
- Risk engine
- Observabilidad

Todo lo anterior está explícitamente prohibido hasta validar el spike.

---

## 5. 🔥 Gate de validación (criterio de verdad)

El sistema solo se evalúa por un resultado binario:

### ✔ ÉXITO
- Usuario mantiene identidad Web → Mobile → Web
- Sin intervención humana
- Ejecutado en CI (GitHub Actions)
- Resultado repetible

### ✖ FALLA
- No se puede transferir identidad entre runtimes
- Dependencia de hacks no confiables
- Ruptura del estado entre plataformas

---

## 6. ⚠️ Riesgos reales identificados

### 1. Fragmentación de identidad cross-runtime
- Web: cookies / session storage
- Mobile: sandbox aislado
- No existe equivalencia directa de estado

### 2. Inestabilidad del entorno CI
- Emuladores Android flakey
- Appium inconsistente
- Timing issues frecuentes

### 3. Ausencia de puente de autenticación real
- Deep links no garantizados
- Apps demo no diseñadas para continuidad de sesión

---

## 7. 🧬 Componentes aún NO existentes (por diseño)

Estos elementos no deben existir aún:

- SessionContext formal
- JourneyOrchestrator
- Identity Layer
- Modularización Maven
- Self-healing
- Risk engine
- Observabilidad
- DSL de orquestación

Razón: no hay evidencia suficiente para justificarlos.

---

## 8. 🧭 Filosofía del proyecto

> Construir para descubrir, no diseñar para construir.

Principios:

- La arquitectura emerge del comportamiento real
- CI es el juez final del sistema
- Todo diseño es descartable hasta validación empírica
- La complejidad solo se introduce después del dolor observado

---

## 9. 🧪 Evolución futura (condicional)

Solo si el spike es exitoso:

### Fase 1 — Núcleo formal
- SessionContext mínimo (estado observable)
- JourneyOrchestrator (secuenciación real)
- Primer modelo de identidad cross-channel basado en evidencia

### Fase 2 — Observabilidad
- Integración Allure cross-channel
- Trazabilidad de estado entre runtimes
- Debugging de identidad

### Fase 3 — Productización
- Modularización Maven (solo si es necesaria)
- Empaquetado reusable
- Ejecución multi-entorno CI

### Fase 4 — Inteligencia avanzada
- Self-healing basado en fallos reales
- Risk-based test selection
- Optimización de flakiness
- Automatización adaptativa

---

## 10. 🧠 Insight clave del sistema

El problema real no es testing.

Es este:

> ¿Cómo se mantiene la continuidad de identidad de un usuario entre sistemas runtime heterogéneos?

Esto redefine el dominio:

**De:** QA Automation Framework  
**A:** Cross-Channel Identity Execution System

---

## 11. 📌 Estado resumido del proyecto

- Arquitectura: ❌ inexistente (intencional)
- Sistema ejecutable: 🧪 spike en validación
- Riesgo crítico: identidad cross-runtime
- Dependencia clave: CI reproducible
- Madurez actual: hipótesis técnica experimental

---

## 12. 🚀 Definición para cualquier IA futura

> QA Sentinel es un experimento para validar si es posible mantener identidad de usuario consistente entre Web y Mobile dentro de un pipeline CI usando Playwright + Appium sin backend dedicado, y construir desde ese comportamiento un sistema de orquestación de journeys.

---