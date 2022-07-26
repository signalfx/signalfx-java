/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micrometer.signalfx;

import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;

final class DeltaHistogramSnapshot {
    // It may get called from different threads, so use volatile to ensure updates are visible.
    // Not null only if producing delta.
    private volatile HistogramSnapshot lastSnapshot;

    DeltaHistogramSnapshot(boolean isDelta) {
        if (isDelta) {
            lastSnapshot = HistogramSnapshot.empty(0, 0, 0);
        } else {
            lastSnapshot = null;
        }
    }

    // TODO: Determine if we need to synchronize, in case multiple calls in parallel.
    HistogramSnapshot calculateSnapshot(HistogramSnapshot currentSnapshot) {
        if (lastSnapshot == null) {
            return currentSnapshot;
        }
        HistogramSnapshot deltaSnapshot = new HistogramSnapshot(
                currentSnapshot.count() - lastSnapshot.count(),
                currentSnapshot.total() - lastSnapshot.total(),
                currentSnapshot.max(),  // Max cannot be calculated as delta, keep the current.
                null,  // No percentile values
                deltaHistogramCounts(currentSnapshot),
                currentSnapshot::outputSummary);
        lastSnapshot = currentSnapshot;
        return deltaSnapshot;
    }

    private CountAtBucket[] deltaHistogramCounts(HistogramSnapshot currentSnapshot) {
        CountAtBucket[] currentHistogramCounts = currentSnapshot.histogramCounts();
        CountAtBucket[] lastHistogramCounts = lastSnapshot.histogramCounts();
        if (lastHistogramCounts == null || lastHistogramCounts.length == 0) {
            return currentHistogramCounts;
        }

        CountAtBucket[] retHistogramCounts = new CountAtBucket[currentHistogramCounts.length];
        for (int i = 0; i < currentHistogramCounts.length; i++) {
            retHistogramCounts[i] = new CountAtBucket(
                    currentHistogramCounts[i].bucket(),
                    currentHistogramCounts[i].count() - lastHistogramCounts[i].count());
        }
        return retHistogramCounts;
    }
}
