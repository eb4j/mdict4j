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
import io.github.eb4j.mdict.io.LzoBlockInputStream;
import io.github.eb4j.mdict.io.MDBlockInputStream;
import io.github.eb4j.mdict.io.MDInputStream;
import io.github.eb4j.mdict.io.ZlibBlockInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;

public class MdxParser {
    private MdxParser() {
    }

    /**
     * Builder method to parse Header.
     * @param inputStream RandomAccessStream of MDX file.
     * @return DictionaryInfo object.
     * @throws MDException when read or parse error.
     */
    public static DictionaryInfo parseHeader(final MDInputStream inputStream) throws MDException {
        byte[] word = new byte[4];
        DictionaryInfo dictionaryInfo;
        try {
            // Header
            // LEN + UTF-16LE string + checksum
            inputStream.seek(0);
            inputStream.readFully(word);
            int headerStrLength = byteArrayToInt(word);
            byte[] headerStringBytes = new byte[headerStrLength];
            inputStream.readFully(headerStringBytes);
            Adler32 adler32 = new Adler32();
            adler32.update(headerStringBytes);
            inputStream.readFully(word);
            long checksum = Integer.toUnsignedLong(byteArrayToInt(word, ByteOrder.LITTLE_ENDIAN));
            if (checksum != adler32.getValue()) {
                throw new MDException("checksum error.");
            }
            String headerString = new String(headerStringBytes, StandardCharsets.UTF_16LE);
            ObjectMapper mapper = new XmlMapper();
            dictionaryInfo = mapper.readValue(headerString, DictionaryInfo.class);
            String version = dictionaryInfo.getRequiredEngineVersion();
            if (!version.equals("2.0")) {
                throw new MDException("Unsupported dictionary version.");
            }
            long keyBlockPosition = inputStream.tell();
            dictionaryInfo.setKeyBlockPosition(keyBlockPosition);
        } catch (IOException e) {
            throw new MDException("Parse header error", e);
        }
        return dictionaryInfo;
    }

    /**
     * Builder method of dictionary index.
     * @param mdInputStream RandomAccessFile of MDX file.
     * @return DictionaryIndex object.
     * @throws MDException when read or parse error.
     */
    public static DictionaryIndex parseIndex(final MDInputStream mdInputStream, final DictionaryInfo info)
            throws MDException, IOException, DataFormatException {
        DictionaryIndex dictionaryIndex = new DictionaryIndex();
        Charset encoding = Charset.forName(info.getEncoding());
        mdInputStream.seek(info.getKeyBlockPosition());
        parseKeyBlockSizes(mdInputStream, dictionaryIndex);
        parseKeyBlockInfo(mdInputStream, encoding, dictionaryIndex);
        parseKeyBlocks(mdInputStream, encoding, dictionaryIndex);
        parseRecordBlock(mdInputStream, encoding, dictionaryIndex);
        return dictionaryIndex;
    }

    private static void parseKeyBlockSizes(final MDInputStream ras, DictionaryIndex sizes)
            throws MDException, IOException {
        // Key block
        // number of Key Blocks + number of entries
        // + decompression size + bytes of keyBlockInfo + bytes of KeyBlocks
        // + checksum
        byte[] word = new byte[4];
        byte[] dWord = new byte[8];
        Adler32 adler32 = new Adler32();
        ras.readFully(dWord);
        adler32.update(dWord);
        sizes.keyNumBlocks = byteArrayToLong(dWord);
        ras.readFully(dWord);
        adler32.update(dWord);
        sizes.keySum = byteArrayToLong(dWord);
        ras.readFully(dWord);
        adler32.update(dWord);
        sizes.keyIndexDecompLen = byteArrayToLong(dWord);
        ras.readFully(dWord);
        adler32.update(dWord);
        sizes.keyIndexCompLen = byteArrayToLong(dWord);
        ras.readFully(dWord);
        adler32.update(dWord);
        sizes.keyBlocksLen = byteArrayToLong(dWord);
        ras.readFully(word);
        int checksum = byteArrayToInt(word);
        if (adler32.getValue() != checksum) {
            throw new MDException("checksum error.");
        }
    }

    private static void parseKeyBlockInfo(final MDInputStream mdInputStream, final Charset encoding,
                                          final DictionaryIndex keyBlockValues) throws MDException, IOException, DataFormatException {
        // Key Block info
        //
        // +----------------------------------------------------+
        // | 02 00 00 00 | Adler32 | zlib compressed data       |
        // +----------------------------------------------------+
        //  decompressed data is
        // +----------------------------------------------------+
        // | numEntries | size | entry | size | entry |.........|
        // +----------------------------------------------------+
        //  each entry is
        // +-------------------------------------+
        // | compressed size | decompressed size |
        // +-------------------------------------+
        //
        byte[] hWord = new byte[2];
        byte[] dWord = new byte[8];
        MDBlockInputStream indexDs = decompress(mdInputStream, keyBlockValues.keyIndexCompLen, keyBlockValues.keyIndexDecompLen);
        keyBlockValues.compSize = new long[(int) keyBlockValues.keyNumBlocks];
        keyBlockValues.decompSize = new long[(int) keyBlockValues.keyNumBlocks];
        keyBlockValues.numEntries = new long[(int) keyBlockValues.keyNumBlocks];
        for (int i = 0; i < keyBlockValues.keyNumBlocks; i++) {
            indexDs.readFully(dWord);
            keyBlockValues.numEntries[i] = byteArrayToLong(dWord);
            indexDs.readFully(hWord);
            short firstSize = byteArrayToShort(hWord);
            byte[] firstBytes = new byte[firstSize];
            indexDs.readFully(firstBytes);
            String firstWord = new String(firstBytes, encoding);
            keyBlockValues.firstLastKeys.add(firstWord);
            indexDs.skip(1);
            //
            indexDs.readFully(hWord);
            short lastSize = byteArrayToShort(hWord);
            byte[] lastBytes = new byte[lastSize];
            indexDs.readFully(lastBytes);
            String lastWord = new String(lastBytes, encoding);
            keyBlockValues.firstLastKeys.add(lastWord);
            indexDs.skip(1);
            //
            indexDs.readFully(dWord);
            keyBlockValues.compSize[i] = byteArrayToLong(dWord);
            indexDs.readFully(dWord);
            keyBlockValues.decompSize[i] = byteArrayToLong(dWord);
        }
    }

