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

import java.util.List;

class DictionaryIndex {
    private final List<String> keyNameList;
    /*
      Size of these lists is keyNumBlocks.
     */
    private final List<String> firstKeys;
    private final List<String> lastKeys;
    private final int[] numEntries;
    private final RecordIndex recordIndex;

    private final int numBlocks;

    DictionaryIndex(final List<String> keyNameList, final List<String> firstKeys, final List<String> lastKeys,
                    final int[] numEntries, final RecordIndex recordIndex, final int numBlocks) {
        this.keyNameList = keyNameList;
        this.firstKeys = firstKeys;
        this.lastKeys = lastKeys;
        this.numEntries = numEntries;
        this.numBlocks = numBlocks;
        this.recordIndex = recordIndex;
    }

    /**
     * Number of keys that dictionary has.
     * @return
     */
    long keySize() {
        return keyNameList.size();
    }

    public int getNumBlocks() {
        return numBlocks;
    }

    String getKeyName(final int index) {
        return keyNameList.get(index);
    }

    long getRecordOffset(final int index) {
        return recordIndex.getRecordOffset(index);
    }

    long getRecordCompSize(final int index) {
        return recordIndex.getRecordCompSize(index);
    }

    long getRecordDecompSize(final int index) {
        return recordIndex.getRecordDecompSize(index);
    }

    int getNumEntries(final int i) {
        return numEntries[i];
    }

    public long getRecordNumEntries() {
        return recordIndex.getRecordNumEntries();
    }

    public String getFirstKey(final int index) {
        return firstKeys.get(index);
    }

    public String getLastKey(final int index) {
        return lastKeys.get(index);
    }

}
