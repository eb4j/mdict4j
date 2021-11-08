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

import io.github.eb4j.mdict.data.DictionaryIndex;
import io.github.eb4j.mdict.data.DictionaryInfo;
import io.github.eb4j.mdict.data.MdxParser;
import io.github.eb4j.mdict.io.MDInputStream;

import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

public class Dictionary {
    private final File mdxFile;
    private final File mddFile;
    private final DictionaryInfo dictionaryInfo;
    private final DictionaryIndex dictionaryIndex;

    public Dictionary(final DictionaryInfo info, final DictionaryIndex index, final File mdx, final File mdd) {
        mdxFile = mdx;
        mddFile = mdd;
        dictionaryInfo = info;
        dictionaryIndex = index;
    }

    public Dictionary(final DictionaryInfo info, final DictionaryIndex index, final File mdxFile) {
        this(info, index, mdxFile, null);
    }

    public static Dictionary builder(final String mdxFile) throws MDException {
        DictionaryInfo info;
        DictionaryIndex index;
        File file = new File(mdxFile);
        if (!file.isFile()) {
            throw new MDException("Target file is not MDict file.");
        }
        try {
            MDInputStream ras = new MDInputStream(mdxFile);
            info = MdxParser.parseHeader(ras);
            index = MdxParser.parseIndex(ras, info);
        } catch (IOException | DataFormatException e) {
            throw new MDException("io error", e);
        }
        return new Dictionary(info, index, file);
    }
}
