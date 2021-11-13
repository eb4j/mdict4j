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

import java.util.ArrayList;
import java.util.List;

public class DictionaryIndex {
    final List<String> keyNameList = new ArrayList<>();
    public final List<String> firstLastKeys = new ArrayList<>();

    int keyNumBlocks;
    private long[] keyCompSize;
    private long[] keyDecompSize;
    long[] numEntries;

    private long[] recordCompSize;
    private long[] recordDecompSize;
    private long[] recordOffsets;
    private long recordOffset;

    public DictionaryIndex() {
    }

    void initKeyNum(final int keyNumBlocks) {
        this.keyNumBlocks = keyNumBlocks;
        keyCompSize = new long[(int) keyNumBlocks];
        keyDecompSize = new long[(int) keyNumBlocks];
        numEntries = new long[(int) keyNumBlocks];
    }

    void initRecordNum(final int recordNumBlocks, final long offsetBase) {
        recordCompSize = new long[(int) recordNumBlocks];
        recordDecompSize = new long[(int) recordNumBlocks];
        recordOffsets = new long[(int) recordNumBlocks];
        recordOffset = offsetBase;
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

    public void setKeySizes(final int i, final long compSize, final long decompSize) {
        keyCompSize[i] = compSize;
        keyDecompSize[i] = decompSize;
    }

    public long getKeyDecompSize(final int i) {
        return keyDecompSize[i];
    }

    public long getKeyCompSize(final int i) {
        return keyCompSize[i];
    }
}
