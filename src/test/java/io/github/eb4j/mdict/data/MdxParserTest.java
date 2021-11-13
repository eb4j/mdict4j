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

package io.github.eb4j.mdict.data;

import io.github.eb4j.mdict.MDException;
import io.github.eb4j.mdict.data.DictionaryIndex;
import io.github.eb4j.mdict.data.DictionaryInfo;
import io.github.eb4j.mdict.data.MdxParser;
import io.github.eb4j.mdict.io.MDInputStream;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.zip.DataFormatException;

import static org.junit.jupiter.api.Assertions.*;

class MdxParserTest {

    @Test
    void parseTestHeader() throws FileNotFoundException, URISyntaxException, MDException {
        MDInputStream inputStream = new MDInputStream(Objects.requireNonNull(
                this.getClass().getResource("/test.mdx")).toURI().getPath());
        DictionaryInfo dictionaryInfo = MdxParser.parseHeader(inputStream);
        assertEquals("Html", dictionaryInfo.getFormat());
        assertEquals("2.0", dictionaryInfo.getRequiredEngineVersion());
        assertEquals(630, dictionaryInfo.getKeyBlockPosition());
        assertEquals("0", dictionaryInfo.getEncrypted());
    }

    @Test
    void parseTestIndex() throws MDException, URISyntaxException, IOException, DataFormatException {
        MDInputStream inputStream = new MDInputStream(Objects.requireNonNull(
                this.getClass().getResource("/test.mdx")).toURI().getPath());
        DictionaryInfo info = new DictionaryInfo();
        info.setKeyBlockPosition(630);
        info.setEncoding("UTF-8");
        DictionaryIndex index = MdxParser.parseIndex(inputStream, info);
        assertEquals(81, index.keySize());
        assertEquals(3289, index.getRecordCompSize(0));
        assertEquals(6422, index.getRecordDecompSize(0));
        assertEquals(1331, index.getRecordOffset(0));
        assertEquals(1, index.getKeyNumBlocks());
    }
}