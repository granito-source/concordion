package org.concordion.integration.junit.platform.engine;

import jakarta.enterprise.inject.spi.CDI;
import org.concordion.api.SpecificationLocator;
import org.junit.platform.engine.UniqueId;

public class QuarkusSpecificationDescriptor extends SpecificationDescriptor {
    public QuarkusSpecificationDescriptor(UniqueId id, Class<?> fixture,
        SpecificationLocator locator)
    {
        super(id, fixture, locator);
    }

    @Override
    protected Object createFixtureObject()
    {
        return CDI.current().select(getFixtureClass()).get();
    }
}
