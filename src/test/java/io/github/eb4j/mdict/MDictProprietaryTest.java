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

import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MDictProprietaryTest {

    private static final String TARGET = "/proprietaryData/proprietary.mdx";
    private static final String TARGET2 = "/proprietaryData/English-Italian-Dictionary.mdx";

    @Test
    @EnabledIf("targetFileExist")
    // Test with proprietary dictionary data; requires dictionary prepared by tester
    void loadProprietaryDictionary() throws URISyntaxException, MDException, IOException {
        MDictDictionary dictionary = MDictDictionary.loadDicitonary(Objects.requireNonNull(
                this.getClass().getResource(TARGET)).toURI().getPath());
        assertTrue(dictionary.isMdx());
        assertEquals(StandardCharsets.UTF_8, dictionary.getEncoding());
        assertFalse(dictionary.isHeaderEncrypted());
        assertTrue(dictionary.isIndexEncrypted());
        assertEquals("2.0", dictionary.getMdxVersion());
        assertEquals("Html", dictionary.getFormat());
        String[] queries = new String[] {"test", "script", "zoom"};
        for (String query: queries) {
            Map.Entry<String, String> entry = dictionary.readArticles(query).get(0);
            String word = entry.getKey();
            assertEquals(query, word);
            String text = entry.getValue();
            assertTrue(text.startsWith("<link rel=\"stylesheet\""));
        }
        MDictDictionary dictData = MDictDictionary.loadDictionaryData(Objects.requireNonNull(
                this.getClass().getResource(TARGET)).toURI().getPath());
        assertFalse(dictData.isMdx());
        Map.Entry<String, Object> entry = dictData.getEntries("/audio/test.mp3").get(0);
        String word = entry.getKey();
        assertEquals("/audio/test.mp3", word);
        Object value = entry.getValue();
        assertTrue(value instanceof Long);
        byte[] buf = dictData.getData((Long) value);
        Tika tika = new Tika();
        String mediaType = tika.detect(buf);
        assertEquals("audio/mpeg", mediaType);
    }

    boolean targetFileExist() {
        return this.getClass().getResource(TARGET) != null;
    }

    @Test
    @EnabledIf("target2FileExist")
    void loadItalianDictionary() throws URISyntaxException, MDException {
        MDictDictionary dictionary = MDictDictionary.loadDicitonary(Objects.requireNonNull(
                this.getClass().getResource(TARGET)).toURI().getPath());
        assertTrue(dictionary.isMdx());
        assertEquals(StandardCharsets.UTF_8, dictionary.getEncoding());
        assertFalse(dictionary.isHeaderEncrypted());
        assertTrue(dictionary.isIndexEncrypted());
        assertEquals("2.0", dictionary.getMdxVersion());
        assertEquals("Html", dictionary.getFormat());
        String[] queries = new String[] {"test", "script", "zoom"};
        for (String query: queries) {
            Map.Entry<String, String> entry = dictionary.readArticles(query).get(0);
            String word = entry.getKey();
            assertEquals(query, word);
            String text = entry.getValue();
            assertTrue(text.startsWith("<link rel=\"stylesheet\""));
        }
    }

    boolean target2FileExist() {
        return this.getClass().getResource(TARGET2) != null;
    }
}
