package org.bidouille.binparsergen.util;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class EndianDataInputStream extends DataInputStream {

    public EndianDataInputStream( InputStream in ) {
        super( in );
    }

    public byte[] readBytes( int length ) throws IOException {
        byte[] buffer = new byte[length];
        readFully( buffer );
        return buffer;
    }

    private byte longBuffer[] = new byte[8];

    public short readShortLE() throws IOException {
        readFully( longBuffer, 0, 2 );
        return (short) (((longBuffer[1] & 0xff) << 8) | ((longBuffer[0] & 0xff) << 0));
    }

    public int readIntLE() throws IOException {
        readFully( longBuffer, 0, 4 );
        return (((longBuffer[3] & 0xff) << 24) |
                ((longBuffer[2] & 0xff) << 16) |
                ((longBuffer[1] & 0xff) << 8) | ((longBuffer[0] & 0xff) << 0));
    }

    public long readLongLE() throws IOException {
        readFully( longBuffer );
        return (((long) longBuffer[7] << 56) +
                ((long) (longBuffer[6] & 0xff) << 48) +
                ((long) (longBuffer[5] & 0xff) << 40) +
                ((long) (longBuffer[4] & 0xff) << 32) +
                ((long) (longBuffer[3] & 0xff) << 24) +
                ((longBuffer[2] & 0xff) << 16) +
                ((longBuffer[1] & 0xff) << 8) + ((longBuffer[0] & 0xff) << 0));
    }

    public int readUnsignedShortLE() throws IOException {
        readFully( longBuffer, 0, 2 );
        return (((longBuffer[1] & 0xff) << 8) | ((longBuffer[0] & 0xff) << 0));
    }

    public long readUnsignedIntLE() throws IOException {
        readFully( longBuffer, 0, 4 );
        return (((long) (longBuffer[3] & 0xff) << 24) |
                ((longBuffer[2] & 0xff) << 16) |
                ((longBuffer[1] & 0xff) << 8) | ((longBuffer[0] & 0xff) << 0));
    }

    public int readInt24() throws IOException {
        readFully( longBuffer, 0, 3 );
        return (((longBuffer[0] & 0xff) << 16)
                | ((longBuffer[1] & 0xff) << 8)
                | ((longBuffer[2] & 0xff) << 0));
    }

    /**
     * Reads a null-terminated string up to maxLength byte long, with the specified charset.
     * @throws EOFException if stream end is reached before finding a zero byte.
     * @throws UnsupportedEncodingException if the charset is not supported.
     */
    public String readCString( int maxLength, String charset ) throws IOException {
        byte[] bytes = new byte[maxLength];
        int i;
        for( i = 0; i < bytes.length; i++ ) {
            bytes[i] = readByte();
            if( bytes[i] == 0 ) {
                break;
            }
        }
        return new String( bytes, 0, i, charset );
    }

    /**
     * Reads a null-terminated string up to length bytes long, with the specified charset. Discards any bytes between the end of the string and length
     * @throws EOFException if there are less than length byte available in the stream.
     * @throws UnsupportedEncodingException if the charset is not supported.
     */
    public String readZeroFillString( int length, String charset ) throws IOException {
        byte[] bytes = new byte[length];
        int i;
        for( i = 0; i < bytes.length; i++ ) {
            bytes[i] = readByte();
            if( bytes[i] == 0 ) {
                break;
            }
        }
        if( i != length ) {
            int skipped = skipBytes( length - i - 1 );
            if( skipped != length - i - 1 ) {
                throw new EOFException();
            }
        }
        return new String( bytes, 0, i, charset );
    }

    /**
     * Reads a fixed length string of length bytes long, with the specified charset.
     * @throws EOFException if there are less than length byte available in the stream.
     * @throws UnsupportedEncodingException if the charset is not supported.
     */
    public String readFixedString( int length, String charset ) throws IOException {
        byte[] bytes = new byte[length];
        readFully( bytes );
        return new String( bytes, charset );
    }

}
