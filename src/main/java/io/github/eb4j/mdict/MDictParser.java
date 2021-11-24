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
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;

abstract class MDictParser {

    protected final MDFileInputStream mdInputStream;
    protected MDictDictionaryInfo dictionaryInfo;

    protected long keyNumBlocks;
    protected long keySum;
    protected long keyIndexDecompLen = 0;  // used only when v2
    protected long keyIndexCompLen;
    protected long keyBlocksLen;

    protected int[] numEntries;

    protected final List<String> firstKeys = new ArrayList<>();
    protected final List<String> lastKeys = new ArrayList<>();

    private static final Pattern MDDReplacePattern = Pattern.compile(Pattern.quote("\\"));


    MDictParser(final MDFileInputStream mdInputStream) {
        this.mdInputStream = mdInputStream;
    }

    protected static MDictParser createMDXParser(final MDFileInputStream inputStream) {
        return new MDictParser(inputStream) {
            @Override
            protected boolean isV2Index() {
                String requiredVersion = dictionaryInfo.getRequiredEngineVersion();
                return !requiredVersion.startsWith("1");
            }
        };
    }

    protected static MDictParser createMDDParser(final MDFileInputStream inputStream) {
        return new MDictParser(inputStream) {
            @Override
            protected boolean isV2Index() {
                return true;
            }
            @Override
            protected String unescapeKey(final String keytext) {
                return MDDReplacePattern.matcher(keytext).replaceAll("/");
            }
        };
    }

    Charset getDictCharset() {
        String encoding = dictionaryInfo.getEncoding();
        if (encoding == null || encoding.equals("")) {
            return StandardCharsets.UTF_8;
        }
        if (encoding.equalsIgnoreCase("UTF-8")) {
            return StandardCharsets.UTF_8;
        }
        if (encoding.equalsIgnoreCase("UTF-16") || encoding.equalsIgnoreCase("UTF-16LE")) {
            return StandardCharsets.UTF_16LE;
        }
        if (encoding.equalsIgnoreCase("ISO-8859-1")) {
            return StandardCharsets.ISO_8859_1;
        }
        if (encoding.equalsIgnoreCase("GBK") || encoding.equalsIgnoreCase("GB2312")) {
            return Charset.forName("GB18030");
        }
        // fall back to UTF-8
        return StandardCharsets.UTF_8;
    }

    protected abstract boolean isV2Index();

    protected boolean isHeaderEncrypted() {
        if (!isV2Index()) {
            return false;
        }
        int encrypt = Integer.parseInt(dictionaryInfo.getEncrypted());
        return (encrypt & 0x01) > 0;
    }

    boolean isIndexEncrypted() {
        if ("Yes".equals(dictionaryInfo.getEncrypted())) {
            return true;
        } else if ("No".equals(dictionaryInfo.getEncrypted())) {
            return false;
        } else {
            int encrypt = Integer.parseInt(dictionaryInfo.getEncrypted());
            return (encrypt & 0x02) > 0;
        }
    }

