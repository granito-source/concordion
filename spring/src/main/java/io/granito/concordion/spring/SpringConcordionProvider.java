package io.granito.concordion.spring;

import org.concordion.integration.TestFrameworkProvider;
import org.junit.platform.commons.support.AnnotationSupport;

public class SpringConcordionProvider implements TestFrameworkProvider {
    @Override
    public boolean isConcordionFixture(Class<?> clazz)
    {
        return AnnotationSupport
            .findAnnotation(clazz, SpringConcordionFixture.class)
            .isPresent();
    }
}
