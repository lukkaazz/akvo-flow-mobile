package org.akvo.appium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import io.appium.java_client.android.AndroidDriver;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.appium.java_client.remote.MobileBrowserType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Author: Ruarcc McAloon
 * Date: 20/01/15
 */
public class FieldSurveyTests {

//    TODO: Eventually remove these locator strategy reminders
//    driver.findElement(MobileBy.className("UIAButton"));
//    driver.findElement(MobileBy.xpath("//android.widget.EditText"));
//    driver.findElement(MobileBy.id("com.aut.android:id/searchButton"));
//    driver.findElement(MobileBy.AccessibilityId("Sign In"));
//    driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().className('android.widget.TextView')")) ;

    private AppiumDriver driver;
    private static final String FIELD_SURVEY_APP = "fieldsurvey-1.13.6.apk";

    @Before
    public void setUp() throws Exception {
        File classpathRoot = new File(System.getProperty("user.dir"));
        File appDir = new File(classpathRoot, "../apps/");
        File app = new File(appDir, FIELD_SURVEY_APP);

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.BROWSER_NAME, "");
        capabilities.setCapability("deviceName","6435b877");
        capabilities.setCapability("platformVersion", "4.4.2");
        capabilities.setCapability("platformName","Android");
        capabilities.setCapability("app", app.getAbsolutePath());

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
    }

    @Test
    public void testActionDisallowedWithoutUserSelected(){
        WebElement manageUsersButton = driver.findElement(By.id("com.gallatinsystems.survey.device:id/buttonText"));
        assertEquals("Manage Users", manageUsersButton.getText());

        driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'Settings')]")).click();

        driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'Download Survey')]")).click();
        driver.findElement(By.className("android.widget.EditText")).sendKeys("12345");
        driver.findElement(By.id("android:id/button1")).click();
        driver.findElement(By.className("android.widget.EditText")).sendKeys("3379117");
        driver.findElement(By.id("android:id/button1")).click();

        driver.sendKeyEvent(4);

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'1.8.0 Final All Questions Forms v. 1.0')]")).click();

        WebElement warningMessage = driver.findElement(By.id("android:id/message"));
        assertEquals("Please click the Manage Users icon and choose a user before continuing.", warningMessage.getText());
    }

    @Test
    public void testAddUserAndSelect(){
        //check there is not a user currently selected
        assertTrue(driver.findElements(By.id("com.gallatinsystems.survey.device:id/currentUserField")).size()<1);

        driver.findElement(By.id("com.gallatinsystems.survey.device:id/buttonText")).click();

        driver.sendKeyEvent(82);

        driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'Add User')]")).click();
        driver.findElement(By.id("com.gallatinsystems.survey.device:id/displayNameField")).sendKeys("LearningIsFun");
        driver.findElement(By.id("com.gallatinsystems.survey.device:id/confirm")).click();
        driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'LearningIsFun')]")).click();

        WebElement currentUser = driver.findElement(By.id("com.gallatinsystems.survey.device:id/currentUserField"));
        assertEquals("LearningIsFun",currentUser.getText());
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
    }

}


