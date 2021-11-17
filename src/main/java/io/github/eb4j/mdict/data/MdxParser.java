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
    DictionaryData<Object> parseIndex(final byte[] password) throws MDException, IOException, DataFormatException {
        Charset encoding = Charset.forName(dictionaryInfo.getEncoding());
        String requiredVersion = dictionaryInfo.getRequiredEngineVersion();
        boolean v2 = !requiredVersion.startsWith("1");
        int encrypt = Integer.parseInt(dictionaryInfo.getEncrypted());
        boolean headerEncrypted = (encrypt & 0x01) > 0;
        mdInputStream.seek(dictionaryInfo.getKeyBlockPosition());
        byte[] word = new byte[4];
        if (v2) {
            if (headerEncrypted) {
                byte[] buf = new byte[40];
                mdInputStream.readFully(buf);
                MDBlockInputStream decrypted = Utils.decompressKeyHeader(buf, password);
                Adler32 adler32 = new Adler32();
                keyNumBlocks = Utils.readLong(decrypted, adler32);
                keySum = Utils.readLong(decrypted, adler32);
                keyIndexDecompLen = Utils.readLong(decrypted, adler32);
                keyIndexCompLen = Utils.readLong(decrypted, adler32);
                keyBlocksLen = Utils.readLong(decrypted, adler32);
                mdInputStream.readFully(word);
                int checksum = Utils.byteArrayToInt(word);
                if (adler32.getValue() != checksum) {
                    throw new MDException("checksum error.");
                }
            } else {
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
            }
        } else {
            keyNumBlocks = Utils.readLong(mdInputStream);
            keySum = Utils.readLong(mdInputStream);
            keyIndexCompLen = Utils.readLong(mdInputStream);
            keyBlocksLen = Utils.readLong(mdInputStream);
        }
        boolean indexEncrypted = (encrypt & 0x02) > 0;
        return parseKeyBlock(v2, encoding, indexEncrypted);
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
    private DictionaryData<Object> parseKeyBlock(final boolean v2, final Charset encoding, final boolean encrypted)
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
        DictionaryDataBuilder<Object> newDataBuilder = new DictionaryDataBuilder<>();
        long totalKeys = 0;
        for (int i = 0; i < keyNumBlocks; i++) {
            MDBlockInputStream blockIns = Utils.decompress(mdInputStream, keyCompSize[i], keyDecompSize[i], false);
            for (int j = 0; j < numEntries[i]; j++) {
                long offset = Utils.readLong(blockIns);
                String keytext = Utils.readCString(blockIns, encoding);
                newDataBuilder.add(keytext, offset);
                totalKeys++;
            }
        }
        if (totalKeys != keySum) {
            throw new MDException("Invalid number of keys");
        }
        return newDataBuilder.build();
    }

    public RecordIndex parseRecordBlock() throws MDException, IOException {
        long recordNumBlocks = Utils.readLong(mdInputStream);
        long[] recordCompSize = new long[(int) recordNumBlocks];
        long[] recordDecompSize = new long[(int) recordNumBlocks];
        long[] recordOffsetComp = new long[(int) recordNumBlocks];
        long[] recordOffsetDecomp = new long[(int) recordNumBlocks];
        long recordNumEntries = Utils.readLong(mdInputStream);
        long recordIndexLen = Utils.readLong(mdInputStream);
        long recordBlockLen = Utils.readLong(mdInputStream);
        long offsetDecomp = 0;
        long offsetComp = mdInputStream.tell() + recordIndexLen;
        long endOffsetComp = offsetComp + recordBlockLen;
        for (int i = 0; i < recordNumBlocks; i++) {
            recordCompSize[i] = Utils.readLong(mdInputStream);
            recordOffsetComp[i] = offsetComp;
            offsetComp += recordCompSize[i];
            recordDecompSize[i] = Utils.readLong(mdInputStream);
            recordOffsetDecomp[i] = offsetDecomp;
            offsetDecomp += recordDecompSize[i];
        }
        if (offsetComp != endOffsetComp) {
            throw new MDException("Wrong index position.");
        }
        return new RecordIndex(recordCompSize, recordDecompSize, recordOffsetComp, recordOffsetDecomp,
                recordNumEntries);
    }

}
