/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 *
 * @author kyle
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
        }
        else if (responseSizePrintWriter != null) {
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

    /**
     * @return the response size in bytes. 
     */
    public long getResponseSize() {

        if (responseSizeServletOutputStreamWrapper != null) {
            return responseSizeServletOutputStreamWrapper.getResponseSize();
        }

        return 0;
    }

    @Override
    public PrintWriter getWriter() throws IOException {

        if (getOutputStreamCalled) {
            throw new IllegalStateException("getOutputStream() already called for this repsonse.");
        }

        if (responseSizePrintWriter == null) {
            responseSizeServletOutputStreamWrapper = new ResponseSizeServletOutputStreamWrapper(super.getOutputStream());
            String characterEncoding = getResponse().getCharacterEncoding();

            if (characterEncoding == null) {
                characterEncoding = StandardCharsets.UTF_8.toString();
            }

            responseSizePrintWriter = new AutoFlushingPrintWriter(responseSizeServletOutputStreamWrapper, characterEncoding);
        }

        return responseSizePrintWriter;
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
