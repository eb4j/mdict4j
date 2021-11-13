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

public final class MdxParser {
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
            int headerStrLength = Utils.byteArrayToInt(word);
            byte[] headerStringBytes = new byte[headerStrLength];
            inputStream.readFully(headerStringBytes);
            Adler32 adler32 = new Adler32();
            adler32.update(headerStringBytes);
            inputStream.readFully(word);
            long checksum = Integer.toUnsignedLong(Utils.byteArrayToInt(word, ByteOrder.LITTLE_ENDIAN));
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
        Charset encoding = Charset.forName(info.getEncoding());
        mdInputStream.seek(info.getKeyBlockPosition());
        return parseBlocks(mdInputStream, encoding);
    }

    private static DictionaryIndex parseBlocks(final MDInputStream mdInputStream, final Charset encoding)
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
        long keyNumBlocks = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keySum = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keyIndexDecompLen = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keyIndexCompLen = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        long keyBlocksLen = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(word);
        int checksum = Utils.byteArrayToInt(word);
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
        MDBlockInputStream indexDs = Utils.decompress(mdInputStream, keyIndexCompLen, keyIndexDecompLen);
        int[] numEntries = new int[(int) keyNumBlocks];
        long[] keyCompSize = new long[(int) keyNumBlocks];
        long[] keyDecompSize = new long[(int) keyNumBlocks];
        List<String> firstKeys = new ArrayList<>();
        List<String> lastKeys = new ArrayList<>();
        List<String> keyNameList = new ArrayList<>();
        long sum = 0;
        for (int i = 0; i < keyNumBlocks; i++) {
            indexDs.readFully(dWord);
            numEntries[i] = (int) Utils.byteArrayToLong(dWord);
            indexDs.readFully(hWord);
            short firstSize = Utils.byteArrayToShort(hWord);
            byte[] firstBytes = new byte[firstSize];
            indexDs.readFully(firstBytes);
            String firstWord = new String(firstBytes, encoding);
            indexDs.skip(1);
            //
            indexDs.readFully(hWord);
            short lastSize = Utils.byteArrayToShort(hWord);
            byte[] lastBytes = new byte[lastSize];
            indexDs.readFully(lastBytes);
            String lastWord = new String(lastBytes, encoding);
            indexDs.skip(1);
            //
            indexDs.readFully(dWord);
            keyCompSize[i] = Utils.byteArrayToLong(dWord);
            indexDs.readFully(dWord);
            keyDecompSize[i] = Utils.byteArrayToLong(dWord);
            //
            sum += keyCompSize[i];
            firstKeys.add(firstWord);
            lastKeys.add(lastWord);
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
        for (int i = 0; i < keyNumBlocks; i++) {
            MDBlockInputStream blockIns = Utils.decompress(mdInputStream, keyCompSize[i], keyDecompSize[i]);
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
                keyNameList.add(result);
            }
        }
        if (keyNameList.size() != keySum) {
            throw new MDException("Key sum error.");
        }
        // Record block
        //  number of record blocks + number of entries +
        //  number of bytes of all lists +
        //  number of bytes of all blocks +
        //  index of  each blocks +
        //  record blocks
        mdInputStream.readFully(dWord);
        long recordNumBlocks = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        long recordNumEntries = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        long recordIndexLen = Utils.byteArrayToLong(dWord);
        mdInputStream.readFully(dWord);
        long recordBlockLen = Utils.byteArrayToLong(dWord);
        long offset = mdInputStream.tell() + recordIndexLen;
        long endOffset = offset + recordBlockLen;
        //
        long[] recordCompSize = new long[(int) recordNumBlocks];
        long[] recordDecompSize = new long[(int) recordNumBlocks];
        long[] recordOffsets = new long[(int) recordNumBlocks];
        long recordBlockCompSize;
        long recordBlockDecompSize;
        for (int i = 0; i < recordNumBlocks; i++) {
            mdInputStream.readFully(dWord);
            recordBlockCompSize = Utils.byteArrayToLong(dWord);
            mdInputStream.readFully(dWord);
            recordBlockDecompSize = Utils.byteArrayToLong(dWord);
            //
            recordCompSize[i] = recordBlockCompSize;
            recordDecompSize[i] = recordBlockDecompSize;
            recordOffsets[i] = offset;
            offset += recordBlockCompSize;
        }
        if (offset != endOffset) {
            throw new MDException("Wrong index position.");
        }
        // generate index POJO
        return new DictionaryIndex(keyNameList, firstKeys, lastKeys, numEntries, recordCompSize, recordDecompSize,
                recordOffsets, recordNumEntries, (int) keyNumBlocks);
    }

}
