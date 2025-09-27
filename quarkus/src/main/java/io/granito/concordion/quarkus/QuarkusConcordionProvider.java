package io.granito.concordion.quarkus;

import io.granito.concordion.api.QuarkusConcordionFixture;
import org.concordion.integration.TestFrameworkProvider;
import org.junit.platform.commons.support.AnnotationSupport;

public class QuarkusConcordionProvider implements TestFrameworkProvider {
    @Override
    public boolean isConcordionFixture(Class<?> clazz)
    {
        return AnnotationSupport
            .findAnnotation(clazz, QuarkusConcordionFixture.class)
            .isPresent();
    }
}
