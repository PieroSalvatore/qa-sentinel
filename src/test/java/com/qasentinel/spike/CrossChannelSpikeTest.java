package com.qasentinel.spike;

import com.microsoft.playwright.*;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

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
        
        // Extraer estado
        List<Cookie> cookies = context.cookies();
        String sessionCookie = cookies.stream()
            .filter(c -> c.name.equals("session-username"))
            .map(c -> c.value)
            .findFirst()
            .orElse("");
            
        System.out.println("[WEB] Cookie de sesión extraída: session-username=" + sessionCookie);

        // 2. MOBILE: Levantar Appium
        System.out.println("[MOBILE] Levantando Appium y vinculando app en Emulador...");
        UiAutomator2Options options = new UiAutomator2Options()
            .setDeviceName("emulator-5554")
            .setApp(System.getProperty("user.dir") + "/app/Android.SauceLabs.Mobile.Sample.app.apk")
            .setNoReset(true); // Evitar limpiar el estado interno si logramos inyectarlo
        
        AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);

        // 3. TRANSFERENCIA DE SESIÓN (EL CORE DEL SPIKE)
        System.out.println("[TRANSFER] Intentando inyectar estado en Mobile...");
        
        /* 
           Opcion A: Vía Deep link.
           Asume que la app soporta autenticación o navegación vía scheme.
        */
        Map<String, Object> args = new HashMap<>();
        args.put("command", "am");
        args.put("args", Arrays.asList("start", "-W", "-a", "android.intent.action.VIEW", "-d", "mydemoapprn://cart?session=" + sessionCookie));
        try {
            driver.executeScript("mobile: shell", args);
            System.out.println("[TRANSFER] Deep link enviado.");
        } catch (Exception e) {
            System.out.println("[TRANSFER FAIL] Falla ejecutando shell ADB: " + e.getMessage());
        }

        System.out.println("[MOBILE] Evaluando si el estado inyectado persiste en UI...");
        // Pequeño wait para permitir re-render en React Native
        Thread.sleep(3000); 
        
        // 4. WEB: Retornar a la sesión original
        System.out.println("[WEB] Verificando persistencia de sesión original en Playwright...");
        page.navigate("https://www.saucedemo.com/inventory.html");
        boolean isStillLogged = page.locator(".shopping_cart_link").isVisible();
        System.out.println("[WEB] ¿Sesión sigue viva en Web? " + isStillLogged);
        
        // Teardown
        System.out.println("Cerrando drivers...");
        driver.quit();
        browser.close();
        playwright.close();
        
        System.out.println("--- FIN SPIKE ---");
    }
}
