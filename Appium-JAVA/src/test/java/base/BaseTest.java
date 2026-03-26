package base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import utils.DriverManager;

public class BaseTest {

    @BeforeEach
    public void setUp() throws Exception {
        DriverManager.initDriver();
    }

    @AfterEach
    public void tearDown() {
        if (DriverManager.driver != null) {
            DriverManager.driver.quit();
        }
    }
}
