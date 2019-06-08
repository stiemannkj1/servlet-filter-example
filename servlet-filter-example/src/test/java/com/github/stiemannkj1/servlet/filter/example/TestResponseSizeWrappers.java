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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * @author Kyle Stiemann
 */
public final class TestResponseSizeWrappers {

    @Test
    public final void testResponseSizeServletOutputStreamWrapperResponseSize() throws IOException {

        final StringWriter stringWriter = new StringWriter();
        final ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);
        doAnswer((invocation) -> {
            stringWriter.write(invocation.getArgument(0, Integer.class));
            return null;
        }).when(servletOutputStream).write(any(Integer.class));
        final ResponseSizeServletOutputStreamWrapper responseSizeServletOutputStreamWrapper =
                new ResponseSizeServletOutputStreamWrapper(servletOutputStream);
        final String testString = "test";
        responseSizeServletOutputStreamWrapper.print(testString);
        responseSizeServletOutputStreamWrapper.flush();
        Assert.assertEquals(testString, stringWriter.toString());
        Assert.assertEquals(testString.length(), responseSizeServletOutputStreamWrapper.getResponseSize());
    }

    @Test
    public final void testResponseSizeHttpServletResponseWrapperSpecCompliance() throws IOException {
        final HttpServletResponse httpServletResponse = newMockHttpServletResponse();
        final HttpServletResponse testGetWriterThenGetOutputStreamResponse =
                new ResponseSizeHttpServletResponseWrapper(httpServletResponse);
        testGetWriterThenGetOutputStreamResponse.getWriter();

        try {
            testGetWriterThenGetOutputStreamResponse.getOutputStream();
            Assert.fail("Failed to throw IllegalStateException when getOutputStream() called after getWriter().");
        } catch (IllegalStateException e) {
            // Test passed.
        }

        final HttpServletResponse testGetOutputStreamThenGetWriterResponse =
                new ResponseSizeHttpServletResponseWrapper(httpServletResponse);
        testGetOutputStreamThenGetWriterResponse.getOutputStream();

        try {
            testGetOutputStreamThenGetWriterResponse.getWriter();
            Assert.fail("Failed to throw IllegalStateException when getOutputStream() called after getOutputStream().");
        } catch (IllegalStateException e) {
            // Test passed.
        }
    }

    @Test
    public final void testResponseSizeHttpServletResponseWrapperOutputStream() throws IOException {
        testResponseSizeHttpServletResponseWrapper(WriteResponseWith.OUTPUT_STREAM, Flush.RESPONSE);
        testResponseSizeHttpServletResponseWrapper(WriteResponseWith.OUTPUT_STREAM, Flush.OUTPUT_STREAM);
    }

    @Test
    public final void testResponseSizeHttpServletResponseWrapperReset() throws IOException {
        final ResponseSizeHttpServletResponseWrapper testResponseWrapper =
                new ResponseSizeHttpServletResponseWrapper(newMockHttpServletResponse());
        testResponseWrapper.getWriter().print("test");
        testResponseWrapper.reset();
        Assert.assertEquals(0, testResponseWrapper.getResponseSize());

        try {
            testResponseWrapper.getOutputStream().print("test");
        } catch (IllegalStateException e) {
            throw new AssertionError("ResponseSizeHttpServletResponseWrapper.reset() failed to reset buffer state.", e);
        }

        testResponseWrapper.resetBuffer();
        Assert.assertEquals(0, testResponseWrapper.getResponseSize());

        try {
            testResponseSizeHttpServletResponseWrapper(WriteResponseWith.WRITER, Flush.RESPONSE);
        } catch (IllegalStateException e) {
            throw new AssertionError("ResponseSizeHttpServletResponseWrapper.reset() failed to reset buffer state.", e);
        }
    }

    @Test
    public final void testResponseSizeHttpServletResponseWrapperWriter() throws IOException {
        testResponseSizeHttpServletResponseWrapper(WriteResponseWith.WRITER, Flush.RESPONSE);
        testResponseSizeHttpServletResponseWrapper(WriteResponseWith.WRITER, Flush.WRITER);
    }

    private void testResponseSizeHttpServletResponseWrapper(WriteResponseWith writeResponseWith, Flush flush)
            throws IOException {

        final String testResponse = "test";
        final ResponseSizeHttpServletResponseWrapper testResponseWrapper =
                new ResponseSizeHttpServletResponseWrapper(newMockHttpServletResponse());

        if (writeResponseWith.equals(WriteResponseWith.WRITER)) {
            testResponseWrapper.getWriter().print(testResponse);
        } else {
            testResponseWrapper.getOutputStream().print(testResponse);
        }

        if (flush.equals(Flush.RESPONSE)) {
            testResponseWrapper.flushBuffer();
        } else if (flush.equals(Flush.WRITER)) {
            testResponseWrapper.getWriter().flush();
        } else {
            testResponseWrapper.getOutputStream().flush();
        }

        Assert.assertEquals(testResponse.length(), testResponseWrapper.getResponseSize());
    }

    private HttpServletResponse newMockHttpServletResponse() throws IOException {

        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final ServletOutputStream servletOutputStream = mock(ServletOutputStream.class);
        when(httpServletResponse.getOutputStream()).thenReturn(servletOutputStream);
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        return httpServletResponse;
    }

    enum WriteResponseWith {
        OUTPUT_STREAM,
        WRITER
    }

    enum Flush {
        OUTPUT_STREAM,
        RESPONSE,
        WRITER
    }
}
