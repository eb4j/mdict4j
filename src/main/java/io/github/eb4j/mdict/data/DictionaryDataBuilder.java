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
import org.trie4j.MapTrie;
import org.trie4j.patricia.MapPatriciaTrie;

public final class DictionaryDataBuilder<T> {

    private final MapTrie<Object> temp = new MapPatriciaTrie<>();

    /**
     * Builder factory for POJO class DictionaryData.
     */
    public DictionaryDataBuilder() {
    }

    /**
     * build DictionaryData POJO.
     * @return DictionaryData immutable object.
     */
    public DictionaryData<T> build() throws MDException {
        return new DictionaryData<>(temp);
    }

    /**
     * Insert a key=value pair into the data store. Unicode normalization is
     * performed on the key. The value is stored both for the key and its
     * lowercase version, if the latter differs.
     *
     * @param key
     *            The key
     * @param value
     *            The value
     */
    public void add(final String key, final T value) {
        doAdd(key, value);
        String lowerKey = key.toLowerCase();
        if (!key.equals(lowerKey)) {
            doAdd(lowerKey, value);
        }
    }

    /**
     * Do the actual storing of the value. Most values are going to be singular,
     * but dictionaries may store multiple definitions for the same key, so in
     * that case we store the values in an array.
     *
     * @param key
     * @param value
     */
    private void doAdd(final String key, final T value) {
        Object stored = temp.get(key);
        if (stored == null) {
            temp.insert(key, value);
        } else {
            if (stored instanceof Object[]) {
                stored = extendArray((Object[]) stored, value);
            } else {
                stored = new Object[] {stored, value};
            }
            temp.put(key, stored);
        }
    }

    /**
     * Return the given array with the given value appended to it.
     *
     * @param array
     * @param value
     * @return
     */
    Object[] extendArray(final Object[] array, final Object value) {
        Object[] newArray = new Object[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[newArray.length - 1] = value;
        return newArray;
    }

}
