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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * The metrics for a specific response. The metrics can be set at most once. This class is package private and
 * implements {@link List} to allow metrics to be accessed from EL without exposing additional API. This class is
 * thread-safe.
 *
 * @author Kyle Stiemann
 */
final class SpecificResponseMetrics implements List<Long> {

    /**
     * The type of the metric to record or display.
     */
    enum Metric {
        RESPONSE_TIME(0, "minimumResponseTime", "maximumResponseTime", "averageResponseTime"),
        RESPONSE_SIZE(1, "minimumResponseSize", "maximumResponseSize", "averageResponseSize");

        private final int index;
        private final String minId;
        private final String maxId;
        private final String averageId;

        private Metric(int index, String minId, String maxId, String averageId) {
            this.index = index;
            this.minId = minId;
            this.maxId = maxId;
            this.averageId = averageId;
        }

        /**
         * @return the index of the metric type when stored in a list in {@link SpecificResponseMetrics}.
         *
         * @see SpecificResponseMetrics
         * @see MetricsFilter#setMetricsAttributes(
         * com.github.stiemannkj1.servlet.filter.example.SpecificResponseMetrics.Metric,
         * javax.servlet.http.HttpServletRequest, java.util.Collection)
         */
        public int getIndex() {
            return index;
        }

        /**
         * @return the request attribute name and client id for the minimum value for the metric.
         */
        public String getMinId() {
            return minId;
        }

        /**
         * @return the request attribute name and client id for the maximum value for the metric.
         */
        public String getMaxId() {
            return maxId;
        }

        /**
         * @return the request attribute name and client id for the average value for the metric.
         */
        public String getAverageId() {
            return averageId;
        }
    }

    private List<Long> wrappedList = null;

    synchronized List<Long> getMetrics() {
        return wrappedList;
    }

    synchronized void setMetrics(long responseTime, long responseSize) {

        if (this.wrappedList != null) {
            throw new UnsupportedOperationException("Metrics may only be set once.");
        }

        this.wrappedList = Collections.unmodifiableList(Arrays.asList(responseTime, responseSize));
    }

    @Override
    public int size() {
        return getMetrics().size();
    }

    @Override
    public boolean isEmpty() {
        return getMetrics().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return getMetrics().isEmpty();
    }

    @Override
    public Iterator<Long> iterator() {
        return getMetrics().iterator();
    }

    @Override
    public Object[] toArray() {
        return getMetrics().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return getMetrics().toArray(a);
    }

    @Override
    public boolean add(Long e) {
        return getMetrics().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return getMetrics().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getMetrics().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Long> c) {
        return getMetrics().addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends Long> c) {
        return getMetrics().addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return getMetrics().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return getMetrics().retainAll(c);
    }

    @Override
    public void clear() {
        getMetrics().clear();
    }

    @Override
    public Long get(int index) {
        return getMetrics().get(index);
    }

    @Override
    public Long set(int index, Long element) {
        return getMetrics().set(index, element);
    }

    @Override
    public void add(int index, Long element) {
        getMetrics().add(index, element);
    }

    @Override
    public Long remove(int index) {
        return getMetrics().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return getMetrics().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return getMetrics().lastIndexOf(o);
    }

    @Override
    public ListIterator<Long> listIterator() {
        return getMetrics().listIterator();
    }

    @Override
    public ListIterator<Long> listIterator(int index) {
        return getMetrics().listIterator(index);
    }

    @Override
    public List<Long> subList(int fromIndex, int toIndex) {
        return getMetrics().subList(fromIndex, toIndex);
    }
}
