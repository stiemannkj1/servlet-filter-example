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
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author kyle
 */
public final class TestMetricsServletFilter {

    private static final long TOTAL_REQUESTS_TO_SEND = 1000;

    @Test
    public final void testServletMetricFilterUniqueId() throws ServletException, IOException {

        final Filter servletMetricFilter = new MetricsServletFilter();
        servletMetricFilter.init(mock(FilterConfig.class));
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final ConcurrentLinkedQueue<String> responseIds = new ConcurrentLinkedQueue<>();

        doAnswer((invocation) -> {
            responseIds.add(invocation.getArgument(1, String.class));
            return null;
        }).when(servletResponse).addHeader(eq(MetricsServletFilter.UNIQUE_RESPONSE_ID), any(String.class));

        final FilterChain filterChain = mock(FilterChain.class);

        LongStream.rangeClosed(1, TOTAL_REQUESTS_TO_SEND + 1).parallel().forEach((i) -> {
            try {
                servletMetricFilter.doFilter(servletRequest, servletResponse, filterChain);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });

        final List<String> sortedResponseIds = new ArrayList<>(responseIds);
        sortedResponseIds.sort(null);

        for (int i = 0; i < sortedResponseIds.size() - 1; i++) {
            Assert.assertNotEquals(sortedResponseIds.get(i), sortedResponseIds.get(i + 1));
        }

        servletMetricFilter.destroy();
    }

    @Test
    public final void testServletMetricFilterResponseSize() throws ServletException, IOException {

        final Filter servletMetricFilter = new MetricsServletFilter();
        servletMetricFilter.init(mock(FilterConfig.class));

        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final long minimumResponseSize = 1L;
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        when(servletResponse.getWriter()).thenReturn(printWriter);

        final ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void write(int b) throws IOException {
                stringWriter.write(b);
            }
        };
        when(servletResponse.getOutputStream()).thenReturn(servletOutputStream);
        LongStream.rangeClosed(minimumResponseSize, TOTAL_REQUESTS_TO_SEND).parallel().forEach((i) -> {

            try {
                final FilterChain filterChain = mock(FilterChain.class);
                doAnswer((invocation) -> {
                    final ServletResponse testServletResponse = invocation.getArgument(1, ServletResponse.class);
                    ServletOutputStream responseServletOutputStream = null;
                    PrintWriter responsePrintWriter = null;

                    if (i % 2 == 0) {
                        responsePrintWriter = testServletResponse.getWriter();
                    }
                    else {
                        responseServletOutputStream = testServletResponse.getOutputStream();
                    }

                    for (long j = 0; j < i; j++) {

                        if (responsePrintWriter != null) {
                            responsePrintWriter.write("b");
                        }
                        else {
                            responseServletOutputStream.print("b");
                        }
                    }

                    testServletResponse.flushBuffer();
                    return null;
                }).when(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

                servletMetricFilter.doFilter(servletRequest, servletResponse, filterChain);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });

        Assert.assertNull(servletRequest.getAttribute(MetricsServletFilter.MINIMUM_RESPONSE_SIZE));
        Assert.assertNull(servletRequest.getAttribute(MetricsServletFilter.MAXIMUM_RESPONSE_SIZE));
        Assert.assertNull(servletRequest.getAttribute(MetricsServletFilter.AVERAGE_RESPONSE_SIZE));

        when(servletRequest.getServletPath()).thenReturn(MetricsServletFilter.METRICS_JSP_PAGE);

        final Map<String, Object> requestAttrs = new HashMap<>();
        when(servletRequest.getAttribute(any(String.class))).thenAnswer((invocation) -> {
            return requestAttrs.get(invocation.getArgument(0, String.class));
        });
        doAnswer((invocation) -> {
            requestAttrs.put(invocation.getArgument(0, String.class), invocation.getArgument(1, Object.class));
            return null;
        }).when(servletRequest).setAttribute(any(String.class), any(Object.class));

        final FilterChain filterChain = mock(FilterChain.class);
        servletMetricFilter.doFilter(servletRequest, servletResponse, filterChain);

        Assert.assertEquals(minimumResponseSize,
                servletRequest.getAttribute(MetricsServletFilter.MINIMUM_RESPONSE_SIZE));
        Assert.assertEquals(TOTAL_REQUESTS_TO_SEND,
                servletRequest.getAttribute(MetricsServletFilter.MAXIMUM_RESPONSE_SIZE));
        Assert.assertEquals(LongStream.rangeClosed(minimumResponseSize, TOTAL_REQUESTS_TO_SEND).asDoubleStream().average()
                .getAsDouble(),
                servletRequest.getAttribute(MetricsServletFilter.AVERAGE_RESPONSE_SIZE));
        Assert.assertTrue(stringWriter.toString().length() > TOTAL_REQUESTS_TO_SEND);

        servletMetricFilter.destroy();
    }
}
