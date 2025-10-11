package spec.quarkus;

import io.granito.concordion.quarkus.QuarkusConcordionFixture;
import org.concordion.api.ConcordionResources;

@QuarkusConcordionFixture
@ConcordionResources(value="/concordion.css", includeDefaultStyling = false)
public class DemoFixture {
    public String greetingFor(String firstName)
    {
        return String.format("Hello %s!", firstName);
    }
}
