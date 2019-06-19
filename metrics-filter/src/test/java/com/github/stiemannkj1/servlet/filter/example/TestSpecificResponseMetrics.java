/*
 * The MIT License
 *
 * Copyright 2019 Kyle Stiemann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.stiemannkj1.servlet.filter.example;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kyle Stiemann
 */
public final class TestSpecificResponseMetrics {

    @Test
    public final void testGetMetrics() {
        final SpecificResponseMetrics specificResponseMetrics = new SpecificResponseMetrics();
        final long responseTime = Long.MAX_VALUE;
        final long responseSize = Long.MIN_VALUE;
        specificResponseMetrics.setMetrics(responseTime, responseSize);
        Assert.assertEquals((Long) responseTime,
                specificResponseMetrics.get(SpecificResponseMetrics.Metric.RESPONSE_TIME.getIndex()));
        Assert.assertEquals((Long) responseSize,
                specificResponseMetrics.get(SpecificResponseMetrics.Metric.RESPONSE_SIZE.getIndex()));
    }

    @Test
    public final void testImmutableAsList() {
        final SpecificResponseMetrics specificResponseMetrics = new SpecificResponseMetrics();
        testMutatingListOperations(specificResponseMetrics);
        specificResponseMetrics.setMetrics(0, 0);
        testMutatingListOperations(specificResponseMetrics);
    }

    @Test
    public final void testMetricsMayBeSetOnce() {
        final SpecificResponseMetrics specificResponseMetrics = new SpecificResponseMetrics();
        Assert.assertNull(specificResponseMetrics.getMetrics());
        specificResponseMetrics.setMetrics(0, 0);
        Assert.assertNotNull(specificResponseMetrics.getMetrics());

        try {
            specificResponseMetrics.setMetrics(0, 0);
            Assert.fail("SpecificResponseMetrics failed to throw " +
                    UnsupportedOperationException.class.getSimpleName() +
                    " when setMetrics() was called multiple times on a single instance.");
        } catch (UnsupportedOperationException e) {
            // Test passed.
        }
    }

    @Test
    public final void testSetMetricsParametersLengthEqualsEnumMetricsValuesLength() throws NoSuchMethodException,
            SecurityException {
        final Method setMetricsMethod = Arrays.asList(SpecificResponseMetrics.class.getDeclaredMethods()).stream()
                .filter((method) -> {
                    return "setMetrics".equals(method.getName());
                }).findFirst().get();

        Assert.assertNotNull(setMetricsMethod);
        Assert.assertEquals(SpecificResponseMetrics.Metric.values().length, setMetricsMethod.getParameters().length);
    }

    private void testMutatingListOperation(Runnable performMutatingListOperation) {
        try {
            performMutatingListOperation.run();
            Assert.fail("SpecificResponseMetrics is not immutable when accessed as a List.");
        } catch (NullPointerException | UnsupportedOperationException e) {
            // Test passed.
        }
    }

    private void testMutatingListOperations(List<Long> specificResponseMetrics) {
        testMutatingListOperation(() -> {
            specificResponseMetrics.add(0L);
        });

        testMutatingListOperation(() -> {
            specificResponseMetrics.remove(0L);
        });

        testMutatingListOperation(() -> {
            specificResponseMetrics.clear();
        });
    }
}
