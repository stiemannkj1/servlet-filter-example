/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.stiemannkj1.servlet.filter.example;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 *
 * @author kyle
 */
final class ResponseSizeServletOutputStreamWrapper extends ServletOutputStream {

    private final ServletOutputStream wrappedServletOutputStream;

    private long responseSize = 0;

    public ResponseSizeServletOutputStreamWrapper(ServletOutputStream wrappedServletOutputStream) {
        this.wrappedServletOutputStream = wrappedServletOutputStream;
    }

    @Override
    public void close() throws IOException {
        wrappedServletOutputStream.close();
    }

    @Override
    public void flush() throws IOException {
        wrappedServletOutputStream.flush();
    }

    /**
     * @return the response size in bytes. 
     */
    public long getResponseSize() {
        return responseSize;
    }

    @Override
    public boolean equals(Object obj) {
        return wrappedServletOutputStream.equals(obj);
    }

    @Override
    public int hashCode() {
        return wrappedServletOutputStream.hashCode();
    }

    @Override
    public boolean isReady() {
        return wrappedServletOutputStream.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        wrappedServletOutputStream.setWriteListener(writeListener);
    }

    @Override
    public String toString() {
        return wrappedServletOutputStream.toString();
    }

    @Override
    public void write(int b) throws IOException {
        responseSize++;
        wrappedServletOutputStream.write(b);
    }
}
