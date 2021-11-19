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

import io.github.eb4j.mdict.io.MDFileInputStream;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.zip.DataFormatException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MDictMDXParserTest {

    @Test
    void parseTestHeader() throws FileNotFoundException, URISyntaxException, MDException {
        MDFileInputStream inputStream = new MDFileInputStream(Objects.requireNonNull(
                this.getClass().getResource("/test.mdx")).toURI().getPath());
        MDictParser parser = MDictParser.createMDXParser(inputStream);
        MDictDictionaryInfo dictionaryInfo = parser.parseHeader();
        assertEquals("Html", dictionaryInfo.getFormat());
        assertEquals("2.0", dictionaryInfo.getRequiredEngineVersion());
        assertEquals(630, dictionaryInfo.getKeyBlockPosition());
        assertEquals("0", dictionaryInfo.getEncrypted());
    }

    @Test
    void parseTestIndex() throws MDException, URISyntaxException, IOException, DataFormatException {
        MDFileInputStream inputStream = new MDFileInputStream(Objects.requireNonNull(
                this.getClass().getResource("/test.mdx")).toURI().getPath());
        MDictParser parser = MDictParser.createMDXParser(inputStream);
        parser.parseHeader();
        DictionaryData<Object> index = parser.parseIndex(null);
        assertEquals(100, index.size());
        RecordIndex recordIndex = parser.parseRecordBlock();
        assertEquals(81, recordIndex.getRecordNumEntries());
    }

}
