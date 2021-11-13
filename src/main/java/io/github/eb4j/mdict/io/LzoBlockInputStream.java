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
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.lzo_uintp;

import java.io.IOException;

public class LzoBlockInputStream extends MDBlockInputStream {

    public LzoBlockInputStream(final MDInputStream inputStream, final long compSize, final long decompSize,
                               final long checksum) throws IOException, MDException {
        super();
        byte[] input = new byte[(int) compSize];
        inputStream.readFully(input);
        LzoAlgorithm algorithm = LzoAlgorithm.LZO1X;
        LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
        byte[] output = new byte[(int) decompSize];
        lzo_uintp outLen = new lzo_uintp();
        decompressor.decompress(input, 0, (int) compSize, output, 0, outLen);
        if (outLen.value != decompSize) {
            throw new MDException("Decompression size is differ.");
        }
        bytes.put(output);
    }
}
