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

import java.util.Objects;

public class MDictEntry {
    private final long blockNumber;
    private final long entryIndex;

    public MDictEntry(final long blockNumber, final long entryIndex) {
        this.blockNumber = blockNumber;
        this.entryIndex = entryIndex;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public long getEntryIndex() {
        return entryIndex;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MDictEntry that = (MDictEntry) o;
        return blockNumber == that.blockNumber && entryIndex == that.entryIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockNumber, entryIndex);
    }
}
