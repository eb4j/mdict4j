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

import java.util.ArrayList;
import java.util.List;

public class DictionaryIndex {
    private final List<String> keyNameList = new ArrayList<>();
    private final List<String> firstKeys = new ArrayList<>();
    private final List<String> lastKeys = new ArrayList<>();

    private int keyNumBlocks;
    private long[] keyCompSize;
    private long[] keyDecompSize;
    private int[] numEntries;

    private long[] recordCompSize;
    private long[] recordDecompSize;
    private long[] recordOffsets;
    private long recordOffset;

    public DictionaryIndex() {
    }

    void initKeyNum(final int keyNumBlocks) {
        this.keyNumBlocks = keyNumBlocks;
        keyCompSize = new long[keyNumBlocks];
        keyDecompSize = new long[keyNumBlocks];
        numEntries = new int[keyNumBlocks];
    }

    void initRecordNum(final int recordNumBlocks, final long offsetBase) {
        recordCompSize = new long[recordNumBlocks];
        recordDecompSize = new long[recordNumBlocks];
        recordOffsets = new long[recordNumBlocks];
        recordOffset = offsetBase;
    }

    public int getKeyNumBlocks() {
        return keyNumBlocks;
    }

    void addFirstLastKeys(final int index, final String firstKey, final String lastKey) {
        firstKeys.add(firstKey);
        lastKeys.add(lastKey);
    }

    void addRecordSizes(final int index, final long compSize, final long decmpSize) {
        recordCompSize[index] = compSize;
        recordDecompSize[index] = decmpSize;
        recordOffsets[index] = recordOffset;
        recordOffset += compSize;
    }

    long endRecordOffset() {
        return recordOffset;
    }

    public long keySize() {
        return keyNameList.size();
    }

    boolean keyContainsName(final String name) {
        return keyNameList.contains(name);
    }

    void setKeyName(final int index, final String name) {
        keyNameList.add(name);
    }

    public String getKeyName(final int index) {
        return keyNameList.get(index);
    }

    long keyNameSize() {
        return keyNameList.size();
    }

    public long getRecordOffset(final int index) {
        return recordOffsets[index];
    }

    public long getRecordCompSize(final int index) {
        return recordCompSize[index];
    }

    public long getRecordDecompSize(final int index) {
        return recordDecompSize[index];
    }

    public void setKeySizes(final int i, final int numEntry, final long compSize, final long decompSize) {
        numEntries[i] = numEntry;
        keyCompSize[i] = compSize;
        keyDecompSize[i] = decompSize;
    }

    public long getKeyDecompSize(final int i) {
        return keyDecompSize[i];
    }

    public long getKeyCompSize(final int i) {
        return keyCompSize[i];
    }

    public int getNumEntries(final int i) {
        return numEntries[i];
    }
}
