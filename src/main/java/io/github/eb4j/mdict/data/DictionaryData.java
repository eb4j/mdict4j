/*
 * MD4J, a parser library for MDict format.
 * Copyright (C) 2021 Hiroshi Miura.
 *               2015-2016 Hiroshi Miura, Aaron Madlon-Kay
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

import org.trie4j.MapTrie;
import org.trie4j.doublearray.MapDoubleArray;
import org.trie4j.patricia.MapPatriciaTrie;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

/**
 * A class that encapsulates the storage and retrieval of string-keyed data.
 * Usage:
 * <ol>
 * <li>Instantiate and insert data with {@link #add(String, Object)}
 * <li>Call {@link #done()} when done adding data (required!)
 * <li>Retrieve data with {@link #lookUp(String)}
 * </ol>
 *
 * @author Aaron Madlon-Kay
 *
 * @param <T>
 *            The type of data stored
 */
public class DictionaryData<T> {

    private MapDoubleArray<Object> data;
    private MapTrie<Object> temp;

    /**
     * POJO class to hold dictionary data.
     */
    public DictionaryData() {
        this.temp = new MapPatriciaTrie<>();
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
        // key = normalizeUnicode(key);
        doAdd(key, value);
        String lowerKey = key.toLowerCase();
        // String lowerKey = key.toLowerCase(language.getLocale());
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

    /**
     * Finalize the data store. This is <strong>required</strong> to be called
     * before any lookups can be performed.
     */
    public void done() {
        data = new MapDoubleArray<>(temp);
        temp = null;
    }

    /**
     * Look up the given word.
     *
     * @param word
     *            The word to look up
     * @return A list of stored objects matching the given word
     * @throws IllegalStateException
     *             If {@link #done()} has not yet been called
     */
    public List<Entry<String, T>> lookUp(final String word) throws IllegalStateException {
        return doLookUpWithLowerCase(word, false);
    }

    /**
     * Look up the given word using predictive completion; e.g. "term" will
     * match "terminology" (and "terminal", etc.).
     *
     * @param word
     *            The word to look up
     * @return A list of stored objects matching the given word
     * @throws IllegalStateException
     *             If {@link #done()} has not yet been called
     */
    public List<Entry<String, T>> lookUpPredictive(final String word) throws IllegalStateException {
        return doLookUpWithLowerCase(word, true);
    }

    private List<Entry<String, T>> doLookUpWithLowerCase(final String word, final boolean predictive) {
        List<Entry<String, T>> result = doLookUp(word, predictive);
        if (result.isEmpty()) {
            String lowerWord = word.toLowerCase();
            result = doLookUp(lowerWord, predictive);
        }
        return result;
    }

    private List<Entry<String, T>> doLookUp(final String word, final boolean predictive) throws IllegalStateException {
        if (data == null) {
            throw new IllegalStateException(
                    "Object has not been finalized! You must call done() before doing any lookups.");
        }
        List<Entry<String, T>> result = new ArrayList<>();
        if (predictive) {
            data.predictiveSearch(word).forEach(w -> get(w, data.get(w), result));
        } else {
            get(word, data.get(word), result);
        }

        return result;
    }

    /**
     * Unpack the given stored object (singular, or array) into the given
     * collection.
     *
     * @param key
     * @param value
     * @param into
     */
    @SuppressWarnings("unchecked")
    private <U> void get(final U key, final Object value, final Collection<Entry<U, T>> into) {
        if (value == null) {
            return;
        }
        if (value instanceof Object[]) {
            for (Object o : (Object[]) value) {
                into.add(new AbstractMap.SimpleImmutableEntry<>(key, (T) o));
            }
        } else {
            into.add(new AbstractMap.SimpleImmutableEntry<>(key, (T) value));
        }
    }

    /**
     * Get the number of stored keys. Returns <code>-1</code> if {@link #done()}
     * has not yet been called.
     *
     * @return The number of stored keys
     */
    public int size() {
        if (data == null) {
            return -1;
        }
        return data.size();
    }
}
