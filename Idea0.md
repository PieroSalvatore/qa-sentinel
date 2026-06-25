# QA Sentinel
Un framework de automatización de próxima generación construido en 3 épicas progresivas.

**Tags:**
`Java 17+` `Playwright` `Appium` `Allure Reports` `Maven` `TestNG / JUnit5` `GitHub Actions`

---

## VISIÓN DE NEGOCIO

### Problema
Las empresas invierten en automatización y terminan con frameworks frágiles: tests que se rompen ante cualquier cambio de selector, suites que no distinguen web de mobile como experiencia continua, y pipelines de CI que ejecutan 5.000 tests cuando cambiaron 3 archivos. El resultado es un ciclo donde la automatización cuesta más de mantener que el valor que genera.

### Solución
QA Sentinel es un framework modular construido en 3 épicas acumulativas: un orquestador de journeys cross-channel (web + mobile como un flujo único), un motor de auto-reparación de selectores rotos, y un analizador de riesgo que decide qué tests ejecutar basado en lo que realmente cambió. Cada épica entrega valor independiente. Juntas, forman una plataforma que las empresas querrían adoptar.

### ROI esperado por épica
* **E1:** Elimina la duplicación de suites web/mobile.
* **E2:** Reduce el tiempo de mantenimiento de tests hasta un 70%.
* **E3:** Reduce el tiempo de ejecución en CI hasta un 65% al eliminar tests irrelevantes.

---

## STACK TECNOLÓGICO

* **web automation:** Playwright Java
* **mobile automation:** Appium + UIAutomator2
* **test runner:** TestNG 7.x
* **reporting:** Allure 2.x
* **build:** Maven
* **CI/CD:** GitHub Actions
* **api / mock:** REST Assured
* **dom analysis:** Jsoup
* **git parsing:** JGit

---

## ÉPICAS

### EPIC-01: Cross-Channel Journey Framework
Orquestador de flujos de usuario que corre el mismo journey de negocio sobre web (Playwright) y mobile (Appium) con estado compartido entre sesiones. Un solo reporte Allure muestra la traza completa multicanal.

* **Pain resuelto:** Bugs de sincronización de estado entre plataformas no detectados.
* **Buyer:** QA Lead / VP Engineering en fintech o retail con app nativa.
* **Métrica de éxito:** 1 test suite cubre web + mobile; 0 duplicación de lógica de negocio.

*(Fases: Por detallar)*

### EPIC-02: Self-Healing Selector Engine
Motor que intercepta fallos de localización de elementos en tiempo real, analiza el DOM actual, aplica heurísticas de similitud y repara el selector automáticamente — registrando cada healing en Allure con el selector antiguo y el nuevo.

* **Pain resuelto:** Tests rotos por cambios de selector cuestan horas de mantenimiento por sprint.
* **Buyer:** QA Manager con suite grande (200+ tests) en producto que itera rápido.
* **Métrica de éxito:** Selector roto reparado automáticamente sin intervención humana en ≥70% de casos.

*(Fases: Por detallar)*

### EPIC-03: Risk-Based Test Selector
Analizador que parsea el diff de Git, lo cruza con un mapa de cobertura de tests, calcula el riesgo por módulo y genera una lista priorizada de qué tests ejecutar — eliminando los irrelevantes de cada corrida de CI.

* **Pain resuelto:** CI ejecuta suite completa ante cualquier commit, aunque cambió un archivo CSS.
* **Buyer:** DevOps Lead / CTO con pipelines lentos que bloquean el deploy.
* **Métrica de éxito:** Reducción ≥50% en tiempo de ejecución de CI manteniendo 0 regresiones no detectadas.

*(Fases: Por detallar)*

---

## CÓMO SE INTEGRAN LAS ÉPICAS

### EPIC-01 es la base — nunca se descarta
La arquitectura multi-módulo Maven, el SessionContext, el JourneyOrchestrator y el Allure adapter viven en EPIC-01. Las épicas 2 y 3 son extensiones que se enchufan sobre esta base sin modificar su API. Un agente que trabaje en EPIC-02 o EPIC-03 debe asumir que EPIC-01 está completa y estable.

### EPIC-02 extiende los wrappers de EPIC-01
Los HealingWrapper reemplazan transparentemente las llamadas de Playwright y Appium dentro del JourneyOrchestrator. Desde afuera, el orchestrator no sabe que está usando wrappers — los tests de EPIC-01 pasan exactamente igual, con la capacidad de auto-reparación activada por config.

### EPIC-03 opera antes de que los tests corran
El Risk Selector es un proceso pre-ejecución. Lee el estado del repositorio Git, produce un XML de subset para TestNG, y pasa el control al runner normal. No necesita saber nada de EPIC-02. El único acoplamiento es el coverage-map generado sobre los tests de EPIC-01.

---

## CONTEXTO PARA AGENTES DE IA

```yaml
project_name: QA Sentinel
language: Java 17+
build_tool: Maven (multi-module)
test_runner: TestNG 7.x
web_automation: Playwright Java (com.microsoft.playwright)
mobile_automation: Appium Java Client 9.x + UIAutomator2
reporting: Allure 2.x con custom listeners
api_testing: REST Assured
dom_parsing: Jsoup
git_parsing: JGit (org.eclipse.jgit)
persistence: JSON local o SQLite (sin servidor)
ci: GitHub Actions con emulador Android

epic_order: E1 → E2 → E3 (acumulativo, no paralelo)
phase_contract: cada fase entrega artefactos funcionales, no WIP
no_ml: true — toda lógica es determinista con pesos configurables
no_external_services: true — 0 dependencias de APIs de pago o cloud propietario
demo_app: Saucedemo (web) + Sauce Labs Demo App (Android APK, open source)
target_reviewer: QA Lead técnico o VP Engineering — el README debe hablar su idioma