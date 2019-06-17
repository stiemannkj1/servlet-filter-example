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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.LongStream;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * @author Kyle Stiemann
 */
public final class TestMetricsFilter {

    private static final long TOTAL_REQUESTS_TO_SEND = 100;

    @Test
    public final void testMetricsFilterUniqueAtomicLongId() throws ServletException, IOException {
        testMetricsFilterUniqueId(false);
    }

    @Test
    public final void testMetricsFilterUniqueUUID() throws ServletException, IOException {
        testMetricsFilterUniqueId(true);
    }

    @Test
    public final void testMetricsFilterGetResponseSize() throws ServletException, IOException {

        final Filter metricsFilter = new MetricsFilter();
        metricsFilter.init(mock(FilterConfig.class));
        testMetricsWithNoRequests(metricsFilter, MetricsFilter.Metric.RESPONSE_SIZE);
        testMetricsFilterResponseSize(metricsFilter);

        metricsFilter.destroy();
    }

    @Test
    public final void testMetricsFilterGetResponseTime() throws ServletException, IOException {

        final Filter metricsFilter = new MetricsFilter();
        metricsFilter.init(mock(FilterConfig.class));
        testMetricsWithNoRequests(metricsFilter, MetricsFilter.Metric.RESPONSE_SIZE);
        testMetricsFilterResponseSize(metricsFilter);

        final HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);

        final long minimumResponseTime = (Long) request.getAttribute(MetricsFilter.Metric.RESPONSE_TIME.getMinId());
        Assert.assertTrue("Minimum response time is zero after requests were filtered by MetricsFilter.",
                (0 < minimumResponseTime));

        final long maximumResponseTime = (Long) request.getAttribute(MetricsFilter.Metric.RESPONSE_TIME.getMaxId());
        Assert.assertTrue("Minimum response time was not less than or equal to maximum.",
                (minimumResponseTime <= maximumResponseTime));

        final Double averageResponseTime =
                (Double) request.getAttribute(MetricsFilter.Metric.RESPONSE_TIME.getAverageId());
        Assert.assertTrue("Average response time is not between (or equal to) minimum and maximum.",
                (minimumResponseTime < averageResponseTime && averageResponseTime < maximumResponseTime) ||
                    (minimumResponseTime == maximumResponseTime));

