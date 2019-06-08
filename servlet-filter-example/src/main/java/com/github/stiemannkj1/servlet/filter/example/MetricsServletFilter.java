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
import java.util.LongSummaryStatistics;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public final class MetricsServletFilter implements Filter {

    static final String UNIQUE_RESPONSE_ID =
            MetricsServletFilter.class.getName() + ".UNIQUE_RESPONSE_ID";
    static final String MINIMUM_RESPONSE_SIZE = "minimumResponseSize";
    static final String MAXIMUM_RESPONSE_SIZE = "maximumResponseSize";
    static final String AVERAGE_RESPONSE_SIZE = "averageResponseSize";
    static final String MINIMUM_RESPONSE_TIME = "minimumResponseTime";
    static final String MAXIMUM_RESPONSE_TIME = "maximumResponseTime";
    static final String AVERAGE_RESPONSE_TIME = "averageResponseTime";
    static final String METRICS_JSP_PAGE =
            "/" + MetricsServletFilter.class.getName().replace(".", "_") + ".jsp";

    private final AtomicLong uniqueResponseId = new AtomicLong();
    private final ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> responseSizes = new ConcurrentLinkedQueue<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        uniqueResponseId.set(0);
        responseSizes.clear();
        responseTimes.clear();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final String servletPath = httpServletRequest.getServletPath();

        if (servletPath != null && servletPath.equals(METRICS_JSP_PAGE)) {

            setMetricsAttributes(httpServletRequest, responseSizes, MAXIMUM_RESPONSE_SIZE, MINIMUM_RESPONSE_SIZE,
                    AVERAGE_RESPONSE_SIZE);
            setMetricsAttributes(httpServletRequest, responseTimes, MAXIMUM_RESPONSE_TIME, MINIMUM_RESPONSE_TIME,
                    AVERAGE_RESPONSE_TIME);
            chain.doFilter(httpServletRequest, response);
        } else {

            final ResponseSizeHttpServletResponseWrapper httpServletResponse =
                    new ResponseSizeHttpServletResponseWrapper((HttpServletResponse) response);
            httpServletResponse.addHeader(UNIQUE_RESPONSE_ID, Long.toString(uniqueResponseId.getAndIncrement()));
            long startTime = System.nanoTime();
            chain.doFilter(httpServletRequest, httpServletResponse);
            responseTimes.add(System.nanoTime() - startTime);
            responseSizes.add(httpServletResponse.getResponseSize());
        }
    }

    @Override
    public void destroy() {
        uniqueResponseId.set(0);
        responseSizes.clear();
        responseTimes.clear();
    }

    private void setMetricsAttributes(HttpServletRequest httpServletRequest, ConcurrentLinkedQueue<Long> metrics,
            String maxAttrName, String minAttrName, String averageAttrName) {
        final LongSummaryStatistics stats =
                new ArrayList<>(metrics).stream().collect(Collectors.summarizingLong(Long::longValue));
        httpServletRequest.setAttribute(maxAttrName, stats.getMax());
        httpServletRequest.setAttribute(minAttrName, stats.getMin());
        httpServletRequest.setAttribute(averageAttrName, stats.getAverage());
    }
}
