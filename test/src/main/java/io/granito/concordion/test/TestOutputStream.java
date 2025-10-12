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

public class TestOutputStream extends OutputStream {
    private final ByteArrayOutputStream content = new ByteArrayOutputStream();

    private IOException writeException;

    private IOException closeException;

    private boolean closed = false;

    public void setWriteException(IOException exception)
    {
        writeException = exception;
    }

    @Override
    public void write(int b) throws IOException
    {
        checkAndThrowWriteException();
        content.write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException
    {
        checkAndThrowWriteException();
        content.write(buf, off, len);
    }

    public void setCloseException(IOException exception)
    {
        closeException = exception;
    }

    public boolean isClosed()
    {
        return closed;
    }

    @Override
    public void close() throws IOException
    {
        checkAndThrowCloseException();
        content.close();
        this.closed = true;
    }

    public byte[] toByteArray()
    {
        return content.toByteArray();
    }

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
