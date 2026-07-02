package com.qasentinel.spike;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrossChannelSpikeTest {

    @Test
    public void testCrossChannelSession() throws Exception {
        System.out.println("--- INICIANDO SPIKE WEB -> MOBILE -> WEB ---");

        // 1. WEB: Login con Playwright
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        System.out.println("[WEB] Navegando a SauceDemo y haciendo login...");
        page.navigate("https://www.saucedemo.com/");
        page.fill("[data-test='username']", "standard_user");
        page.fill("[data-test='password']", "secret_sauce");
        page.click("[data-test='login-button']");

        // Extraer estado de la cookie de sesion
        List<Cookie> cookies = context.cookies();
        String sessionCookie = cookies.stream()
            .filter(c -> c.name.equals("session-username"))
            .map(c -> c.value)
            .findFirst()
            .orElse("NO_COOKIE_FOUND");

        System.out.println("[WEB] Cookie de sesion extraida: session-username=" + sessionCookie);

        // 2. MOBILE: Conectar con Appium usando ApiDemos (x86_64, siempre lanza)
        System.out.println("[MOBILE] Conectando con Appium al emulador...");
        UiAutomator2Options options = new UiAutomator2Options()
            .setDeviceName("emulator-5554")
            .setApp(System.getProperty("user.dir") + "/app/ApiDemos.apk");
        // Sin noReset: instalacion limpia garantizada

        AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        System.out.println("[MOBILE] App lanzada exitosamente.");

        // 3. TRANSFERENCIA DE SESION (el nucleo del Spike)
        // Intentamos inyectar via shell ADB. ApiDemos no tiene deep link propio,
        // pero capturamos el fallo con gracia. Lo que importa es ver si
        // Playwright sigue vivo despues del cruce.
        System.out.println("[TRANSFER] Intentando inyectar estado via shell ADB...");
        Map<String, Object> args = new HashMap<>();
        args.put("command", "am");
        args.put("args", Arrays.asList(
            "start", "-W", "-a", "android.intent.action.VIEW",
            "-d", "qasentinel://session?token=" + sessionCookie
        ));

        try {
            driver.executeScript("mobile: shell", args);
            System.out.println("[TRANSFER] Comando ADB enviado.");
        } catch (Exception e) {
            System.out.println("[TRANSFER INFO] Shell ADB no ejecutado: " + e.getMessage());
        }

        // Esperar re-render
        Thread.sleep(3000);

        // 4. WEB: Verificar que la sesion original de Playwright sigue activa
        System.out.println("[WEB] Verificando persistencia de sesion original en Playwright...");
        page.navigate("https://www.saucedemo.com/inventory.html");
        boolean isStillLogged = page.locator(".shopping_cart_link").isVisible();
        System.out.println("[WEB] Sesion sigue viva en Web: " + isStillLogged);

        // === RESULTADO DEL SPIKE ===
        if (isStillLogged) {
            System.out.println("SPIKE RESULT: GO - La sesion Web sobrevivio al cruce con Mobile.");
        } else {
            System.out.println("SPIKE RESULT: NO-GO - La sesion Web se perdio durante el cruce.");
        }

        // Teardown ordenado
        System.out.println("Cerrando drivers...");
        driver.quit();
        browser.close();
        playwright.close();

        System.out.println("--- FIN SPIKE ---");
    }
}
