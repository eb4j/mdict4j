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
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DictionaryTest {

    @Test
    void loadDictionary() throws URISyntaxException, MDException {
        Dictionary dictionary = Dictionary.loadData(
                Objects.requireNonNull(this.getClass().getResource("/test.mdx")).toURI().getPath());
        assertNotNull(dictionary);
        assertEquals(StandardCharsets.UTF_8, dictionary.getEncoding());
        assertEquals("Html", dictionary.getFormat());
        assertEquals("", dictionary.getStyleSheet());
        assertEquals("2021-11-11", dictionary.getCreationDate());
        assertEquals("EJDIC", dictionary.getTitle());
        assertEquals("\"UTF-8\" encoding.", dictionary.getDescription());
        for (Map.Entry<String, Object> entry: dictionary.getEntries("z")) {
            String word = entry.getKey();
            Object value = entry.getValue();
            String text = dictionary.getText((Long) value);
            assertNotNull(text);
        }
    }

}
