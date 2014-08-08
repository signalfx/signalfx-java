package com.signalfuse.metrics.metric.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.signalfuse.metrics.datumhandler.DatumHandler;

/**
 * @author jack
 */
abstract class DatumHandlerMetric extends BaseMetric {
    private final DatumHandler datumHandler;

    protected DatumHandlerMetric(String source, String metric, DatumHandler datumHandler) {
        super(source, metric);
        this.datumHandler = datumHandler;
    }

    protected DatumHandler getDatumHandler() {
        return datumHandler;
    }

    abstract static class BaseFactory<T extends DatumHandlerMetric> {
        private final DatumHandler datumHandler;
        private final Map<Pair<String, String>, T> counterCache;

        protected BaseFactory(DatumHandler datumHandler) {
            this.datumHandler = datumHandler;
            // Does not need to be sync safe b/c only accessed inside synchronized method
            counterCache = new HashMap<Pair<String, String>, T>();
        }

        protected abstract T createItem(String sourceName, String metricName);

        public synchronized T getMetric(String sourceName, String metricName) {
            Pair<String, String> mapkey = Pair.of(sourceName, metricName);
            T existingValue = counterCache.get(mapkey);
            if (existingValue != null) {
                return existingValue;
            }

            // Don't create the actual item if BaseMetric is in the map
            existingValue = createItem(sourceName, metricName);
            counterCache.put(mapkey, existingValue);
            return existingValue;
        }

        protected DatumHandler getDatumHandler() {
            return datumHandler;
        }
    }
}
