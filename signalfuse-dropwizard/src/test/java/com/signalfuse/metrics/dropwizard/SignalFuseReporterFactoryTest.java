package com.signalfuse.metrics.dropwizard;

import java.lang.reflect.Field;
import org.junit.Test;
import com.google.common.annotations.VisibleForTesting;

import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SignalFuseReporterFactoryTest {

    @Test
    public void testDiscoverability() {
        assertTrue(new DiscoverableSubtypeResolver().getDiscoveredSubtypes()
                .contains(SignalFuseReporterFactory.class));
    }

    @Test
    public void testDoesThrowNull() {
        new SignalFuseReporterFactory().build(null);
    }

    @Test
    public void testCanCreate() throws NoSuchFieldException, IllegalAccessException {
        SignalFuseReporterFactory factory = new SignalFuseReporterFactory();
        Field field = factory.getClass().getDeclaredField("authToken");
        field.setAccessible(true);
        field.set(factory, "authToken");
        assertNotNull(factory.build(null));
    }
}
