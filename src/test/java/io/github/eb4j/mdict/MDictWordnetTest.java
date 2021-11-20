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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MDictWordnetTest {

    @Test
    void loadDictionary() throws URISyntaxException, MDException, IOException {
        MDictDictionary dictionary = MDictDictionary.loadDicitonary(
                Objects.requireNonNull(this.getClass().getResource("/wordnet.mdx")).toURI().getPath());
        assertNotNull(dictionary);
        assertEquals("1.2", dictionary.getMdxVersion());
        assertEquals(StandardCharsets.ISO_8859_1, dictionary.getEncoding());
        assertEquals("Html", dictionary.getFormat());
        assertEquals("WordNet 2.0", dictionary.getTitle());
        assertEquals("1 <font color=red size=+2><b> </b></font><br><br>" +
                " 2 <font color=#990066><I>( )</I></font><br>" +
                " 3    4 <br>  5 <br><font color=#666666><I> </I></font> ",
                dictionary.getStyleSheet());
        for (Map.Entry<String, Object> entry: dictionary.getEntries("z")) {
            String word = entry.getKey();
            assertNotNull(word);
            Object value = entry.getValue();
            String text = dictionary.getText((Long) value);
            assertNotNull(text);
        }
    }

}
