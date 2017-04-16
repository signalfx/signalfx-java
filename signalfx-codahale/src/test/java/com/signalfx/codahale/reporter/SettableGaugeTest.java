package com.signalfx.codahale.reporter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.signalfx.codahale.metrics.SettableLongGauge;

public class SettableGaugeTest {

    @Test
    public void testHasChanged() {
        SettableGauge gauge = new SettableLongGauge();
        assertFalse(gauge.hasChanged());

        ((SettableLongGauge) gauge).setValue(42L);
        assertTrue(gauge.hasChanged());
    }

    @Test
    public void testMarks() {
        SettableGauge gauge = new SettableLongGauge();
        assertFalse(gauge.hasChanged());

        gauge.markReported();
        assertFalse(gauge.hasChanged());

        gauge.markSet();
        assertTrue(gauge.hasChanged());

        gauge.markReported();
        assertFalse(gauge.hasChanged());

        gauge.markSet();
        gauge.markReported();
        assertFalse(gauge.hasChanged());
    }
}
