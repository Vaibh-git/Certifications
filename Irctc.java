import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;

import javax.imageio.ImageIO;

import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.io.FileHandler;
import org.testng.annotations.*;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IrctcLoginPage {

    private WebDriver driver;
    private final String baseUrl = "https://www.irctc.co.in/nget/train-search";
    private final String userId = "golu07239";
    private final String password = "SangAnu123@";
    private final Logger logger = LoggerFactory.getLogger(IrctcLoginPage.class);

    @BeforeClass
    public void setUp() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--disable-notifications");
        driver = new EdgeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().window().maximize();
    }

    @Test
    public void loginSuccess() {
        try {
            driver.get(baseUrl);

            // Navigate to login
            driver.findElement(By.xpath("//div[@class='h_menu_drop_button hidden-xs']//i[@class='fa fa-align-justify']")).click();
            driver.findElement(By.cssSelector("button[class='search_btn']")).click();

            // Enter credentials
            driver.findElement(By.xpath("//input[@formcontrolname='userid']")).sendKeys(userId);
            driver.findElement(By.xpath("//input[@placeholder='Password']")).sendKeys(password);

            // Capture captcha image
            WebElement captchaImage = driver.findElement(By.xpath("//img[@class='captcha-img']"));
            File srcFile = captchaImage.getScreenshotAs(OutputType.FILE);
            String captchaPath = System.getProperty("user.dir") + "/Screenshots/captcha.png";
            FileHandler.copy(srcFile, new File(captchaPath));

            // Re-encode image with standard DPI
            BufferedImage originalImage = ImageIO.read(new File(captchaPath));
            BufferedImage cleanImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = cleanImage.createGraphics();
            g.drawImage(originalImage, 0, 0, null);
            g.dispose();

            // Save the processed image
            File processedImageFile = new File(System.getProperty("user.dir") + "/Screenshots/processed_captcha.png");
            ImageIO.write(cleanImage, "png", processedImageFile);

            // Perform OCR using Tesseract
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata"); // Optional if you're using default
            String rawCaptchaText = tesseract.doOCR(processedImageFile);
            System.out.println("Raw OCR output: " + rawCaptchaText);

            // Remove unwanted characters but preserve important symbols
            String captchaText = rawCaptchaText.replaceAll("[^a-zA-Z0-9@#&$!%*+=\\-_]", "").trim();
            logger.info("Processed Captcha: " + captchaText);

            // Enter captcha and submit
            driver.findElement(By.xpath("//input[@formcontrolname='captcha']")).sendKeys(captchaText);
            driver.findElement(By.xpath("//button[text()='SIGN IN']")).click();

        } catch (IOException | TesseractException e) {
            logger.error("Error processing captcha: " + e.getMessage());
        } catch (NoSuchElementException e) {
            logger.error("Element not found: " + e.getMessage());
        }
    }

    @AfterClass
    public void tearDown() {
        // Commented out to keep the browser open after test
        // driver.quit();
    }
}
