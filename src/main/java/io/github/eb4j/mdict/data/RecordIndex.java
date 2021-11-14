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

    public RecordIndex(final long[] recordCompSize, final long[] recordDecompSize, final long[] recordOffsets, final long recordNumEntries) {
        this.recordCompSize = recordCompSize;
        this.recordDecompSize = recordDecompSize;
        this.recordOffsets = recordOffsets;
        this.recordNumEntries = recordNumEntries;
    }

    public long getRecordCompSize(final int index) {
        return recordCompSize[index];
    }

    public long getRecordDecompSize(final int index) {
        return recordDecompSize[index];
    }

    public long[] getRecordOffsets() {
        return recordOffsets;
    }

    public long getRecordNumEntries() {
        return recordNumEntries;
    }
}
