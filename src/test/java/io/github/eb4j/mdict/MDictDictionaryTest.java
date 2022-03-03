/*
 * MD4J, a parser library for MDict format.
 * Copyright (C) 2021,2022 Hiroshi Miura.
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

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MDictDictionaryTest {

    @Test
    void loadV2Dictionary() throws URISyntaxException, MDException {
        MDictDictionary dictionary = MDictDictionary.loadDictionary(
                Objects.requireNonNull(this.getClass().getResource("/test.mdx")).toURI().getPath());
        assertNotNull(dictionary);
        assertEquals(StandardCharsets.UTF_8, dictionary.getEncoding());
        assertEquals("Html", dictionary.getFormat());
        assertEquals("", dictionary.getStyleSheet());
        assertEquals("2021-11-11", dictionary.getCreationDate());
        assertEquals("EJDIC", dictionary.getTitle());
        assertEquals("\"UTF-8\" encoding.", dictionary.getDescription());
        Map.Entry<String, Object> entry = dictionary.getEntries("Z").get(0);
        Object value = entry.getValue();
        String text = dictionary.getText((Long) value);
        assertNotNull(text);
        Map.Entry<String, String> result = dictionary.readArticles("Z").get(0);
        assertEquals("Z", result.getKey());
        assertEquals(text, result.getValue());
        Map.Entry<String, String> result2 = dictionary.readArticlesPredictive("Z").get(0);
        assertEquals(text, result2.getValue());
    }

    @Test
    void loadV1Dictionary() throws URISyntaxException, MDException {
        MDictDictionary dictionary = MDictDictionary.loadDictionary(
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
        Map.Entry<String, Object> entry = dictionary.getEntries("test").get(0);
        String word = entry.getKey();
        assertNotNull(word);
        Object value = entry.getValue();
        String text = dictionary.getText((Long) value);
        assertNotNull(text);
        Map.Entry<String, String> result = dictionary.readArticles("test").get(0);
        assertEquals(text, result.getValue());
    }

    @Test
    void loadMultipleDictionary() throws URISyntaxException, MDException {
        String[] words = new String[] {"test", "word"};
        List<MDictDictionary> dictionaries = new ArrayList<>();
        dictionaries.add(MDictDictionary.loadDictionary(
                Objects.requireNonNull(this.getClass().getResource("/test.mdx")).toURI().getPath()));
        dictionaries.add(MDictDictionary.loadDictionary(
                Objects.requireNonNull(this.getClass().getResource("/wordnet.mdx")).toURI().getPath()));
        for (String word: words) {
            for (MDictDictionary dict : dictionaries) {
                for (Map.Entry<String, Object> entry : dict.getEntries(word)) {
                    checkEntry(dict, entry);
                }
            }
        }
    }

    private void checkEntry(final MDictDictionary mdictionary, final Map.Entry<String, Object> entry)
            throws MDException {
        String key;
        String value;
        if (entry.getValue() instanceof Long) {
            key = entry.getKey();
            value = cleaHtmlArticle(mdictionary.getText((Long) entry.getValue()));
            assertNotNull(key);
            assertNotNull(value);
        } else {
            Long[] values = (Long[]) entry.getValue();
            for (final Long aLong : values) {
                key = entry.getKey();
                value = cleaHtmlArticle(mdictionary.getText(aLong));
                assertNotNull(key);
                assertNotNull(value);
            }
        }
    }

    private String cleaHtmlArticle(final String mdictHtmlText) {
        Safelist whitelist = new Safelist();
        whitelist.addTags("b", "br");
        whitelist.addAttributes("font", "color", "face");
        return Jsoup.clean(mdictHtmlText, whitelist);
    }
}
