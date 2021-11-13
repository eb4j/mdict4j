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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.eb4j.mdict.MDException;
import io.github.eb4j.mdict.io.MDBlockInputStream;
import io.github.eb4j.mdict.io.MDInputStream;
import io.github.eb4j.mdict.io.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;

class MdxParser {

    private final MDInputStream mdInputStream;
    private DictionaryInfo dictionaryInfo;
    private long keyNumBlocks;
    private long keySum;
    private long keyIndexDecompLen = 0;  // used only when v2
    private long keyIndexCompLen;
    private long keyBlocksLen;

    private int[] numEntries;

    private final List<String> firstKeys = new ArrayList<>();
    private final List<String> lastKeys = new ArrayList<>();
    private final List<String> keyNameList = new ArrayList<>();

    MdxParser(final MDInputStream inputStream) {
        mdInputStream = inputStream;
    }

    /**
     * Builder method to parse Header.
     *
     * @return DictionaryInfo object.
     * @throws MDException when read or parse error.
     */
    DictionaryInfo parseHeader() throws MDException {
        byte[] word = new byte[4];
        try {
            // Header
            // LEN + UTF-16LE string + checksum
            mdInputStream.seek(0);
            mdInputStream.readFully(word);
            int headerStrLength = Utils.byteArrayToInt(word);
            byte[] headerStringBytes = new byte[headerStrLength];
            mdInputStream.readFully(headerStringBytes);
            Adler32 adler32 = new Adler32();
            adler32.update(headerStringBytes);
            mdInputStream.readFully(word);
            long checksum = Integer.toUnsignedLong(Utils.byteArrayToInt(word, ByteOrder.LITTLE_ENDIAN));
            if (checksum != adler32.getValue()) {
                throw new MDException("checksum error.");
            }
            String headerString = new String(headerStringBytes, StandardCharsets.UTF_16LE);
            ObjectMapper mapper = new XmlMapper();
            dictionaryInfo = mapper.readValue(headerString, DictionaryInfo.class);
            long keyBlockPosition = mdInputStream.tell();
            dictionaryInfo.setKeyBlockPosition(keyBlockPosition);
        } catch (IOException e) {
            throw new MDException("Parse header error", e);
        }
        return dictionaryInfo;
    }

    /**
     * dictionary index parser.
     *
     * @return DictionaryIndex object.
     * @throws MDException when read or parse error.
     */
    DictionaryIndex parseIndex()
            throws MDException, IOException, DataFormatException {
        Charset encoding = Charset.forName(dictionaryInfo.getEncoding());
        String requiredVersion = dictionaryInfo.getRequiredEngineVersion();
        boolean v2 = !requiredVersion.startsWith("1");
        if (dictionaryInfo.getEncrypted().equals("3")) {
            throw new MDException("Unknown encryption algorithm found.");
        }
        boolean encrypted = "2".equals(dictionaryInfo.getEncrypted());
        mdInputStream.seek(dictionaryInfo.getKeyBlockPosition());
        byte[] word = new byte[4];
        if (v2) {
            Adler32 adler32 = new Adler32();
            keyNumBlocks = Utils.readLong(mdInputStream, adler32);
            keySum = Utils.readLong(mdInputStream, adler32);
            keyIndexDecompLen = Utils.readLong(mdInputStream, adler32);
            keyIndexCompLen = Utils.readLong(mdInputStream, adler32);
            keyBlocksLen = Utils.readLong(mdInputStream, adler32);
            mdInputStream.readFully(word);
            int checksum = Utils.byteArrayToInt(word);
            if (adler32.getValue() != checksum) {
                throw new MDException("checksum error.");
            }
        } else {
            keyNumBlocks = Utils.readLong(mdInputStream);
            keySum = Utils.readLong(mdInputStream);
            keyIndexCompLen = Utils.readLong(mdInputStream);
            keyBlocksLen = Utils.readLong(mdInputStream);
        }
        parseKeyBlock(v2, encoding, encrypted);
        return parseRecordBlock();
    }

