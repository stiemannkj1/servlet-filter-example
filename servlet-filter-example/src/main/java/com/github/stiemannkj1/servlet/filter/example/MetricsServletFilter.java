/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.stiemannkj1.servlet.filter.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author kyle
 */
public final class MetricsServletFilter implements Filter {

    /* package-private */ static final String UNIQUE_RESPONSE_ID = MetricsServletFilter.class.getName()
            + ".UNIQUE_RESPONSE_ID";
    /* package-private */ static final String MINIMUM_RESPONSE_SIZE = "minimumResponseSize";
    /* package-private */ static final String MAXIMUM_RESPONSE_SIZE = "maximumResponseSize";
    /* package-private */ static final String AVERAGE_RESPONSE_SIZE = "averageResponseSize";
    /* package-private */ static final String METRICS_JSP_PAGE = "/" + MetricsServletFilter.class.getName().replace(".", "_") + ".jsp";

    private final AtomicLong uniqueResponseId = new AtomicLong();
    private final ConcurrentLinkedQueue<Long> responseSizes = new ConcurrentLinkedQueue<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        uniqueResponseId.set(0);
        responseSizes.clear();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final String servletPath = httpServletRequest.getServletPath();

        if (servletPath != null && servletPath.equals(METRICS_JSP_PAGE)) {

            List<Long> responseSizes = new ArrayList<>(this.responseSizes);
            long minimumResponseSize = responseSizes.stream().min(Comparator.naturalOrder()).orElse(0L);
            httpServletRequest.setAttribute(MINIMUM_RESPONSE_SIZE, minimumResponseSize);
            long maximumResponseSize = responseSizes.stream().max(Comparator.naturalOrder()).orElse(0L);
            httpServletRequest.setAttribute(MAXIMUM_RESPONSE_SIZE, maximumResponseSize);
            double averageResponseSize = responseSizes.stream().mapToDouble(i -> i).average().orElse(0L);
            httpServletRequest.setAttribute(AVERAGE_RESPONSE_SIZE, averageResponseSize);
            chain.doFilter(httpServletRequest, response);
        } else {

            final ResponseSizeHttpServletResponseWrapper httpServletResponse =
                    new ResponseSizeHttpServletResponseWrapper((HttpServletResponse) response);
            httpServletResponse.addHeader(UNIQUE_RESPONSE_ID, Long.toString(uniqueResponseId.getAndIncrement()));
            chain.doFilter(httpServletRequest, httpServletResponse);
            responseSizes.add(httpServletResponse.getResponseSize());
        }
    }

    @Override
    public void destroy() {
        uniqueResponseId.set(0);
        responseSizes.clear();
    }
}
