package io.granito.concordion.spring;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import org.concordion.integration.junit.platform.engine.BaseConcordionTestEngine;
import org.concordion.internal.runner.SpringConcordionRunner;
import org.junit.platform.commons.support.ReflectionSupport;
import org.springframework.test.context.TestContextManager;

public class SpringConcordionTestEngine extends BaseConcordionTestEngine {
    public static final String ENGINE_ID = "concordion-spring";
    public SpringConcordionTestEngine()
    {
        System.setProperty("concordion.runner.concordion",
            SpringConcordionRunner.class.getName());
    }

    @Override
    public String getId()
    {
        return ENGINE_ID;
    }

    @Override
    protected boolean annotatedAsFixture(Class<?> clazz)
    {
        return findAnnotation(clazz, ConcordionFixture.class)
            .isPresent();
    }

    @Override
    protected Object createFixtureObject(Class<?> clazz)
    {
        var testManager = new TestContextManager(clazz);
        var object = ReflectionSupport.newInstance(clazz);

        try {
            testManager.prepareTestInstance(object);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return object;
    }
}
