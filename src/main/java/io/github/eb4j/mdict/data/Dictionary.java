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
import io.github.eb4j.mdict.io.MDBlockInputStream;
import io.github.eb4j.mdict.io.MDInputStream;
import io.github.eb4j.mdict.io.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

public class Dictionary {
    private final MDInputStream mdInputStream;
    private final DictionaryData<MDictEntry> dictionaryData;
    private final DictionaryIndex dictionaryIndex;
    private final String title;
    private final String encoding;
    private final String creationDate;
    private final String format;
    private final String description;
    private final String styleSheet;
    private final boolean encrypted;
    private final boolean keyCaseSensitive;

    public Dictionary(final DictionaryInfo info, final DictionaryIndex index, final DictionaryData<MDictEntry> data,
                      final MDInputStream mdInputStream) {
        dictionaryData = data;
        dictionaryIndex = index;
        title = info.getTitle();
        encoding = info.getEncoding();
        creationDate = info.getCreationDate();
        format = info.getFormat();
        description = info.getDescription();
        styleSheet = info.getStyleSheet();
        encrypted = "true".equalsIgnoreCase(info.getEncrypted());
        keyCaseSensitive = "true".equalsIgnoreCase(info.getKeyCaseSensitive());
        this.mdInputStream = mdInputStream;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getTitle() {
        return title;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public boolean isKeyCaseSensitive() {
        return keyCaseSensitive;
    }

    public String getStyleSheet() {
        return styleSheet;
    }

    public List<Map.Entry<String, MDictEntry>> getEntries(final String word) {
        return dictionaryData.lookUpPredictive(word);
    }

    public String getText(final MDictEntry mDictEntry) throws MDException {
        String result = null;
        int blockNumber = (int) mDictEntry.getBlockNumber();
        long offset = dictionaryIndex.getRecordOffset(blockNumber);
        try {
            mdInputStream.seek(offset);
        } catch (IOException e) {
            throw new MDException("IO error.", e);
        }
        long entryIndex = mDictEntry.getEntryIndex();
        long compSize = dictionaryIndex.getRecordCompSize(blockNumber);
        long decompSize = dictionaryIndex.getRecordDecompSize(blockNumber);
        try (MDBlockInputStream decompressedStream = Utils.decompress(mdInputStream, compSize, decompSize, false);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(decompressedStream),
                    (int) decompSize)) {
            int i = 0;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (i == entryIndex) {
                    result = line.substring(0, line.length());
                }
                i++;
            }
        } catch (DataFormatException | IOException e) {
            throw new MDException("data decompression error.", e);
        }
        return result;
    }

    public static Dictionary loadData(final String mdxFile) throws MDException {
        DictionaryInfo info;
        DictionaryIndex index;
        File file = new File(mdxFile);
        if (!file.isFile()) {
            throw new MDException("Target file is not MDict file.");
        }
        DictionaryDataBuilder<MDictEntry> newDataBuilder = new DictionaryDataBuilder<>();
        MDInputStream mdInputStream;
        try {
            mdInputStream = new MDInputStream(mdxFile);
            info = MdxParser.parseHeader(mdInputStream);
            index = MdxParser.parseIndex(mdInputStream, info);
            int keySize = (int) index.keySize();
            int recordEntries = (int) index.getRecordNumEntries();
            if (keySize != recordEntries) {
                throw new MDException("Different number of keys and records entries.");
            }
            int numBlocks = index.getNumBlocks();
            int i = 0;
            for (int j = 0; j < numBlocks; j++) {
                int numEntries = index.getNumEntries(j);
                for (int k = 0; k < numEntries; k++) {
                    String key = index.getKeyName(i);
                    MDictEntry entry = new MDictEntry(j, k);
                    newDataBuilder.add(key, entry);
                    i++;
                }
            }
        } catch (IOException | DataFormatException e) {
            throw new MDException("Dictionary data read error", e);
        }
        return new Dictionary(info, index, newDataBuilder.build(), mdInputStream);
    }
}
