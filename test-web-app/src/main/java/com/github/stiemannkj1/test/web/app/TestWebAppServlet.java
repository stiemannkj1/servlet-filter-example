/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.stiemannkj1.test.web.app;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Kyle Stiemann
 */
public class TestWebAppServlet extends HttpServlet {

    private static final String PAGE_NUMBER_PLACEHOLDER = "PAGE_NUMBER_PLACEHOLDER";
    private static final String TODAY_PLACEHOLDER = "TODAY_PLACEHOLDER";
    private static final String RESPONSE_TEMPLATE =
            "<!DOCTYPE html>\n"
            + "<html>\n"
            + "    <head>\n"
            + "        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
            + "        <title>page" + PAGE_NUMBER_PLACEHOLDER + "</title>\n"
            + "    </head>\n"
            + "    <body>\n"
            + "        <h1>page" + PAGE_NUMBER_PLACEHOLDER + "</h1>\n"
            + "        <p>Hello World!</p>\n"
            + "        <p id=\"today\">" + TODAY_PLACEHOLDER + "</p>\n"
            + "        <a href=\"./index.jsp\">Back to index.jsp.</a>\n"
            + "    </body>\n"
            + "</html>";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final Date today = new Date();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (request.getServletPath().contains("page5")) {
            response.getWriter().write(RESPONSE_TEMPLATE.replace(PAGE_NUMBER_PLACEHOLDER, "5")
                    .replace(TODAY_PLACEHOLDER, simpleDateFormat.format(today)));
        } else if (request.getServletPath().contains("page6")) {
            response.getOutputStream().print(RESPONSE_TEMPLATE.replace(PAGE_NUMBER_PLACEHOLDER, "6")
                    .replace(TODAY_PLACEHOLDER, simpleDateFormat.format(today)));
        } else {
            throw new IllegalStateException();
        }

        response.flushBuffer();
    }
}
