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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An example {@link Servlet} {@link Filter} that collects metrics on request/response sizes and times and provides a
 * unique id to differentiate responses. This Servlet Filter implementation exposes only the Filter API as public and is
 * marked as {@code final} to avoid misuse (although some package-private API is exposed for the purposes of testing).
 *
 * @author Kyle Stiemann
 */
public final class MetricsFilter implements Filter {

    static final String UNIQUE_RESPONSE_ID = MetricsFilter.class.getName() + ".UNIQUE_RESPONSE_ID";
    static final String RESPONSE_METRICS = "responseMetrics";
    static final String METRICS_JSP_PAGE = "/com_github_stiemannkj1_servlet_filter_example_Metrics.jsp";

    /**
     * The type of the metric to record or display.
     */
    enum Metric {
        RESPONSE_SIZE(0, "minimumResponseSize", "maximumResponseSize", "averageResponseSize"),
        RESPONSE_TIME(1, "minimumResponseTime", "maximumResponseTime", "averageResponseTime");

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
         * @return the index of the metric type when stored in a list in {@link #responseMetrics}.
         *
         * @see #responseMetrics
         * @see #setMetricsAttributes(com.github.stiemannkj1.servlet.filter.example.MetricsFilter.Metric,
         * javax.servlet.http.HttpServletRequest, java.util.Collection)
         */
        private int getIndex() {
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

    private final AtomicLong uniqueResponseId = new AtomicLong();

    /**
     * Although we could use a more descriptive custom inner class in place of {@link List}&gt;Long&lt; (which contains
     * responseSize and responseTime), the class would need to be public to be accessed from EL in
     * com_github_stiemannkj1_servlet_filter_example_Metrics.jsp. To avoid introducing unnecessary public API, use an
     * unmodifiable List&gt;Long&lt; for now.
     *
     * @see Metric#getIndex()
     */
    private final ConcurrentMap<Long, List<Long>> responseMetrics = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        uniqueResponseId.set(0);
        responseMetrics.clear();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final String servletPath = httpServletRequest.getServletPath();

        if (METRICS_JSP_PAGE.equals(servletPath)) {
            final Map<Long, List<Long>> metrics = Collections.unmodifiableMap(new HashMap<>(responseMetrics));
            final Collection<List<Long>> metricsCollection = metrics.values();
            setMetricsAttributes(Metric.RESPONSE_SIZE, httpServletRequest, metricsCollection);
            setMetricsAttributes(Metric.RESPONSE_TIME, httpServletRequest, metricsCollection);
            httpServletRequest.setAttribute(RESPONSE_METRICS, metrics);
            chain.doFilter(httpServletRequest, response);
        } else {
            final ResponseSizeHttpServletResponseWrapper httpServletResponse =
                    new ResponseSizeHttpServletResponseWrapper((HttpServletResponse) response);
            final long currentUniqueResponseId = uniqueResponseId.getAndIncrement();
            httpServletResponse.addHeader(UNIQUE_RESPONSE_ID, Long.toString(currentUniqueResponseId));
            final long startTime = System.nanoTime();
            chain.doFilter(httpServletRequest, httpServletResponse);
            responseMetrics.put(currentUniqueResponseId,
                    unmodifiableList(httpServletResponse.getResponseSize(), (System.nanoTime() - startTime)));
        }
    }

    @Override
    public void destroy() {
        uniqueResponseId.set(0);
        responseMetrics.clear();
    }

    private void setMetricsAttributes(Metric metric, HttpServletRequest httpServletRequest,
            Collection<List<Long>> metrics) {

        long min = 0;
        long max = 0;
        double average = 0.0;

        if (!metrics.isEmpty()) {
            final LongSummaryStatistics stats =
                    metrics.stream().collect(Collectors.summarizingLong((specificResponseMetrics) -> {
                        return specificResponseMetrics.get(metric.getIndex());
                    }));

            min = stats.getMin();
            max = stats.getMax();
            average = stats.getAverage();
        }

        httpServletRequest.setAttribute(metric.getMinId(), min);
        httpServletRequest.setAttribute(metric.getMaxId(), max);
        httpServletRequest.setAttribute(metric.getAverageId(), average);
    }

    private static <T> List<T> unmodifiableList(T... ts) {
        return Collections.unmodifiableList(Arrays.asList(ts));
    }
}
