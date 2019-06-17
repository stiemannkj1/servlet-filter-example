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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Allows access to the current response size in bytes via {@link #getResponseSize()}. This class returns a
 * {@link ResponseSizeServletOutputStreamWrapper} to calculate the response size when either {@link #getOutputStream()}
 * or {@link #getWriter()} is called. If {@link #getWriter()} is called, the {@link
 * ResponseSizeServletOutputStreamWrapper} is wrapped with an {@link AutoFlushingPrintWriter}.
 *
 * @author Kyle Stiemann
 */
final class ResponseSizeHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private boolean getOutputStreamCalled = false;
    private PrintWriter responseSizePrintWriter;
    private ResponseSizeServletOutputStreamWrapper responseSizeServletOutputStreamWrapper;

    public ResponseSizeHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void flushBuffer() throws IOException {

        if (getOutputStreamCalled) {
            responseSizeServletOutputStreamWrapper.flush();
        } else if (responseSizePrintWriter != null) {
            responseSizePrintWriter.flush();
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {

        if (responseSizePrintWriter != null) {
            throw new IllegalStateException("getWriter() already called for this repsonse.");
        }

        if (responseSizeServletOutputStreamWrapper == null) {
            responseSizeServletOutputStreamWrapper =
                    new ResponseSizeServletOutputStreamWrapper(super.getOutputStream());
            getOutputStreamCalled = true;
        }

        return responseSizeServletOutputStreamWrapper;
    }

    @Override
    public PrintWriter getWriter() throws IOException {

        if (getOutputStreamCalled) {
            throw new IllegalStateException("getOutputStream() already called for this repsonse.");
        }

        if (responseSizePrintWriter == null) {
            responseSizeServletOutputStreamWrapper =
                    new ResponseSizeServletOutputStreamWrapper(super.getOutputStream());
            String characterEncoding = getResponse().getCharacterEncoding();

            if (characterEncoding == null) {
                characterEncoding = StandardCharsets.UTF_8.toString();
            }

            responseSizePrintWriter =
                    new AutoFlushingPrintWriter(responseSizeServletOutputStreamWrapper, characterEncoding);
        }

        return responseSizePrintWriter;
    }

    /**
     * @return the response size in bytes.
     */
    long getResponseSize() {

        if (responseSizeServletOutputStreamWrapper != null) {
            return responseSizeServletOutputStreamWrapper.getResponseSize();
        }

        return 0;
    }

    @Override
    public void reset() {
        resetResponseSizeBuffers();
        super.reset();
    }

    @Override
    public void resetBuffer() {
        resetResponseSizeBuffers();
        super.resetBuffer();
    }

    private void resetResponseSizeBuffers() {
        responseSizePrintWriter = null;
        responseSizeServletOutputStreamWrapper = null;
        getOutputStreamCalled = false;
    }

    private static final class AutoFlushingPrintWriter extends PrintWriter {

        public AutoFlushingPrintWriter(ServletOutputStream servletOutputStream, String characterEncoding)
                throws UnsupportedEncodingException {
            super(new OutputStreamWriter(servletOutputStream, characterEncoding), true);
        }

        @Override
        public void write(char[] buf, int off, int len) {
            super.write(buf, off, len);
            super.flush();
        }
    }
}
