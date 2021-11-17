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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

public class Dictionary {
    private final MDInputStream mdInputStream;
    private final DictionaryData<Object> dictionaryData;
    private final RecordIndex recordIndex;

    private final String title;
    private final Charset encoding;
    private final String creationDate;
    private final String format;
    private final String description;
    private final String styleSheet;
    private final int encrypted;
    private final boolean keyCaseSensitive;

    public Dictionary(final DictionaryInfo info, final DictionaryData<Object> index, final RecordIndex recordIndex,
                      final MDInputStream mdInputStream) {
        dictionaryData = index;
        this.recordIndex = recordIndex;
        this.mdInputStream = mdInputStream;
        //
        title = info.getTitle();
        encoding = Charset.forName(info.getEncoding());
        creationDate = info.getCreationDate();
        format = info.getFormat();
        description = info.getDescription();
        styleSheet = info.getStyleSheet();
        encrypted = Integer.parseInt(info.getEncrypted());
        keyCaseSensitive = "true".equalsIgnoreCase(info.getKeyCaseSensitive());
    }

    public Charset getEncoding() {
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

    public boolean isHeaderEncrypted() {
        return (encrypted & 0x01) > 0;
    }

    public boolean isIndexEncrypted() {
        return (encrypted & 0x02) > 0;
    }

    public boolean isKeyCaseSensitive() {
        return keyCaseSensitive;
    }

    public String getStyleSheet() {
        return styleSheet;
    }

    public List<Map.Entry<String, Object>> getEntries(final String word) {
        return dictionaryData.lookUpPredictive(word);
    }

    public String getText(final Long offset) throws MDException {
        String result = null;
        // calculate block index and seek it
        int index = recordIndex.searchOffsetIndex(offset);
        long skipSize = offset - recordIndex.getRecordOffsetDecomp(index);
        try {
            mdInputStream.seek(recordIndex.getCompOffset(index));
        } catch (IOException e) {
            throw new MDException("IO error.", e);
        }
        long compSize = recordIndex.getRecordCompSize(index);
        long decompSize = recordIndex.getRecordDecompSize(index);
        try (MDBlockInputStream decompressedStream = Utils.decompress(mdInputStream, compSize, decompSize, false)) {
            long pos = decompressedStream.skip(skipSize);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(decompressedStream, encoding),
                    (int) decompSize)) {
                result = bufferedReader.readLine();
                return result;
            }
        } catch (DataFormatException | IOException e) {
            throw new MDException("data decompression error.", e);
        }
    }

    public static Dictionary loadData(final String mdxFile) throws MDException {
        DictionaryInfo info;
        DictionaryData<Object> index;
        RecordIndex record;
        File file = new File(mdxFile);
        if (!file.isFile()) {
            throw new MDException("Target file is not MDict file.");
        }
        MDInputStream mdInputStream;
        try {
            mdInputStream = new MDInputStream(mdxFile);
            MdxParser parser = new MdxParser(mdInputStream);
            info = parser.parseHeader();
            index = parser.parseIndex();
            record = parser.parseRecordBlock();
        } catch (IOException | DataFormatException e) {
            throw new MDException("Dictionary data read error", e);
        }
        return new Dictionary(info, index, record, mdInputStream);
    }
}
