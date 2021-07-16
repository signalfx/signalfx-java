package com.signalfx.metrics.flush;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class AggregateMetricSenderTest {

  @Test
  public void shouldFailOnNullDefaultSourceName() {
    try {
      new AggregateMetricSender(null, null, null, null, null);
      fail("NPE was expected");
    } catch (NullPointerException npe) {
      assertTrue(npe.getMessage().contains("defaultSourceName"));
    }
  }

  @Test
  public void shouldNotFailOnNonNullDefaultSourceName() {
    new AggregateMetricSender("source", null, null, null, null);
  }
}