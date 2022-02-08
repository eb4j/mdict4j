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

import io.github.eb4j.mdict.io.MDFileInputStream;
import io.github.eb4j.mdict.io.MDInputStream;
import io.github.eb4j.mdict.io.MDictUtils;
import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

public class MDictDictionary {
    private final MDFileInputStream mdInputStream;
    private final DictionaryData<Object> dictionaryData;
    private final RecordIndex recordIndex;

    private final String mdxVersion;
    private final String title;
    private final Charset encoding;
    private final String creationDate;
    private final String format;
    private final String description;
    private final String styleSheet;
    private final String encrypted;
    private final String keyCaseSensitive;
    private final boolean mdx;

    public MDictDictionary(final MDictDictionaryInfo info, final DictionaryData<Object> index,
                           final RecordIndex recordIndex, final MDFileInputStream mdInputStream,
                           final boolean mdx) {
        dictionaryData = index;
        this.recordIndex = recordIndex;
        this.mdInputStream = mdInputStream;
        //
        mdxVersion = info.getRequiredEngineVersion();
        title = info.getTitle();
        String encodingName = info.getEncoding();
        if (encodingName.equalsIgnoreCase("UTF-16")) {
            encodingName = "UTF-16LE";
        }
        encoding = Charset.forName(encodingName);
        creationDate = info.getCreationDate();
        format = info.getFormat();
        description = info.getDescription();
        styleSheet = info.getStyleSheet();
        encrypted = info.getEncrypted();
        keyCaseSensitive = info.getKeyCaseSensitive();
        this.mdx = mdx;
    }

    public String getMdxVersion() {
        return mdxVersion;
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
        return (Integer.parseInt(encrypted) & 0x01) > 0;
    }

    public boolean isIndexEncrypted() {
        if ("Yes".equals(encrypted)) {
            return true;
        }
        if ("No".equals(encrypted)) {
            return false;
        }
        return (Integer.parseInt(encrypted) & 0x02) > 0;
    }

    public boolean isMdx() {
        return mdx;
    }

    public boolean isKeyCaseSensitive() {
        return "Yes".equals(keyCaseSensitive) || "true".equals(keyCaseSensitive);
    }

    public String getStyleSheet() {
        return styleSheet;
    }

