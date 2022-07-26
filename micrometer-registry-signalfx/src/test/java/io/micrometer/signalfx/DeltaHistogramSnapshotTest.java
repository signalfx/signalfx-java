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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeltaHistogramSnapshotTest {

    @Test
    void empty() {
        DeltaHistogramSnapshot deltaHistogramSnapshot = new DeltaHistogramSnapshot();
        HistogramSnapshot empty = HistogramSnapshot.empty(0, 0, 0);
        assertEqualSnapshot(deltaHistogramSnapshot.calculateSnapshot(empty), empty);
    }

    @Test
    void nonEmpty() {
        DeltaHistogramSnapshot deltaHistogramSnapshot = new DeltaHistogramSnapshot();
        HistogramSnapshot first = new HistogramSnapshot(
                1, 2, 3, null,
                new CountAtBucket[]{
                        new CountAtBucket(1.0, 0),
                        new CountAtBucket(5.0, 1),
                        new CountAtBucket(Double.MAX_VALUE, 1)},
                (printStream, aDouble) -> {});
        assertEqualSnapshot(deltaHistogramSnapshot.calculateSnapshot(first), first);
        HistogramSnapshot second = new HistogramSnapshot(
                3, 10, 5.5, null,
                new CountAtBucket[]{
                        new CountAtBucket(1.0, 0),
                        new CountAtBucket(5.0, 2),
                        new CountAtBucket(Double.MAX_VALUE, 3)},
                (printStream, aDouble) -> {});
        assertEqualSnapshot(deltaHistogramSnapshot.calculateSnapshot(second), new HistogramSnapshot(
                2, 8, 5.5, null,
                new CountAtBucket[]{
                        new CountAtBucket(1.0, 0),
                        new CountAtBucket(5.0, 1),
                        new CountAtBucket(Double.MAX_VALUE, 2)},
                (printStream, aDouble) -> {}));
    }

    // Cannot directly use assertThat(got).isEqualTo(want) because HistogramSnapshot does not implement equals.
    private static void assertEqualSnapshot(HistogramSnapshot got, HistogramSnapshot want) {
        assertThat(got.count()).isEqualTo(want.count());
        assertThat(got.total()).isEqualTo(want.total());
        assertThat(got.max()).isEqualTo(want.max());
        assertThat(got.percentileValues()).isEqualTo(want.percentileValues());
        assertThat(got.histogramCounts()).isEqualTo(want.histogramCounts());
    }
}
