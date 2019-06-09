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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public final void testMetricsFilterUniqueId() throws ServletException, IOException {

        final Filter metricsFilter = new MetricsFilter();
        metricsFilter.init(mock(FilterConfig.class));

        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final ConcurrentLinkedQueue<String> responseIds = new ConcurrentLinkedQueue<>();

        doAnswer((invocation) -> {
            responseIds.add(invocation.getArgument(1, String.class));
            return null;
        }).when(servletResponse).addHeader(eq(MetricsFilter.UNIQUE_RESPONSE_ID), any(String.class));

        final FilterChain filterChain = mock(FilterChain.class);

        LongStream.rangeClosed(1, TOTAL_REQUESTS_TO_SEND + 1).parallel().forEach((i) -> {
            try {
                metricsFilter.doFilter(servletRequest, servletResponse, filterChain);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });

        // Sort all response ids and compare each one with the next to ensure that none are the same.
        final List<String> sortedResponseIds = new ArrayList<>(responseIds);
        sortedResponseIds.sort(null);

        for (int i = 0; i < sortedResponseIds.size() - 1; i++) {
            Assert.assertNotEquals(sortedResponseIds.get(i), sortedResponseIds.get(i + 1));
        }

        final HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);
        Assert.assertEquals(sortedResponseIds.size(),
                ((Map) request.getAttribute(MetricsFilter.RESPONSE_METRICS)).size());

        metricsFilter.destroy();
    }

    @Test
    public final void testMetricsFilterGetResponseSize() throws ServletException, IOException {

        final Filter metricsFilter = new MetricsFilter();
        metricsFilter.init(mock(FilterConfig.class));

        final HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);
        Assert.assertTrue(0L == (Long) request.getAttribute(MetricsFilter.MINIMUM_RESPONSE_SIZE));
        Assert.assertTrue(0L == (Long) request.getAttribute(MetricsFilter.MAXIMUM_RESPONSE_SIZE));
        Assert.assertTrue(0.0 == (Double) request.getAttribute(MetricsFilter.AVERAGE_RESPONSE_SIZE));
        Assert.assertEquals(0, ((Map) request.getAttribute(MetricsFilter.RESPONSE_METRICS)).size());
        testMetricsFilterResponseSize(metricsFilter);

        metricsFilter.destroy();
    }

    @Test
    public final void testMetricsFilterGetResponseTime() throws ServletException, IOException {

        final Filter metricsFilter = new MetricsFilter();
        metricsFilter.init(mock(FilterConfig.class));

        HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);
        Assert.assertTrue(0L == (Long) request.getAttribute(MetricsFilter.MINIMUM_RESPONSE_TIME));
        Assert.assertTrue(0L == (Long) request.getAttribute(MetricsFilter.MAXIMUM_RESPONSE_TIME));
        Assert.assertTrue(0.0 == (Double) request.getAttribute(MetricsFilter.AVERAGE_RESPONSE_TIME));
        Assert.assertEquals(0, ((Map) request.getAttribute(MetricsFilter.RESPONSE_METRICS)).size());
        testMetricsFilterResponseSize(metricsFilter);

        request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);

        final Long minimumResponseTime = (Long) request.getAttribute(MetricsFilter.MINIMUM_RESPONSE_TIME);
        Assert.assertTrue(0 < minimumResponseTime);

        final Long maximumResponseTime = (Long) request.getAttribute(MetricsFilter.MAXIMUM_RESPONSE_TIME);
        Assert.assertTrue((minimumResponseTime < maximumResponseTime) ||
                (minimumResponseTime == maximumResponseTime));

        final Double averageResponseTime = (Double) request.getAttribute(MetricsFilter.AVERAGE_RESPONSE_TIME);
        Assert.assertTrue((minimumResponseTime < averageResponseTime && averageResponseTime < maximumResponseTime) ||
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

        Assert.assertTrue(stringWriter.toString().length() ==
                LongStream.rangeClosed(minimumResponseSize, maximumResponseSize).sum());

        final HttpServletRequest request = newMockHttpServletRequestWithMutableAttributes();
        requestMetricsPage(request, metricsFilter);

        Assert.assertEquals(minimumResponseSize,
                request.getAttribute(MetricsFilter.MINIMUM_RESPONSE_SIZE));
        Assert.assertEquals(maximumResponseSize,
                request.getAttribute(MetricsFilter.MAXIMUM_RESPONSE_SIZE));
        Assert.assertEquals(
                LongStream.rangeClosed(minimumResponseSize, maximumResponseSize).asDoubleStream().average().getAsDouble(),
                request.getAttribute(MetricsFilter.AVERAGE_RESPONSE_SIZE));
    }
}
