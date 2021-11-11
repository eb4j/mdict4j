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

import io.github.eb4j.mdict.MDException;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

public class ZlibBlockInputStream extends MDBlockInputStream implements AutoCloseable {

    public ZlibBlockInputStream(final MDInputStream inputStream, final long compSize, final long decompSize,
                                final long checksum) throws IOException, DataFormatException, MDException {
        super();
        Inflater decompressor = new Inflater();
        byte[] input = new byte[(int) compSize];
        byte[] output = new byte[(int) decompSize];
        inputStream.readFully(input);
        decompressor.setInput(input);
        int size = decompressor.inflate(output);
        if (checksum != decompressor.getAdler()) {
            throw new MDException("checksum error");
        }
        decompressor.end();
        if (size != decompSize) {
            throw new MDException("Decompression error, wrong size.");
        }
        bytes = ByteBuffer.wrap(output);
    }

    @Override
    public void close() throws Exception {
    }
}
