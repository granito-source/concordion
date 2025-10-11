package spec.spring;

import io.granito.concordion.spring.ConcordionFixture;
import org.concordion.api.ConcordionResources;
import org.springframework.test.context.ContextConfiguration;

@ConcordionFixture
@ConcordionResources(value="/concordion.css", includeDefaultStyling = false)
@ContextConfiguration(classes = DemoFixture.class)
public class DemoFixture {
    public String greetingFor(String firstName)
    {
        return String.format("Hello %s!", firstName);
    }
}
