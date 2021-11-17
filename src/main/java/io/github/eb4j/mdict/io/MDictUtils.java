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

package io.github.eb4j.mdict.io;

import io.github.eb4j.mdict.MDException;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.lzo_uintp;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.zip.Adler32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class MDictUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private MDictUtils() {
    }

    public static long readLong(final MDFileInputStream mdInputStream) throws IOException {
        byte[] dWord = new byte[8];
        mdInputStream.readFully(dWord);
        return byteArrayToLong(dWord);
    }

    public static long readLong(final MDFileInputStream mdInputStream, final Adler32 adler32) throws IOException {
        byte[] dWord = new byte[8];
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        return byteArrayToLong(dWord);
    }

    public static long readLong(final MDBlockInputStream mdInputStream, final Adler32 adler32) throws IOException {
        byte[] dWord = new byte[8];
        mdInputStream.readFully(dWord);
        adler32.update(dWord);
        return byteArrayToLong(dWord);
    }

    public static long readLong(final MDInputStream mdInputStream) throws IOException {
        byte[] dWord = new byte[8];
        mdInputStream.readFully(dWord);
        return byteArrayToLong(dWord);
    }

    public static int readByte(final MDFileInputStream mdInputStream) throws IOException {
        byte[] b = new byte[1];
        mdInputStream.readFully(b);
        return b[0] & 0xff;
    }

    public static short readShort(final MDInputStream mdBlockInputStream) throws IOException {
        byte[] bytes = new byte[2];
        mdBlockInputStream.readFully(bytes);
        return byteArrayToShort(bytes);
    }

    public static String readString(final MDInputStream mdInputStream, final int size, final Charset encoding)
            throws IOException {
        byte[] bytes = new byte[size];
        mdInputStream.readFully(bytes);
        return new String(bytes, encoding);
    }

    public static String readString(final MDFileInputStream mdInputStream, final int size, final Charset encoding)
            throws IOException {
        byte[] bytes = new byte[size];
        mdInputStream.readFully(bytes);
        return new String(bytes, encoding);
    }

    public static String readCString(final MDInputStream mdInputStream, final Charset encoding)
            throws MDException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (StandardCharsets.UTF_16LE.equals(encoding) || StandardCharsets.UTF_16.equals(encoding)) {
            byte[] buf = new byte[2];
            try {
                mdInputStream.readFully(buf);
                while (buf[0] != 0 && buf[1] != 0) {
                    baos.write(buf);
                    mdInputStream.readFully(buf);
                }
            } catch (IOException e) {
                throw new MDException("Unexpected end of stream of data.");
            }
        } else {
            int c = mdInputStream.read();
            while (c > 0) {
                baos.write(c);
                c = mdInputStream.read();
            }
            if (c == -1) {
                throw new MDException("Unexpected end of stream of data.");
            }
        }
        return new String(baos.toByteArray(), encoding);
    }

    public static MDInputStream decompress(final MDFileInputStream inputStream, final long compSize,
                                                final long decompSize, final boolean encrypted)
            throws IOException, MDException, DataFormatException {
        int flag = inputStream.read();
        inputStream.skip(3);
        byte[] checksum = new byte[4];
        inputStream.readFully(checksum);
        byte[] input;
        byte[] output;
        switch (flag) {
            case 0:
                break;
            case 1:
                input = new byte[(int) compSize - 8];
                output = new byte[(int) decompSize];
                inputStream.readFully(input);
                LzoAlgorithm algorithm = LzoAlgorithm.LZO1X;
                LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
                lzo_uintp outLen = new lzo_uintp();
                if (encrypted) {
                    try {
                        byte[] temp = decryptKeyIndex(input, checksum);
                        decompressor.decompress(temp, 0, (int) compSize - 8, output, 0, outLen);
                    } catch (NoSuchAlgorithmException e) {
                        throw new MDException("Decryption failed.", e);
                    }
                } else {
                    decompressor.decompress(input, 0, (int) compSize - 8, output, 0, outLen);
                }
                if (outLen.value != decompSize) {
                    throw new MDException("Decompression size is differ.");
                }
                Adler32 adler32 = new Adler32();
                adler32.update(output);
                if (adler32.getValue() != MDictUtils.byteArrayToInt(checksum)) {
                    throw new MDException("Decompression checksum error.");
                }
                return new MDBlockInputStream(new ByteArrayInputStream(output));
            case 2:
                input = new byte[(int) compSize - 8];
                output = new byte[(int) decompSize];
                inputStream.readFully(input);
                Inflater inflater = new Inflater();
                if (encrypted) {
                    try {
                        inflater.setInput(decryptKeyIndex(input, checksum));
                    } catch (NoSuchAlgorithmException e) {
                        throw new MDException("Decryption failed.", e);
                    }
                } else {
                    inflater.setInput(input);
                }
                int size;
                try {
                    size = inflater.inflate(output);
                } catch (DataFormatException e) {
                    throw new MDException("Decompression error.", e);
                }
                inflater.end();
                if (size != decompSize) {
                    throw new MDException("Decompression error, wrong size.");
                }
                return new MDBlockInputStream(new ByteArrayInputStream(output));
            default:
                throw new MDException(String.format("Unknown compression level: %d", flag));
        }
        throw new MDException("Unsupported data.");
    }

    public static byte[] decryptKeyIndex(final byte[] buffer, final byte[] salt)
            throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("RIPEMD128");
        messageDigest.update(salt);
        messageDigest.update(Hex.decode("95360000"));
        byte[] key = messageDigest.digest();
        byte[] result = new byte[buffer.length];
        byte prev = 0x36;
        for (int i = 0; i < buffer.length; i++) {
            byte t = (byte) (((buffer[i] & 0xff) >>> 4) | (buffer[i] << 4) & 0xff);
            result[i] = (byte) (t ^ prev ^ (i & 0xff) ^ key[i % 16]);
            prev = buffer[i];
        }
        return result;
    }

    public static MDBlockInputStream decompressKeyHeader(final byte[] input, final byte[] keytext) {
        byte[] result = decryptKeyHeader(input, keytext);
        return new MDBlockInputStream(new ByteArrayInputStream(result));
    }

    public static byte[] decryptKeyHeader(final byte[] input, final byte[] keytext) {
        byte[] result = new byte[input.length];
        byte[] ivs = Hex.decode("0000000000000000");
        Salsa20Engine salsa20 = new Salsa20Engine(8);
        CipherParameters param = new KeyParameter(keytext);
        param = new ParametersWithIV(param, ivs);
        salsa20.init(false, param);
        salsa20.processBytes(input, 0, input.length, result, 0);
        return result;
    }

    public static long byteArrayToLong(final byte[] dWord) {
        ByteBuffer buffer = ByteBuffer.wrap(dWord).order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    public static int byteArrayToInt(final byte[] word, final ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.wrap(word).order(byteOrder);
        return buffer.getInt();
    }

    public static int byteArrayToInt(final byte[] word) {
        return byteArrayToInt(word, ByteOrder.BIG_ENDIAN);
    }

    public static short byteArrayToShort(final byte[] hWord) {
        ByteBuffer buffer = ByteBuffer.wrap(hWord).order(ByteOrder.BIG_ENDIAN);
        return buffer.getShort();
    }
}