    /**
     * Key Block info
     * <p>
     * +----------------------------------------------------+
     * | 02 00 00 00 | Adler32 | zlib compressed data       |
     * +----------------------------------------------------+
     * decompressed data is
     * +----------------------------------------------------+
     * | numEntries | size | entry | size | entry |.........|
     * +----------------------------------------------------+
     * each entry is
     * +-------------------------------------+
     * | compressed size | decompressed size |
     * +-------------------------------------+
     * Key blocks
     * - plain format
     * +-----------------------------------------------------------------+
     * | 00 00 00 00 | adler32 | 00 00 ... 00 | key text            | 00 |
     * +-----------------------------------------------------------------+
     * - lzo compress
     * +-----------------------------------------------------------------+
     * | 01 00 00 00 | adler32 | lzo compressed block                    |
     * +-----------------------------------------------------------------+
     * - zlib compress
     * +-----------------------------------------------------------------+
     * | 02 00 00 00 | adler32 | zlib compressed block                   |
     * +-----------------------------------------------------------------+
     *
     * - decompressed key format
     * +--------------------------------------------------+
     * | key id | key text                            |00 |
     * +--------------------------------------------------+
     *            ....
     * +--------------------------------------------------+
     * | key id | key text                            |00 |
     * +--------------------------------------------------+
     *
     */
    private void parseKeyBlock(final boolean v2, final Charset encoding, final boolean encrypted)
            throws MDException, IOException, DataFormatException {
        byte[] dWord = new byte[8];
        numEntries = new int[(int) keyNumBlocks];
        long[] keyCompSize = new long[(int) keyNumBlocks];
        long[] keyDecompSize = new long[(int) keyNumBlocks];
        long sum = 0;
        if (v2) {
            MDBlockInputStream indexDs = Utils.decompress(mdInputStream, keyIndexCompLen, keyIndexDecompLen, encrypted);
            for (int i = 0; i < keyNumBlocks; i++) {
                indexDs.readFully(dWord);
                numEntries[i] = (int) Utils.byteArrayToLong(dWord);
                short firstSize = Utils.readShort(indexDs);
                firstKeys.add(Utils.readString(indexDs, firstSize, encoding));
                indexDs.skip(1);
                short lastSize = Utils.readShort(indexDs);
                lastKeys.add(Utils.readString(indexDs, lastSize, encoding));
                indexDs.skip(1);
                keyCompSize[i] = Utils.readLong(indexDs);
                keyDecompSize[i] = Utils.readLong(indexDs);
                sum += keyCompSize[i];
            }
        } else {
            for (int i = 0; i < keyNumBlocks; i++) {
                byte[] b = new byte[1];
                numEntries[i] = (int) Utils.readLong(mdInputStream);
                int firstSize = Utils.readByte(mdInputStream);
                firstKeys.add(Utils.readString(mdInputStream, firstSize, encoding));
                mdInputStream.skip(1);
                int lastSize = Utils.readByte(mdInputStream);
                lastKeys.add(Utils.readString(mdInputStream, lastSize, encoding));
                mdInputStream.skip(1);
                keyCompSize[i] = Utils.readLong(mdInputStream);
                keyDecompSize[i] = Utils.readLong(mdInputStream);
                sum += keyCompSize[i];
            }
        }
        if (sum != keyBlocksLen) {
            throw new MDException("Block size error.");
        }
        for (int i = 0; i < keyNumBlocks; i++) {
            MDBlockInputStream blockIns = Utils.decompress(mdInputStream, keyCompSize[i], keyDecompSize[i], false);
            int b = blockIns.read();
            for (int j = 0; j < numEntries[i]; j++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (int k = 0; k < 8; k++) {
                    b = blockIns.read();
                }
                while (b != 0) {
                    baos.write(b);
                    b = blockIns.read();
                }
                String keytext = new String(baos.toByteArray(), encoding);
                keyNameList.add(getKeyName(keytext));
            }
        }
        if (keyNameList.size() != keySum) {
            throw new MDException("Key sum error.");
        }
    }

    private DictionaryIndex parseRecordBlock() throws MDException, IOException {
        long recordNumBlocks = Utils.readLong(mdInputStream);
        long recordNumEntries = Utils.readLong(mdInputStream);
        long recordIndexLen = Utils.readLong(mdInputStream);
        long recordBlockLen = Utils.readLong(mdInputStream);
        long offset = mdInputStream.tell() + recordIndexLen;
        long endOffset = offset + recordBlockLen;
        RecordIndex recordIndex = new RecordIndex(recordNumEntries, (int) recordNumBlocks);
        long recordBlockCompSize;
        long recordBlockDecompSize;
        for (int i = 0; i < recordNumBlocks; i++) {
            recordBlockCompSize = Utils.readLong(mdInputStream);
            recordBlockDecompSize = Utils.readLong(mdInputStream);
            recordIndex.setRecordCompSize(i, recordBlockCompSize);
            recordIndex.setRecordDecompSize(i, recordBlockDecompSize);
            recordIndex.setRecordOffsets(i, offset);
            offset += recordBlockCompSize;
        }
        if (offset != endOffset) {
            throw new MDException("Wrong index position.");
        }
        return new DictionaryIndex(keyNameList, firstKeys, lastKeys, numEntries, recordIndex, (int) keyNumBlocks);
    }

    private String getKeyName(final String keytext) {
        String result = keytext;
        if (keyNameList.contains(keytext)) {
            for (int k = 1; ; k++) {
                String keywordWithNum = keytext + k;
                if (!keyNameList.contains(keywordWithNum)) {
                    result = keywordWithNum;
                    break;
                }
            }
        }
        return result;
    }
}
