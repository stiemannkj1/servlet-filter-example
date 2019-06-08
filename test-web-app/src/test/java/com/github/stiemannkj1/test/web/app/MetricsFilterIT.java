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
package com.github.stiemannkj1.test.web.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Kyle Stiemann
 */
public final class MetricsFilterIT {

    private static final int TOTAL_REQUESTS_TO_SEND = 100;
    private static final Pattern GET_LINKS_PATTERN = Pattern.compile("<a\\s+href=[\"]([^\"]+)[\"]");
    private static final Pattern GET_MINIMUM_RESPONSE_SIZE = Pattern.compile(getMetricRegex("minimumResponseSize"));
    private static final Pattern GET_MAXIMUM_RESPONSE_SIZE = Pattern.compile(getMetricRegex("maximumResponseSize"));
    private static final Pattern GET_AVERAGE_RESPONSE_SIZE = Pattern.compile(getMetricRegex("averageResponseSize"));
    private static final Pattern GET_MINIMUM_RESPONSE_TIME = Pattern.compile(getMetricRegex("minimumResponseTime"));
    private static final Pattern GET_MAXIMUM_RESPONSE_TIME = Pattern.compile(getMetricRegex("maximumResponseTime"));
    private static final Pattern GET_AVERAGE_RESPONSE_TIME = Pattern.compile(getMetricRegex("averageResponseSize"));
    private static final String TEST_WEBAPP_BASE_URL =
            "http://localhost:" + System.getProperty("it.test.server.port", "8080") + "/test-web-app";

    private static String getMetricRegex(String id) {
        return "id=\"" + id + "\"[^>]*>([^<]+)";
    }

    @Test
    public final void testMetricsFilter() {

        final String indexHtml = getHtmlResponse(TEST_WEBAPP_BASE_URL);
        final Matcher matcher = GET_LINKS_PATTERN.matcher(indexHtml);
        final Set<String> pagesSet = new HashSet<>();

        while (matcher.find()) {
            pagesSet.add(matcher.group(1));
        }

        Assert.assertFalse(pagesSet.isEmpty());

        final int totalLinks = pagesSet.size();
        final List<String> pages = Collections.unmodifiableList(new ArrayList<String>(pagesSet));

        // Send multiple concurrent requests to each page.
        IntStream.rangeClosed(1, TOTAL_REQUESTS_TO_SEND).parallel().forEach((i) -> {
            assertPageRendered(pages.get(i % totalLinks));
        });

        final String metricsHtml = getHtmlResponse(TEST_WEBAPP_BASE_URL + "/" +
                "com_github_stiemannkj1_servlet_filter_example_MetricsServletFilter.jsp");
        Assert.assertTrue(getMetric(GET_MINIMUM_RESPONSE_SIZE, metricsHtml) > 0);
        Assert.assertTrue(getMetric(GET_MAXIMUM_RESPONSE_SIZE, metricsHtml) > 0);
        Assert.assertTrue(getMetric(GET_AVERAGE_RESPONSE_SIZE, metricsHtml) > 0);
        Assert.assertTrue(getMetric(GET_MINIMUM_RESPONSE_TIME, metricsHtml) > 0);
        Assert.assertTrue(getMetric(GET_MAXIMUM_RESPONSE_TIME, metricsHtml) > 0);
        Assert.assertTrue(getMetric(GET_AVERAGE_RESPONSE_TIME, metricsHtml) > 0);
    }

    private void assertPageRendered(String page) {
        final String pageHtmlResponse = getHtmlResponse(TEST_WEBAPP_BASE_URL + "/" + page);
        Assert.assertTrue(pageHtmlResponse.contains(page) && pageHtmlResponse.contains("Hello World!"));
    }

    private String getHtmlResponse(String urlString) throws UncheckedIOException {

        try {
            final URL url = new URL(urlString);
            final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            try (final InputStream inputStream = httpURLConnection.getInputStream();
                    final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);) {
                return bufferedReader.lines().parallel().collect(Collectors.joining("\n"));
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private double getMetric(Pattern getMetricPattern, String metricsHtml) {
        final Matcher matcher = getMetricPattern.matcher(metricsHtml);
        matcher.find();
        return Double.parseDouble(matcher.group(1));
    }
}
