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

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MDBlockInputStream extends MDInputStream {
    protected final ByteArrayInputStream byteArrayInputStream;

     public MDBlockInputStream(final ByteArrayInputStream byteArrayInputStream) {
        this.byteArrayInputStream = byteArrayInputStream;
    }

    public void readFully(@NotNull final byte[] b) throws IOException {
        int r = byteArrayInputStream.read(b);
        if (r < b.length) {
            throw new IOException("Cannot read fully.");
        }
    }

    public void skip(final int size) {
        byteArrayInputStream.skip(size);
    }

    /**
     * read one byte and return value ranged form 0-255.
     * When end-of-data reached, return -1.
     * @return the next byte of data, or -1 if the end of the stream has been reached.
     */
    public int read() {
        return byteArrayInputStream.read();
    }
}
