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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
 * @author Kyle Stiemann
 */
public final class MetricsFilter implements Filter {

    static final String UNIQUE_RESPONSE_ID =
            MetricsFilter.class.getName() + ".UNIQUE_RESPONSE_ID";
    static final String MINIMUM_RESPONSE_SIZE = "minimumResponseSize";
    static final String MAXIMUM_RESPONSE_SIZE = "maximumResponseSize";
    static final String AVERAGE_RESPONSE_SIZE = "averageResponseSize";
    static final String MINIMUM_RESPONSE_TIME = "minimumResponseTime";
    static final String MAXIMUM_RESPONSE_TIME = "maximumResponseTime";
    static final String AVERAGE_RESPONSE_TIME = "averageResponseTime";
    static final String RESPONSE_METRICS = "responseMetrics";
    static final String METRICS_JSP_PAGE = "/com_github_stiemannkj1_servlet_filter_example_Metrics.jsp";

    public static final class SpecificResponseMetrics {

        private final long responseSize;
        private final long responseTime;

        public SpecificResponseMetrics(long responseSize, long responseTime) {
            this.responseSize = responseSize;
            this.responseTime = responseTime;
        }

        public long getResponseSize() {
            return responseSize;
        }

        public long getResponseTime() {
            return responseTime;
        }
    }

    private enum Metric {
        RESPONSE_SIZE,
        RESPONSE_TIME
    }

    private final AtomicLong uniqueResponseId = new AtomicLong();
    private final ConcurrentMap<Long, SpecificResponseMetrics> responseMetrics = new ConcurrentHashMap<>();

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

        if (servletPath != null && servletPath.equals(METRICS_JSP_PAGE)) {
            Map<Long, SpecificResponseMetrics> metrics = Collections.unmodifiableMap(new HashMap<>(responseMetrics));
            Collection<SpecificResponseMetrics> metricsCollection = metrics.values();
            setMetricsAttributes(Metric.RESPONSE_SIZE, httpServletRequest, metricsCollection);
            setMetricsAttributes(Metric.RESPONSE_TIME, httpServletRequest, metricsCollection);
            httpServletRequest.setAttribute(RESPONSE_METRICS, metrics);
            chain.doFilter(httpServletRequest, response);
        } else {
            final ResponseSizeHttpServletResponseWrapper httpServletResponse =
                    new ResponseSizeHttpServletResponseWrapper((HttpServletResponse) response);
            final long uniqueResponseId = this.uniqueResponseId.getAndIncrement();
            httpServletResponse.addHeader(UNIQUE_RESPONSE_ID, Long.toString(uniqueResponseId));
            long startTime = System.nanoTime();
            chain.doFilter(httpServletRequest, httpServletResponse);
            responseMetrics.put(uniqueResponseId, new SpecificResponseMetrics(httpServletResponse.getResponseSize(),
                    System.nanoTime() - startTime));
        }
    }

    @Override
    public void destroy() {
        uniqueResponseId.set(0);
        responseMetrics.clear();
    }

    private void setMetricsAttributes(Metric metric, HttpServletRequest httpServletRequest,
            Collection<SpecificResponseMetrics> metrics) {

        String minAttrName;
        String maxAttrName;
        String averageAttrName;

        if (metric.equals(Metric.RESPONSE_SIZE)) {
            minAttrName = MINIMUM_RESPONSE_SIZE;
            maxAttrName = MAXIMUM_RESPONSE_SIZE;
            averageAttrName = AVERAGE_RESPONSE_SIZE;
        } else {
            minAttrName = MINIMUM_RESPONSE_TIME;
            maxAttrName = MAXIMUM_RESPONSE_TIME;
            averageAttrName = AVERAGE_RESPONSE_TIME;
        }

        long min = 0;
        long max = 0;
        double average = 0.0;

        if (!metrics.isEmpty()) {
            final LongSummaryStatistics stats =
                    metrics.stream().collect(Collectors.summarizingLong((specificResponseMetrics) -> {
                        if (metric.equals(Metric.RESPONSE_SIZE)) {
                            return specificResponseMetrics.responseSize;
                        } else {
                            return specificResponseMetrics.responseTime;
                        }
                    }));

            min = stats.getMin();
            max = stats.getMax();
            average = stats.getAverage();
        }

        httpServletRequest.setAttribute(minAttrName, min);
        httpServletRequest.setAttribute(maxAttrName, max);
        httpServletRequest.setAttribute(averageAttrName, average);
    }
}