        metricsFilter.destroy();
    }

    private HttpServletRequest newMockHttpServletRequestWithMutableAttributes() throws IOException {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Map<String, Object> requestAttrs = new HashMap<>();

        when(request.getAttribute(any(String.class))).thenAnswer((invocation) -> {
            return requestAttrs.get(invocation.getArgument(0, String.class));
        });

        doAnswer((invocation) -> {
            requestAttrs.put(invocation.getArgument(0, String.class), invocation.getArgument(1, Object.class));
            return null;
        }).when(request).setAttribute(any(String.class), any(Object.class));

        return request;
    }

    private void requestMetricsPage(HttpServletRequest mockHttpServletRequest, Filter metricsServletFilter)
            throws ServletException, IOException {

        when(mockHttpServletRequest.getServletPath()).thenReturn(MetricsFilter.METRICS_JSP_PAGE);

        final FilterChain filterChain = mock(FilterChain.class);
        metricsServletFilter.doFilter(mockHttpServletRequest, mock(HttpServletResponse.class), filterChain);
    }

    private void testMetricsFilterResponseSize(Filter metricsFilter) throws ServletException, IOException {

        final long minimumResponseSize = 1L;
        final long maximumResponseSize = TOTAL_REQUESTS_TO_SEND;
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        final ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);

        doAnswer((invocation) -> {
            stringWriter.write(invocation.getArgument(0, Integer.class));
            return null;
        }).when(servletOutputStream).write(any(Integer.class));

        when(servletResponse.getOutputStream()).thenReturn(servletOutputStream);

        // Send several requests in paralell to the metrics filter. The number of the request in the sequence also
        // indicates the size of the response in bytes.
        LongStream.rangeClosed(minimumResponseSize, maximumResponseSize).parallel().forEach((i) -> {
            try {
                final FilterChain filterChain = mock(FilterChain.class);

                doAnswer((invocation) -> {
                    final ServletResponse testServletResponse = invocation.getArgument(1, ServletResponse.class);
                    ServletOutputStream responseServletOutputStream = null;
                    PrintWriter responsePrintWriter = null;

                    // Test half with getWriter() and half with getOutputStream() to fully test the API.
                    final boolean testWriter = (i % 2 == 0);

                    for (long j = 0; j < i; j++) {

                        if (testWriter) {
                            testServletResponse.getWriter().write("b");
                        } else {
                            testServletResponse.getOutputStream().print("b");
                        }
                    }

                    testServletResponse.flushBuffer();
                    return null;
                }).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

                metricsFilter.doFilter(servletRequest, servletResponse, filterChain);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });

        Assert.assertEquals("Not all bytes were written to the wrapped response.", stringWriter.toString().length(),
                    LongStream.rangeClosed(minimumResponseSize, maximumResponseSize).sum());

        final HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);

        Assert.assertEquals("Calculated minimum response size is incorrect.", minimumResponseSize,
                request.getAttribute(MetricsFilter.Metric.RESPONSE_SIZE.getMinId()));
        Assert.assertEquals("Calculated maximum response size is incorrect.", maximumResponseSize,
                request.getAttribute(MetricsFilter.Metric.RESPONSE_SIZE.getMaxId()));
        Assert.assertEquals(
                "Calculated average response size is not the average of bytes written to all responses.",
                LongStream.rangeClosed(minimumResponseSize, maximumResponseSize).asDoubleStream().average()
                        .getAsDouble(),
                (Double) request.getAttribute(MetricsFilter.Metric.RESPONSE_SIZE.getAverageId()), 0.1);
    }

    private void testMetricsFilterUniqueId(boolean testUseUUIDUniqueResponseId) throws ServletException,
            IOException {

        final Filter metricsFilter = new MetricsFilter();
        final FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getInitParameter(MetricsFilter.USE_UUID_UNIQUE_RESPONSE_ID_KEY))
                .thenReturn(Boolean.toString(testUseUUIDUniqueResponseId));
        metricsFilter.init(filterConfig);

        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final ConcurrentLinkedQueue<String> responseIds = new ConcurrentLinkedQueue<>();

        doAnswer((invocation) -> {
            responseIds.add(invocation.getArgument(1, String.class));
            return null;
        }).when(servletResponse).addHeader(eq(MetricsFilter.UNIQUE_RESPONSE_ID), any(String.class));

        final FilterChain filterChain = mock(FilterChain.class);

        LongStream.rangeClosed(1, TOTAL_REQUESTS_TO_SEND).parallel().forEach((i) -> {
            try {
                metricsFilter.doFilter(servletRequest, servletResponse, filterChain);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });

        final Set<String> responseIdsSet = new HashSet<String>();
        final Iterator<String> iterator = responseIds.iterator();

        while (iterator.hasNext()) {
            final String responseId = iterator.next();
            Assert.assertTrue("Duplicate response id found: " + responseId, responseIdsSet.add(responseId));
        }

        final HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);
        Assert.assertEquals(
                "The number of response ids found differs from the number of response metrics recorded by MetricsFilter.",
                responseIdsSet.size(), ((Map) request.getAttribute(MetricsFilter.RESPONSE_METRICS)).size());

        metricsFilter.destroy();
    }

    private void testMetricsWithNoRequests(Filter metricsFilter, MetricsFilter.Metric metric)
            throws ServletException, IOException {
        final HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);

        final String metricName = metric.name().toLowerCase(Locale.ENGLISH).replace("_", " ");
        Assert.assertEquals("Initial " + metricName + " metric min value did not equal zero.",
                Long.valueOf(0L), (Long) request.getAttribute(metric.getMinId()));
        Assert.assertEquals("Initial " + metricName + " metric max value did not equal zero.",
                Long.valueOf(0L), (Long) request.getAttribute(metric.getMaxId()));
        Assert.assertEquals("Initial " + metricName + " metric average value did not equal zero.",
                0.0, (Double) request.getAttribute(metric.getAverageId()), 0.0);
        Assert.assertEquals("Intial " + metricName + " metrics map contained entries.", 0,
                ((Map) request.getAttribute(MetricsFilter.RESPONSE_METRICS)).size());
    }
}
