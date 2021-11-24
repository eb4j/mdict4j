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

import java.util.Arrays;

/**
 * POJO of record index.
 */
public final class RecordIndex {
    private final long[] recordCompSize;
    private final long[] recordDecompSize;
    private final long[] recordOffsetComp;
    private final long[] recordOffsetDecomp;
    private final long recordNumEntries;

    public RecordIndex(final long[] recordCompSize, final long[] recordDecompSize, final long[] recordOffsetComp,
                       final long[] recordOffsetDecomp, final long recordNumEntries) {
        this.recordCompSize = Arrays.copyOf(recordCompSize, recordCompSize.length);
        this.recordDecompSize = Arrays.copyOf(recordDecompSize, recordDecompSize.length);
        this.recordOffsetComp = Arrays.copyOf(recordOffsetComp, recordOffsetComp.length);
        this.recordOffsetDecomp = Arrays.copyOf(recordOffsetDecomp, recordOffsetDecomp.length);
        this.recordNumEntries = recordNumEntries;
    }

    public long getRecordCompSize(final int index) {
        return recordCompSize[index];
    }

    public long getRecordDecompSize(final int index) {
        return recordDecompSize[index];
    }

    public long getRecordOffsetDecomp(final int index) {
        return recordOffsetDecomp[index];
    }

    public long getCompOffset(final int index) {
        return recordOffsetComp[index];
    }

    public long getRecordNumEntries() {
        return recordNumEntries;
    }

    public int searchOffsetIndex(final long off) throws MDException {
        int start = 0;
        int end = recordOffsetDecomp.length - 2;
        // check range of offset
        if (0 > off || off > recordOffsetDecomp[end + 1]) {
            throw new MDException("Wrong search index!!");
        }
        do {
            int middle = (start + end) / 2;
            if (middle == recordOffsetDecomp.length - 1) {
                return middle;
            }
            long begin = recordOffsetDecomp[middle];
            long rend = recordOffsetDecomp[middle + 1];
            if (begin <= off && off < rend) {
                return middle;
            } else if (off < begin) {
                end = middle;
            } else {
                start = middle;
            }
        } while (start <= end);
        return -1;
    }

}
