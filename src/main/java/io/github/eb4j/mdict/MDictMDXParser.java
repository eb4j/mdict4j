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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.eb4j.mdict.io.MDBlockInputStream;
import io.github.eb4j.mdict.io.MDFileInputStream;
import io.github.eb4j.mdict.io.MDInputStream;
import io.github.eb4j.mdict.io.MDictUtils;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;

class MDictMDXParser {

    private final MDFileInputStream mdInputStream;
    private MDictDictionaryInfo dictionaryInfo;
    private long keyNumBlocks;
    private long keySum;
    private long keyIndexDecompLen = 0;  // used only when v2
    private long keyIndexCompLen;
    private long keyBlocksLen;

    private int[] numEntries;

    private final List<String> firstKeys = new ArrayList<>();
    private final List<String> lastKeys = new ArrayList<>();

    MDictMDXParser(final MDFileInputStream inputStream) {
        mdInputStream = inputStream;
    }

    /**
     * Builder method to parse Header.
     *
     * @return DictionaryInfo object.
     * @throws MDException when read or parse error.
     */
    MDictDictionaryInfo parseHeader() throws MDException {
        byte[] word = new byte[4];
        try {
            // Header
            // LEN + UTF-16LE string + checksum
            mdInputStream.seek(0);
            mdInputStream.readFully(word);
            int headerStrLength = MDictUtils.byteArrayToInt(word);
            byte[] headerStringBytes = new byte[headerStrLength];
            mdInputStream.readFully(headerStringBytes);
            Adler32 adler32 = new Adler32();
            adler32.update(headerStringBytes);
            mdInputStream.readFully(word);
            long checksum = Integer.toUnsignedLong(MDictUtils.byteArrayToInt(word, ByteOrder.LITTLE_ENDIAN));
            if (checksum != adler32.getValue()) {
                throw new MDException("checksum error.");
            }
            String headerString = new String(headerStringBytes, StandardCharsets.UTF_16LE);
            ObjectMapper mapper = new XmlMapper();
            dictionaryInfo = mapper.readValue(headerString, MDictDictionaryInfo.class);
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
                MDBlockInputStream decrypted = MDictUtils.decompressKeyHeader(buf, password);
                Adler32 adler32 = new Adler32();
                keyNumBlocks = MDictUtils.readLong(decrypted, adler32);
                keySum = MDictUtils.readLong(decrypted, adler32);
                keyIndexDecompLen = MDictUtils.readLong(decrypted, adler32);
                keyIndexCompLen = MDictUtils.readLong(decrypted, adler32);
                keyBlocksLen = MDictUtils.readLong(decrypted, adler32);
                mdInputStream.readFully(word);
                int checksum = MDictUtils.byteArrayToInt(word);
                if (adler32.getValue() != checksum) {
                    throw new MDException("checksum error.");
                }
            } else {
                Adler32 adler32 = new Adler32();
                keyNumBlocks = MDictUtils.readLong(mdInputStream, adler32);
                keySum = MDictUtils.readLong(mdInputStream, adler32);
                keyIndexDecompLen = MDictUtils.readLong(mdInputStream, adler32);
                keyIndexCompLen = MDictUtils.readLong(mdInputStream, adler32);
                keyBlocksLen = MDictUtils.readLong(mdInputStream, adler32);
                mdInputStream.readFully(word);
                int checksum = MDictUtils.byteArrayToInt(word);
                if (adler32.getValue() != checksum) {
                    throw new MDException("checksum error.");
                }
            }
        } else {
            keyNumBlocks = MDictUtils.readLong(mdInputStream);
            keySum = MDictUtils.readLong(mdInputStream);
            keyIndexCompLen = MDictUtils.readLong(mdInputStream);
            keyBlocksLen = MDictUtils.readLong(mdInputStream);
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
            MDInputStream indexDs = MDictUtils.decompress(mdInputStream, keyIndexCompLen, keyIndexDecompLen, encrypted);
            for (int i = 0; i < keyNumBlocks; i++) {
                indexDs.readFully(dWord);
                numEntries[i] = (int) MDictUtils.byteArrayToLong(dWord);
                short firstSize = MDictUtils.readShort(indexDs);
                firstKeys.add(MDictUtils.readString(indexDs, firstSize, encoding));
                indexDs.skip(1);
                short lastSize = MDictUtils.readShort(indexDs);
                lastKeys.add(MDictUtils.readString(indexDs, lastSize, encoding));
                indexDs.skip(1);
                keyCompSize[i] = MDictUtils.readLong(indexDs);
                keyDecompSize[i] = MDictUtils.readLong(indexDs);
                sum += keyCompSize[i];
            }
        } else {
            for (int i = 0; i < keyNumBlocks; i++) {
                numEntries[i] = (int) MDictUtils.readLong(mdInputStream);
                int firstSize = MDictUtils.readByte(mdInputStream);
                firstKeys.add(MDictUtils.readString(mdInputStream, firstSize, encoding));
                mdInputStream.skip(1);
                int lastSize = MDictUtils.readByte(mdInputStream);
                lastKeys.add(MDictUtils.readString(mdInputStream, lastSize, encoding));
                mdInputStream.skip(1);
                keyCompSize[i] = MDictUtils.readLong(mdInputStream);
                keyDecompSize[i] = MDictUtils.readLong(mdInputStream);
                sum += keyCompSize[i];
            }
        }
        if (sum != keyBlocksLen) {
            throw new MDException("Block size error.");
        }
        DictionaryDataBuilder<Object> newDataBuilder = new DictionaryDataBuilder<>();
        long totalKeys = 0;
        for (int i = 0; i < keyNumBlocks; i++) {
            MDInputStream blockIns = MDictUtils.decompress(mdInputStream, keyCompSize[i], keyDecompSize[i], false);
            for (int j = 0; j < numEntries[i]; j++) {
                long offset = MDictUtils.readLong(blockIns);
                String keytext = MDictUtils.readCString(blockIns, encoding);
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
        long recordNumBlocks = MDictUtils.readLong(mdInputStream);
        long[] recordCompSize = new long[(int) recordNumBlocks];
        long[] recordDecompSize = new long[(int) recordNumBlocks];
        long[] recordOffsetComp = new long[(int) recordNumBlocks];
        long[] recordOffsetDecomp = new long[(int) recordNumBlocks];
        long recordNumEntries = MDictUtils.readLong(mdInputStream);
        long recordIndexLen = MDictUtils.readLong(mdInputStream);
        long recordBlockLen = MDictUtils.readLong(mdInputStream);
        long offsetDecomp = 0;
        long offsetComp = mdInputStream.tell() + recordIndexLen;
        long endOffsetComp = offsetComp + recordBlockLen;
        for (int i = 0; i < recordNumBlocks; i++) {
            recordCompSize[i] = MDictUtils.readLong(mdInputStream);
            recordOffsetComp[i] = offsetComp;
            offsetComp += recordCompSize[i];
            recordDecompSize[i] = MDictUtils.readLong(mdInputStream);
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
