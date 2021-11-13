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

import io.github.eb4j.mdict.data.MDictEntry;

import java.util.*;

/**
 * @author Hiroshi Miura
 */
public class Searcher implements ISearch {

    private final Dictionary dictionary;
    private final List<Map.Entry<String, MDictEntry>> entries = new ArrayList<>();
    private Iterator<Map.Entry<String, MDictEntry>> iterator;

    public Searcher(final Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void search(final String word) {
        entries.clear();
        entries.addAll(dictionary.getEntries(word));
        iterator = entries.listIterator();
    }

    @Override
    public Result getNextResult() throws MDException {
        Map.Entry<String, MDictEntry> entry = iterator.next();
        if (entry == null) {
            return null;
        }
        String key = entry.getKey();
        String text = dictionary.getText(entry.getValue());
        return new Result(key, text);
    }

}