    private static void parseKeyBlocks(final MDInputStream mdInputStream, final Charset encoding,
                                       final DictionaryIndex dictionaryIndex) throws MDException, IOException, DataFormatException {
        // Key blocks
        // - plain format
        // +-----------------------------------------------------------------+
        // | 00 00 00 00 | adler32 | 00 00 ... 00 | key text            | 00 |
        // +-----------------------------------------------------------------+
        // - lzo compress
        // +-----------------------------------------------------------------+
        // | 01 00 00 00 | adler32 | lzo compressed block                    |
        // +-----------------------------------------------------------------+
        // - zlib compress
        // +-----------------------------------------------------------------+
        // | 02 00 00 00 | adler32 | zlib compressed block                   |
        // +-----------------------------------------------------------------+
        //
        // - decompressed key format
        // +--------------------------------------------------+
        // | key id | key text                            |00 |
        // +--------------------------------------------------+
        //            ....
        // +--------------------------------------------------+
        // | key id | key text                            |00 |
        // +--------------------------------------------------+
        //
        byte[] dWord = new byte[8];
        for (int i = 0; i < dictionaryIndex.keyNumBlocks; i++) {
            MDBlockInputStream blockIns = decompress(mdInputStream, dictionaryIndex.compSize[i], dictionaryIndex.decompSize[i]);
            for (int j = 0; j < dictionaryIndex.numEntries[i]; j++) {
                blockIns.readFully(dWord);
                long off = byteArrayToLong(dWord);
                byte[] keytextByte = blockIns.readAll();
                String keytext = new String(keytextByte, encoding);
                String keyName = getKeyName(dictionaryIndex.offsetMap, keytext);
                dictionaryIndex.offsetMap.put(keyName, off);
                dictionaryIndex.keyNameList.add(keyName);
            }
        }
    }

    private static void parseRecordBlock(final MDInputStream mdInputStream, final Charset encoding, DictionaryIndex dictionaryIndex) throws IOException {
        // Record block
        //  number of record blocks + number of entries +
        //  number of bytes of all lists +
        //  number of bytes of all blocks +
        //  index of  each blocks +
        //  record blocks
        byte[] dWord = new byte[8];
        mdInputStream.readFully(dWord);
        long recordNumBlocks = byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        long recordNumEntries = byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        long recordIndexLen = byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        long recordBlockLen = byteArrayToLong(dWord);
        dictionaryIndex.recordCompSize = new long[(int) recordNumBlocks];
        dictionaryIndex.recordDecompSize = new long[(int) recordNumBlocks];
        long offset = mdInputStream.tell();
        for (int i = 0; i < recordNumBlocks; i++) {
            dictionaryIndex.recordMap.put("", offset);
            mdInputStream.readFully(dWord);
            long recordBlockCompSize = byteArrayToLong(dWord);
            dictionaryIndex.recordCompSize[i] = recordBlockCompSize;
            offset += recordBlockCompSize;
            mdInputStream.readFully(dWord);
            long recordBlockDecompSize = byteArrayToLong(dWord);
            dictionaryIndex.recordDecompSize[i] = recordBlockDecompSize;
        }
    }

    private static String getKeyName(final Map<String, Long> offsetMap, final String keyword) {
        String result = keyword;
        if (offsetMap.containsKey(keyword)) {
            for (int k = 1; ; k++) {
                String keywordWithNum = keyword + k;
                if (!offsetMap.containsKey(keywordWithNum)) {
                    result = keywordWithNum;
                    break;
                }
            }
        }
        return result;
    }

    private static MDBlockInputStream decompress(final MDInputStream inputStream, final long compSize,
                                                 final long decompSize) throws IOException, MDException, DataFormatException {
        byte[] word = new byte[4];
        inputStream.readFully(word);
        int flag = byteArrayToInt(word, ByteOrder.LITTLE_ENDIAN);
        inputStream.readFully(word);
        long checksum = byteArrayToInt(word);
        switch(flag) {
            case 0:
                break;
            case 1:
                return new LzoBlockInputStream(inputStream, compSize, decompSize, checksum);
            case 2:
                return new ZlibBlockInputStream(inputStream, compSize, decompSize, checksum);
            default:
                throw new MDException("Unknwn compression level.");
        }
        throw new MDException("Unsupported data.");
    }

    private static long byteArrayToLong(byte[] dWord) {
        ByteBuffer buffer = ByteBuffer.wrap(dWord).order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    private static int byteArrayToInt(byte[] word, final ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.wrap(word).order(byteOrder);
        return buffer.getInt();
    }

    private static int byteArrayToInt(byte[] word) {
        return byteArrayToInt(word, ByteOrder.BIG_ENDIAN);
    }

    private static short byteArrayToShort(byte[] hWord) {
        ByteBuffer buffer = ByteBuffer.wrap(hWord).order(ByteOrder.BIG_ENDIAN);
        return buffer.getShort();
    }
}
