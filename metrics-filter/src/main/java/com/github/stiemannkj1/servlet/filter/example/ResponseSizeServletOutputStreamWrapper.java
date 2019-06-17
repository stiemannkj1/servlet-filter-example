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
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * Tracks the current response size which can be obtained via {@link #getResponseSize()}.
 *
 * @author Kyle Stiemann
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
