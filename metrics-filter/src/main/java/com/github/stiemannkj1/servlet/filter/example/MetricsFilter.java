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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * An example {@link Servlet} {@link Filter} that collects metrics on request/response sizes and times and provides a
 * unique id to differentiate responses. By default, unique response ids are generated using {@link AtomicLong} to
 * ensure uniqueness per MetricsFilter instance. However, {@link UUID} may be used to generate ids that are universally
 * unique across MetricsFilter instances (with an extremely small possibility of collisions) by setting the
 * {@code "com.github.stiemannkj1.servlet.filter.example.MetricsFilter.USE_UUID_UNIQUE_RESPONSE_ID"} init-param to
 * true.</p>
 *
 * <p>
 * This Servlet Filter implementation exposes only the Filter API as public and is marked as {@code final} to avoid
 * misuse (although some package-private API is exposed for the purposes of testing).</p>
 *
 * @author Kyle Stiemann
 */
public final class MetricsFilter implements Filter {

    static final String USE_UUID_UNIQUE_RESPONSE_ID_KEY =
            MetricsFilter.class.getName() + ".USE_UUID_UNIQUE_RESPONSE_ID";
    static final String UNIQUE_RESPONSE_ID = MetricsFilter.class.getName() + ".UNIQUE_RESPONSE_ID";
    static final String RESPONSE_METRICS = "responseMetrics";
    static final String METRICS_JSP_PAGE = "/com_github_stiemannkj1_servlet_filter_example_Metrics.jsp";

    private final ConcurrentMap<String, SpecificResponseMetrics> responseMetrics = new ConcurrentHashMap<>();

    private Supplier<String> uniqueResponseIdFactory;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        if ("true".equalsIgnoreCase(filterConfig.getInitParameter(USE_UUID_UNIQUE_RESPONSE_ID_KEY))) {
            uniqueResponseIdFactory = () -> {
                return UUID.randomUUID().toString();
            };
        } else {
            uniqueResponseIdFactory = new Supplier<String>() {
                private final AtomicLong uniqueResponseId = new AtomicLong();

                @Override
                public String get() {
                    return Long.toString(uniqueResponseId.incrementAndGet());
                }
            };
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final String servletPath = httpServletRequest.getServletPath();

        if (METRICS_JSP_PAGE.equals(servletPath)) {
            final Map<String, SpecificResponseMetrics> metrics = new HashMap<>(responseMetrics);
            final Collection<SpecificResponseMetrics> metricsCollection = metrics.values();

            metricsCollection.removeIf((specificResponseMetrics) -> {
                return specificResponseMetrics.getMetrics() == null;
            });

            setMetricsAttributes(SpecificResponseMetrics.Metric.RESPONSE_SIZE, httpServletRequest, metricsCollection);
            setMetricsAttributes(SpecificResponseMetrics.Metric.RESPONSE_TIME, httpServletRequest, metricsCollection);
            httpServletRequest.setAttribute(RESPONSE_METRICS, metrics);
            chain.doFilter(httpServletRequest, response);
        } else {
            final ResponseSizeHttpServletResponseWrapper httpServletResponse =
                    new ResponseSizeHttpServletResponseWrapper((HttpServletResponse) response);
            final SpecificResponseMetrics specificResponseMetrics = new SpecificResponseMetrics();
            String currentUniqueResponseId = uniqueResponseIdFactory.get();

            while (responseMetrics.putIfAbsent(currentUniqueResponseId, specificResponseMetrics) != null) {
                currentUniqueResponseId = uniqueResponseIdFactory.get();
            }

            httpServletResponse.addHeader(UNIQUE_RESPONSE_ID, currentUniqueResponseId);

            final long startTime = System.nanoTime();
            chain.doFilter(httpServletRequest, httpServletResponse);
            specificResponseMetrics.setMetrics((System.nanoTime() - startTime),
                    httpServletResponse.getResponseSize());
        }
    }

    @Override
    public void destroy() {
        uniqueResponseIdFactory = null;
        responseMetrics.clear();
    }

    private void setMetricsAttributes(SpecificResponseMetrics.Metric metric, HttpServletRequest httpServletRequest,
            Collection<SpecificResponseMetrics> metrics) {

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
}
