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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DictionaryIndex {
    public final List<String> keyNameList = new ArrayList<>();
    public final List<String> firstLastKeys = new ArrayList<>();
    public final Map<String, Long> offsetMap = new HashMap<>();
    public final Map<String, Long> recordMap = new LinkedHashMap<>();

    long keyNumBlocks;
    long keySum;
    long keyIndexDecompLen;
    long keyIndexCompLen;
    long keyBlocksLen;
    long[] compSize;
    long[] decompSize;
    long[] numEntries;
    long[] recordCompSize;
    long[] recordDecompSize;

    public DictionaryIndex() {
    }
}
