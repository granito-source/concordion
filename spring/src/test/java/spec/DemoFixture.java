package spec;

import io.granito.concordion.spring.SpringConcordionFixture;
import org.concordion.api.ConcordionResources;
import org.springframework.test.context.ContextConfiguration;

@SpringConcordionFixture
@ConcordionResources(value="/concordion.css", includeDefaultStyling = false)
@ContextConfiguration(classes = DemoFixture.class)
public class DemoFixture {
    public String greetingFor(String firstName)
    {
        return String.format("Hello %s!", firstName);
    }
}
