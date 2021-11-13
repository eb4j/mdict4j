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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MDBlockInputStream extends InputStream {
    protected final ByteArrayInputStream byteArrayInputStream;

    public MDBlockInputStream(final ByteArrayInputStream byteArrayInputStream) {
        this.byteArrayInputStream = byteArrayInputStream;
    }

    public void readFully(final byte[] b) throws IOException {
        int r = byteArrayInputStream.read(b);
        if (r < b.length) {
            throw new IOException("Cannot read fully.");
        }
    }

    public void skip(final int size) {
        byteArrayInputStream.skip(size);
    }

    public int read() {
        return byteArrayInputStream.read();
    }
}
