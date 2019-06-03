/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.stiemannkj1.test.web.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author kyle
 */
public final class WebAppIT {

    private static final int TOTAL_REQUESTS_TO_SEND = 100;
    private static final Pattern GET_LINKS_PATTERN = Pattern.compile("<a\\s+href=[\"]([^\"]+)[\"]");
    private static final String TEST_WEBAPP_BASE_URL = "http://localhost:"
            + System.getProperty("it.test.server.port", "8080") + "/test-web-app";

    @Test
    public final void testWebAppIT() {

        final String indexHtml = getHtmlResponse(TEST_WEBAPP_BASE_URL);
        final Matcher matcher = GET_LINKS_PATTERN.matcher(indexHtml);
        final Set<String> links = new HashSet<>();

        while (matcher.find()) {
            links.add(matcher.group(1));
        }

        Assert.assertFalse(links.isEmpty());

        final int totalLinks = links.size();

        // Ensure that all pages are tested at least once.
        IntStream.rangeClosed(1, totalLinks).parallel().forEach((i) -> {
            assertPageRendered(i);
        });

        // Send multiple requests to random pages.
        IntStream.rangeClosed(1, TOTAL_REQUESTS_TO_SEND).parallel().forEach((i) -> {
            final int pageNumber = ThreadLocalRandom.current().nextInt(totalLinks) + 1;
            assertPageRendered(pageNumber);
        });
    }

    private void assertPageRendered(int pageNumber) {
        final String pageName = "page" + pageNumber + ".jsp";
        final String pageHtmlResponse = getHtmlResponse(TEST_WEBAPP_BASE_URL + "/" + pageName);
        Assert.assertTrue(pageHtmlResponse.contains(pageName) && pageHtmlResponse.contains("Hello World!"));
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
}