    /**
     * Builder method to parse Header.
     *
     * @return DictionaryInfo object.
     * @throws MDException when read or parse error.
     */
    protected MDictDictionaryInfo parseHeader() throws MDException {
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
    protected DictionaryData<Object> parseIndex(final byte[] password)
            throws MDException, IOException, DataFormatException {
        if (!isV2Index()) {
            parseV1Index();
            return parseKeyBlock();
        }
        if (isHeaderEncrypted()) {
            parseEncryptedIndex(password);
        } else {
            parseV2Index();
        }
        return parseKeyBlock();
    }

    protected String unescapeKey(final String keytext) {
        return keytext;
    }

    protected void parseV1Index() throws IOException {
        keyNumBlocks = MDictUtils.readInt(mdInputStream);
        keySum = MDictUtils.readInt(mdInputStream);
        keyIndexCompLen = MDictUtils.readInt(mdInputStream);
        keyBlocksLen = MDictUtils.readInt(mdInputStream);
    }

    protected void parseV2Index() throws MDException, IOException {
        mdInputStream.seek(dictionaryInfo.getKeyBlockPosition());
        byte[] word = new byte[4];
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

    protected void parseEncryptedIndex(final byte[] password) throws MDException, IOException {
        mdInputStream.seek(dictionaryInfo.getKeyBlockPosition());
        byte[] word = new byte[4];
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
    }

    private String readFirstLastkey(final MDInputStream input, final int size, final Charset encoding)
            throws IOException {
        String key;
        if (StandardCharsets.UTF_16LE.equals(encoding) || StandardCharsets.UTF_16.equals(encoding)) {
            key = MDictUtils.readString(input, size * 2, encoding);
            input.skip(2);
        } else {
            key = MDictUtils.readString(input, size, encoding);
            input.skip(1);
        }
        return key;
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
    private DictionaryData<Object> parseKeyBlock()
            throws MDException, IOException, DataFormatException {
        Charset encoding = getDictCharset();
        numEntries = new int[(int) keyNumBlocks];
        long[] keyCompSize = new long[(int) keyNumBlocks];
        long[] keyDecompSize = new long[(int) keyNumBlocks];
        long sum = 0;
        if (isV2Index()) {
            MDInputStream indexDs = MDictUtils.decompress(mdInputStream, keyIndexCompLen, keyIndexDecompLen,
                    isIndexEncrypted());
            for (int i = 0; i < keyNumBlocks; i++) {
                numEntries[i] = (int) MDictUtils.readLong(indexDs);
                short firstSize = MDictUtils.readShort(indexDs);
                firstKeys.add(unescapeKey(readFirstLastkey(indexDs, firstSize, encoding)));
                short lastSize = MDictUtils.readShort(indexDs);
                lastKeys.add(unescapeKey(readFirstLastkey(indexDs, lastSize, encoding)));
                keyCompSize[i] = MDictUtils.readLong(indexDs);
                keyDecompSize[i] = MDictUtils.readLong(indexDs);
                sum += keyCompSize[i];
            }
        } else {
            for (int i = 0; i < keyNumBlocks; i++) {
                numEntries[i] = MDictUtils.readInt(mdInputStream);
                int firstSize = MDictUtils.readByte(mdInputStream);
                firstKeys.add(MDictUtils.readString(mdInputStream, firstSize, encoding));
                int lastSize = MDictUtils.readByte(mdInputStream);
                lastKeys.add(MDictUtils.readString(mdInputStream, lastSize, encoding));
                keyCompSize[i] = MDictUtils.readInt(mdInputStream);
                keyDecompSize[i] = MDictUtils.readInt(mdInputStream);
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
                long offset;
                if (isV2Index()) {
                    offset = MDictUtils.readLong(blockIns);
                } else {
                    offset = MDictUtils.readInt(blockIns);
                }
                String keytext = unescapeKey(MDictUtils.readCString(blockIns, encoding));
                newDataBuilder.add(keytext, offset);
                totalKeys++;
            }
        }
        if (totalKeys != keySum) {
            throw new MDException("Invalid number of keys");
        }
        return newDataBuilder.build();
    }

    protected RecordIndex parseRecordBlock() throws MDException, IOException {
        long recordNumBlocks;
        long recordNumEntries;
        long recordIndexLen;
        long recordBlockLen;
        if (isV2Index()) {
            recordNumBlocks = MDictUtils.readLong(mdInputStream);
            recordNumEntries = MDictUtils.readLong(mdInputStream);
            recordIndexLen = MDictUtils.readLong(mdInputStream);
            recordBlockLen = MDictUtils.readLong(mdInputStream);
        } else {
            recordNumBlocks = MDictUtils.readInt(mdInputStream);
            recordNumEntries = MDictUtils.readInt(mdInputStream);
            recordIndexLen = MDictUtils.readInt(mdInputStream);
            recordBlockLen = MDictUtils.readInt(mdInputStream);
        }
        long[] recordCompSize = new long[(int) recordNumBlocks];
        long[] recordDecompSize = new long[(int) recordNumBlocks];
        long[] recordOffsetComp = new long[(int) recordNumBlocks];
        long[] recordOffsetDecomp = new long[(int) recordNumBlocks];
        long offsetDecomp = 0;
        long offsetComp = mdInputStream.tell() + recordIndexLen;
        long endPos = offsetComp;
        long endOffsetComp = offsetComp + recordBlockLen;
        for (int i = 0; i < recordNumBlocks; i++) {
            if (isV2Index()) {
                recordCompSize[i] = MDictUtils.readLong(mdInputStream);
                recordDecompSize[i] = MDictUtils.readLong(mdInputStream);
            } else {
                recordCompSize[i] = MDictUtils.readInt(mdInputStream);
                recordDecompSize[i] = MDictUtils.readInt(mdInputStream);
            }
            recordOffsetComp[i] = offsetComp;
            offsetComp += recordCompSize[i];
            recordOffsetDecomp[i] = offsetDecomp;
            offsetDecomp += recordDecompSize[i];
        }
        if (endPos != mdInputStream.tell()) {
            throw new MDException("Wrong index size");
        }
        if (offsetComp != endOffsetComp) {
            throw new MDException("Wrong index position.");
        }
        return new RecordIndex(recordCompSize, recordDecompSize, recordOffsetComp, recordOffsetDecomp,
                recordNumEntries);
    }

}
