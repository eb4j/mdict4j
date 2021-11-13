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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    static DictionaryInfo parseHeader(final MDInputStream inputStream) throws MDException {
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
    static DictionaryIndex parseIndex(final MDInputStream mdInputStream, final DictionaryInfo info)
            throws MDException, IOException, DataFormatException {
        DictionaryIndex dictionaryIndex = new DictionaryIndex();
        Charset encoding = Charset.forName(info.getEncoding());
        mdInputStream.seek(info.getKeyBlockPosition());
        parseKeyBlock(mdInputStream, encoding, dictionaryIndex);
        parseRecordBlock(mdInputStream, dictionaryIndex);
        return dictionaryIndex;
    }

    private static void parseKeyBlock(final MDInputStream mdInputStream, Charset encoding, DictionaryIndex dictionaryIndex)
            throws MDException, IOException, DataFormatException {
        // Key block
        // number of Key Blocks + number of entries
        // + decompression size + bytes of keyBlockInfo + bytes of KeyBlocks
        // + checksum
        byte[] hWord = new byte[2];
        byte[] word = new byte[4];
        byte[] dWord = new byte[8];
        Adler32 adler32 = new Adler32();
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keyNumBlocks = byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keySum = byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keyIndexDecompLen = byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keyIndexCompLen = byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keyBlocksLen = byteArrayToLong(dWord);
        mdInputStream.readFully(word);
        int checksum = byteArrayToInt(word);
        if (adler32.getValue() != checksum) {
            throw new MDException("checksum error.");
        }
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
        MDBlockInputStream indexDs = decompress(mdInputStream, keyIndexCompLen, keyIndexDecompLen);
        dictionaryIndex.initKeyNum((int) keyNumBlocks);
        long sum = 0;
        long compSize;
        long decompSize;
        long numEntries;
        for (int i = 0; i < dictionaryIndex.getKeyNumBlocks(); i++) {
            indexDs.readFully(dWord);
            numEntries = byteArrayToLong(dWord);
            indexDs.readFully(hWord);
            short firstSize = byteArrayToShort(hWord);
            byte[] firstBytes = new byte[firstSize];
            indexDs.readFully(firstBytes);
            String firstWord = new String(firstBytes, encoding);
            indexDs.skip(1);
            //
            indexDs.readFully(hWord);
            short lastSize = byteArrayToShort(hWord);
            byte[] lastBytes = new byte[lastSize];
            indexDs.readFully(lastBytes);
            String lastWord = new String(lastBytes, encoding);
            indexDs.skip(1);
            //
            indexDs.readFully(dWord);
            compSize = byteArrayToLong(dWord);
            indexDs.readFully(dWord);
            decompSize = byteArrayToLong(dWord);
            //
            sum += compSize;
            dictionaryIndex.addFirstLastKeys(i, firstWord, lastWord);
            dictionaryIndex.setKeySizes(i, (int) numEntries, compSize, decompSize);
        }
        if (sum != keyBlocksLen) {
            throw new MDException("Block size error.");
        }
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
        for (int i = 0; i < dictionaryIndex.getKeyNumBlocks(); i++) {
            MDBlockInputStream blockIns = decompress(mdInputStream, dictionaryIndex.getKeyCompSize(i), dictionaryIndex.getKeyDecompSize(i));
            int b = blockIns.read();
            for (int j = 0; j < dictionaryIndex.getNumEntries(i); j++) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (int k = 0; k < 8; k++) {
                    b = blockIns.read();
                }
                while (b != 0) {
                    baos.write(b);
                    b = blockIns.read();
                }
                String keytext = new String(baos.toByteArray(), encoding);
                String result = keytext;
                if (dictionaryIndex.keyContainsName(keytext)) {
                    for (int k = 1; ; k++) {
                        String keywordWithNum = keytext + k;
                        if (!dictionaryIndex.keyContainsName(keywordWithNum)) {
                            result = keywordWithNum;
                            break;
                        }
                    }
                }
                dictionaryIndex.setKeyName(i, result);
            }
        }
        if (dictionaryIndex.keyNameSize() != keySum) {
            throw new MDException("Key sum error.");
        }
    }

    private static void parseRecordBlock(final MDInputStream mdInputStream, DictionaryIndex dictionaryIndex)
            throws IOException, MDException {
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
        long offset = mdInputStream.tell() + recordIndexLen;
        long endOffset = offset + recordBlockLen;
        //
        dictionaryIndex.initRecordNum((int) recordNumBlocks, recordNumEntries, offset);
        long recordBlockCompSize;
        long recordBlockDecompSize;
        for (int i = 0; i < recordNumBlocks; i++) {
            mdInputStream.readFully(dWord);
            recordBlockCompSize = byteArrayToLong(dWord);
            mdInputStream.readFully(dWord);
            recordBlockDecompSize = byteArrayToLong(dWord);
            //
            dictionaryIndex.addRecordSizes(i, recordBlockCompSize, recordBlockDecompSize);
        }
        if (dictionaryIndex.endRecordOffset() != endOffset) {
            throw new MDException("Wrong index position.");
        }
    }

    private static MDBlockInputStream decompress(final MDInputStream inputStream, final long compSize,
                                                 final long decompSize) throws IOException, MDException, DataFormatException {
        int flag = inputStream.read();
        inputStream.skip(3);
        byte[] word = new byte[4];
        inputStream.readFully(word);
        long checksum = byteArrayToInt(word);
        switch(flag) {
            case 0:
                break;
            case 1:
                return new LzoBlockInputStream(inputStream, compSize - 8, decompSize, checksum);
            case 2:
                return new ZlibBlockInputStream(inputStream, compSize - 8, decompSize, checksum);
            default:
                throw new MDException(String.format("Unknown compression level: %d", flag));
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
