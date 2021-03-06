/*
 * Copyright 2016 Kamil Becmer <kamil.becmer at maisica.pl>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.maisica.time.interval;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import static java.util.Spliterator.*;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.maisica.time.span.TemporalSpan;

/**
 * An interface for intervals based on time continuum, that means the temporal can be directly placed on timeline.
 *
 * @param <T> the {@link Temporal} comparable type
 */
public interface TemporalInterval<T extends Temporal & Comparable<? super T>> extends Interval<T> {

    /**
     * Gets the duration of this interval.
     *
     * @return the non-negative duration
     */
    public default Duration toDuration() {
        return Duration.between(getStart(), getEnd());
    }

    /**
     * Converts this interval to span.
     *
     * @return the span with same start and duration
     */
    public TemporalSpan<T> toSpan();

    /**
     * Generates stream of temporals with specified step.
     * <p>
     * The result stream consist all temporals contained in this interval split by the specified step starting from the start temporal. Returns empty stream if
     * this interval is empty, otherwise contains the start temporal at least.
     * </p>
     *
     * @param step the positive step, not null
     * @return stream of temporals
     */
    @SuppressWarnings("unchecked")
    public default Stream<T> stream(final TemporalAmount step) {
        Objects.requireNonNull(step, "step");
        final TemporalUnit unsupportedUnit = step.getUnits().stream().filter(u -> !getStart().isSupported(u)).findFirst().orElse(null);
        if (unsupportedUnit != null) {
            throw new UnsupportedTemporalTypeException(String.format("Unsupported unit: %s", unsupportedUnit));
        }

        final Spliterator<T> spliterator = Stream.iterate(getStart(), step::addTo).map(t -> (T) t).spliterator();
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, DISTINCT | IMMUTABLE | NONNULL | ORDERED | SORTED) {

            private boolean valid = true;

            @Override
            public boolean tryAdvance(final Consumer<? super T> consumer) {
                if (valid) {
                    final boolean hadNext = spliterator.tryAdvance(t -> {
                        if (t.compareTo(getEnd()) < 0) {
                            consumer.accept(t);
                        } else {
                            valid = false;
                        }
                    });
                    return hadNext && valid;
                }
                return false;
            }

            @Override
            public Comparator<? super T> getComparator() {
                return null;
            }

        }, false);
    }

}
