package ro.unibuc.prodeng.e2e;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = CucumberSpringConfiguration.class)
public class CucumberSpringConfiguration {
    // Empty configuration - we don't need Spring context for E2E tests.
    // since we're calling an already running server via REST
}
