/*
 * MD4J, a parser library for MDict format.
 * Copyright (C) 2021 Hiroshi Miura.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.eb4j.mdict.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MDInputStream implements AutoCloseable {

    private final RandomAccessFile file;

    public MDInputStream(final String filename) throws FileNotFoundException {
        file = new RandomAccessFile(filename, "r");
    }

    public void seek(final long pos) throws IOException {
        file.seek(pos);
    }

    public long tell() throws IOException {
        return file.getFilePointer();
    }

    public int read() throws IOException {
        return file.read();
    }

    public void readFully(final byte[] b) throws IOException {
        file.readFully(b);
    }

    public int read(final byte[]  b) throws IOException {
        return file.read(b);
    }

    public void peek(final byte[] b) throws IOException {
        long pos = file.getFilePointer();
        file.readFully(b);
        file.seek(pos);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    public void skip(final int i) throws IOException {
        byte[] buf = new byte[i];
        file.readFully(buf);
    }
}