    public List<Map.Entry<String, String>> readArticlesPredictive(String word) throws MDException {
        if (!mdx) {
            throw new MDException("Can not retrieve text data from MDD file.");
        }
        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : getEntriesPredictive(word)) {
            addEntry(result, entry);
        }
        return result;
    }

    public List<Map.Entry<String, String>> readArticles(final String word) throws MDException {
        if (!mdx) {
            throw new MDException("Can not retrieve text data from MDD file.");
        }
        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : getEntries(word)) {
            addEntry(result, entry);
        }
        return result;
    }

    public byte[] readData(final String path) throws MDException {
        for (Map.Entry<String, Object> entry: getEntries(path)) {
            if (entry.getKey().equals(path)) {
                Object value = entry.getValue();
                if (value instanceof Long) {
                    return getData((Long) value);
                }
            }
        }
        return null;
    }

    public List<Map.Entry<String, Object>> getEntries(final String word) {
        return dictionaryData.lookUp(word);
    }

    public List<Map.Entry<String, Object>> getEntriesPredictive(final String word) {
        return dictionaryData.lookUpPredictive(word);
    }

    public byte[] getData(final Long offset) throws MDException {
        int index = recordIndex.searchOffsetIndex(offset);
        int pos = (int) (offset - recordIndex.getRecordOffsetDecomp(index));
        try {
            mdInputStream.seek(recordIndex.getCompOffset(index));
        } catch (IOException e) {
            throw new MDException("IO error.", e);
        }
        long compSize = recordIndex.getRecordCompSize(index);
        long decompSize = recordIndex.getRecordDecompSize(index);
        int dataSize;
        if (recordIndex.getRecordNumEntries() -1 > index) {
            dataSize = (int) (recordIndex.getRecordOffsetDecomp(index + 1) - offset);
        } else {
            dataSize = (int) (decompSize - pos);
        }
        try {
            byte[] result = new byte[dataSize];
            byte[] buf = MDictUtils.decompressBuf(mdInputStream, compSize, decompSize, false);
            System.arraycopy(buf, pos, result, 0, dataSize);
            return result;
        } catch (DataFormatException | IOException e) {
            throw new MDException("Decompressed data seems incorrect.");
        }
    }

    public String getText(final Long offset) throws MDException {
        if (!mdx) {
            throw new MDException("Can not retrieve text data from MDD file.");
        }
        String result;
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
        try (MDInputStream decompressedStream = MDictUtils.decompress(mdInputStream, compSize, decompSize, false)) {
            long moved = decompressedStream.skip(skipSize);
            if (moved != skipSize) {
                throw new MDException("Decompressed data seems incorrect.");
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(decompressedStream, encoding),
                    (int) decompSize)) {
                result = bufferedReader.readLine();
                return result;
            }
        } catch (DataFormatException | IOException e) {
            throw new MDException("data decompression error.", e);
        }
    }

    private void addEntry(final List<Map.Entry<String, String>> result, final Map.Entry<String, Object> entry)
            throws MDException {
        if (entry.getValue() instanceof Long) {
            result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), getText((Long) entry.getValue())));
        } else {
            Long[] values = (Long[]) entry.getValue();
            for (Long value : values) {
                result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), getText(value)));
            }
        }
    }

    private static String getBaseName(final String path) {
        String f = path;
        if (f.endsWith(".mdx")) {
            f = f.substring(0, f.length() - ".mdx".length());
        }
        return f;
    }

    public static MDictDictionary loadDicitonary(final String mdxFile) throws MDException {
        File file = new File(mdxFile);
        if (!file.isFile()) {
            throw new MDException("Target file is not MDict file.");
        }
        byte[] keyword = loadDictionaryKey(mdxFile);
        MDFileInputStream mdxInputStream;
        MDictDictionaryInfo info;
        DictionaryData<Object> index;
        RecordIndex record;
        try {
            mdxInputStream = new MDFileInputStream(mdxFile);
            MDictParser parser = MDictParser.createMDXParser(mdxInputStream);
            info = parser.parseHeader();
            index = parser.parseIndex(keyword);
            record = parser.parseRecordBlock();
        } catch (IOException | DataFormatException e) {
            throw new MDException("Dictionary data read error", e);
        }
        return new MDictDictionary(info, index, record, mdxInputStream, true);
    }

    public static MDictDictionary loadDictionaryData(final String mdxFile) throws MDException, IOException {
        File file = new File(mdxFile);
        if (!file.isFile()) {
            throw new MDException("Target file is not MDict file.");
        }
        String dictName = getBaseName(mdxFile);
        byte[] keyword = loadDictionaryKey(mdxFile);
        File mddFile = new File(dictName + ".mdd");
        MDFileInputStream mddInputStream;
        MDictDictionaryInfo info;
        DictionaryData<Object> index;
        RecordIndex record;
        try {
            mddInputStream = new MDFileInputStream(mddFile.getAbsolutePath());
            MDictParser parser = MDictParser.createMDDParser(mddInputStream);
            info = parser.parseHeader();
            // force encoding to UTF-16
            info.setEncoding("UTF-16LE");
            index = parser.parseIndex(keyword);
            record = parser.parseRecordBlock();
        } catch (DataFormatException e) {
            throw new MDException("Dictionary data read error", e);
        }
        return new MDictDictionary(info, index, record, mddInputStream, false);
    }

    /**
     * parse dictionary.key file and return 128-bit regcode.
     * @param mdxFile dictionary file path.
     * @return byte[] password data
     */
    private static byte[] loadDictionaryKey(final String mdxFile) {
        String dictName = getBaseName(mdxFile);
        File keyFile = new File(dictName + ".key");
        if (keyFile.canRead()) {
            try {
                byte[] keydata = new byte[16];
                if (keyFile.isFile() && keyFile.canRead()) {
                    try (Stream<String> lines = Files.lines(keyFile.toPath())) {
                        String first = lines.findFirst().orElse(null);
                        if (first != null) {
                            first = first.substring(0, 32);
                            byte[] temp = Hex.decode(first);
                            System.arraycopy(temp, 0, keydata, 0, 16);
                        }
                    }
                }
                return keydata;
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
