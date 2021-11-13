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

/**
 * POJO of record index.
 */
public final class RecordIndex {
    private final long[] recordCompSize;
    private final long[] recordDecompSize;
    private final long[] recordOffsets;
    private final long recordNumEntries;

    public RecordIndex(final long recordNumEntries, final int recordNumBlocks) {
        this.recordNumEntries = recordNumEntries;
        recordCompSize = new long[recordNumBlocks];
        recordDecompSize = new long[recordNumBlocks];
        recordOffsets = new long[recordNumBlocks];
    }

    public long getRecordCompSize(final int index) {
        return recordCompSize[index];
    }

    public long getRecordDecompSize(final int index) {
        return recordDecompSize[index];
    }

    public long getRecordOffset(final int index) {
        return recordOffsets[index];
    }

    public long getRecordNumEntries() {
        return recordNumEntries;
    }

    public void setRecordCompSize(final int i, final long recordBlockCompSize) {
        recordCompSize[i] = recordBlockCompSize;
    }

    public void setRecordDecompSize(final int i, final long recordBlockDecompSize) {
        recordDecompSize[i] = recordBlockDecompSize;
    }

    public void setRecordOffsets(final int i, final long offset) {
        recordOffsets[i] = offset;
    }
}
