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
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearcherTest {

    @Test
    void search() throws MDException, URISyntaxException {
        Dictionary dictionary = Dictionary.loadData(
                Objects.requireNonNull(SearcherTest.class.getResource("/test.mdx")).toURI().getPath(), null);
        Searcher searcher = new Searcher(dictionary);
        searcher.search("z");
        Result result = searcher.getNextResult();
        assertEquals("z", result.getHeading());
        assertEquals("英語アルファベットの第26字(最後の字) ", result.getText());
        result = searcher.getNextResult();
        assertEquals("z", result.getHeading());
        result = searcher.getNextResult();
        assertEquals("z.", result.getHeading());
        assertEquals("zone", result.getText());
    }
}
