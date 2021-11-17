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

package io.github.eb4j.mdict;

import io.github.eb4j.mdict.data.Dictionary;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

public class MDict {

    private final String mdxPath;
    private final String mddPath;
    private final byte[] keyword = new byte[32];

    public MDict(final String mdxPath) throws MDException, IOException {
        this.mdxPath = mdxPath;
        File mdxFile = new File(mdxPath);
        if (!mdxFile.isFile()) {
            throw new MDException("directory not found.");
        }
        if (!mdxFile.canRead()) {
            throw new MDException("directory cannot read.");
        }

        String f = mdxPath;
        if (f.endsWith(".mdx")) {
            f = f.substring(0, f.length() - ".mdx".length());
        }
        String dictName = f;
        String parent = mdxFile.getParent();

        File mddFile = new File(dictName + ".mdd");
        if (mddFile.isFile()) {
            mddPath = mddFile.getPath();
        } else {
            mddPath = null;
        }
        File keyFile = new File(parent, "dictionary.key");
        if (keyFile.isFile() && keyFile.canRead()) {
            try (Stream<String> lines = Files.lines(keyFile.toPath())) {
                String first = lines.findFirst().orElse(null);
                byte[] temp = Hex.decode(first);
                System.arraycopy(temp, 0, keyword, 0, 32);
            }
        }
    }

    public Dictionary getDictionary() throws MDException {
        return Dictionary.loadData(mdxPath, keyword);
    }

    public Dictionary getMDD() throws MDException {
        return Dictionary.loadData(mddPath, keyword);
    }
}
