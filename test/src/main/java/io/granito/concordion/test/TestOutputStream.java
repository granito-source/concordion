/*
 * Copyright 2025 Alexei Yashkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.granito.concordion.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A test double implementation for {@link OutputStream} that captures
 * written data in memory and allows simulating write and close
 * exceptions.
 */
public class TestOutputStream extends OutputStream {
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    private IOException writeException;

    private IOException closeException;

    private boolean closed = false;

    /**
     * Sets an exception to be thrown on the next write operation.
     *
     * @param exception the exception to throw
     */
    public void setWriteException(IOException exception)
    {
        writeException = exception;
    }

    /**
     * Captures the provided {@code byte} in this output stream.
     *
     * @param b the {@code byte} to write
     * @throws IOException when a write exception is set using
     * {@link #setWriteException(IOException)}
     */
    @Override
    public void write(int b) throws IOException
    {
        checkAndThrowWriteException();
        content.write(b);
    }

    /**
     * Captures the provided byte array in this output stream.
     *
     * @param buf the data to write
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IOException when a write exception is set using
     * {@link #setWriteException(IOException)}
     */
    @Override
    public void write(byte[] buf, int off, int len) throws IOException
    {
        checkAndThrowWriteException();
        content.write(buf, off, len);
    }

    /**
     * Sets an exception to be thrown on the next close operation.
     *
     * @param exception the exception to throw
     */
    public void setCloseException(IOException exception)
    {
        closeException = exception;
    }

    /**
     * Indicates whether this output stream has been closed.
     *
     * @return {@code true} if this output stream is closed,
     * {@code false} otherwise
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Closes this output stream.
     *
     * @throws IOException when a close exception is set using
     * {@link #setCloseException(IOException)}
     */
    @Override
    public void close() throws IOException
    {
        checkAndThrowCloseException();
        content.close();
        this.closed = true;
    }

    /**
     * Returns the content captured in this output stream as a byte array.
     *
     * @return the content as a byte array
     */
    public byte[] toByteArray()
    {
        return content.toByteArray();
    }

    /**
     * Returns the content captured in this output stream as a string
     * using UTF-8 encoding.
     *
     * @return the content as a string
     */
    @Override
    public String toString()
    {
        return content.toString(StandardCharsets.UTF_8);
    }

    private void checkAndThrowWriteException() throws IOException
    {
        if (writeException != null)
            try {
                throw writeException;
            } finally {
                writeException = null;
            }
    }

    private void checkAndThrowCloseException() throws IOException
    {
        if (closeException != null)
            try {
                throw closeException;
            } finally {
                closeException = null;
            }
    }
}
