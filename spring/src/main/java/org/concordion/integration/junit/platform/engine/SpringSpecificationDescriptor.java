package org.concordion.integration.junit.platform.engine;

import org.concordion.api.SpecificationLocator;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.UniqueId;
import org.springframework.test.context.TestContextManager;

public class SpringSpecificationDescriptor extends SpecificationDescriptor {
    public SpringSpecificationDescriptor(UniqueId id, Class<?> fixture,
        SpecificationLocator locator)
    {
        super(id, fixture, locator);
    }

    @Override
    protected Object createFixtureObject()
    {
        var clazz = this.getFixtureClass();
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
