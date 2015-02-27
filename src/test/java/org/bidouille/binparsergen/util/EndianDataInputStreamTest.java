package org.bidouille.binparsergen.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.junit.Test;

public class EndianDataInputStreamTest {
    private EndianDataInputStream in;

    @Test
    public void test_readShortLE() throws IOException {
        in = makeStream( new byte[] { 0x12, 0x34 } );
        assertEquals( (short) 0x3412, in.readShortLE() );
    }

    @Test
    public void test_readUnsignedShortLE() throws IOException {
        in = makeStream( new byte[] { (byte) 0xab, (byte) 0xcd } );
        assertEquals( 0xcdab, in.readUnsignedShortLE() );
    }

    @Test
    public void test_readIntLE() throws IOException {
        in = makeStream( new byte[] { 0x12, 0x34, 0x56, 0x78 } );
        assertEquals( 0x78563412, in.readIntLE() );
    }

    @Test
    public void test_readUnsignedIntLE() throws IOException {
        in = makeStream( new byte[] { (byte) 0xab, (byte) 0xef, (byte) 0xcd, (byte) 0xab } );
        assertEquals( 0xabcdefabL, in.readUnsignedIntLE() );
    }

    @Test
    public void test_readLongLE() throws IOException {
        in = makeStream( new byte[] { 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0 } );
        assertEquals( 0xf0debc9a78563412L, in.readLongLE() );
    }

    @Test
    public void test_readInt24() throws IOException {
        in = makeStream( new byte[] { 0x12, 0x34, 0x56, 0x78 } );
        assertEquals( 0x123456, in.readInt24() );
        assertEquals( 0x78, in.readByte() );
    }

    @Test
    public void test_readInt24_negative() throws IOException {
        in = makeStream( new byte[] { (byte) 0x80, 0x34, 0x56, 0x78 } );
        assertEquals( 0xff803456, in.readInt24() );
        assertEquals( 0x78, in.readByte() );
    }

    @Test
    public void test_readUnsignedInt24() throws IOException {
        in = makeStream( new byte[] { (byte) 0x80, 0x34, 0x56, 0x78 } );
        assertEquals( 0x803456, in.readUnsignedInt24() );
        assertEquals( 0x78, in.readByte() );
    }

    @Test
    public void test_readUnsignedInt() throws IOException {
        in = makeStream( new byte[] { (byte) 0x80, 0x34, 0x56, 0x78 } );
        assertEquals( 0x80345678L, in.readUnsignedInt() );
    }

    @Test
    public void test_readCString() throws IOException {
        in = makeStream( "hello\0world".getBytes( "ascii" ) );
        assertEquals( "hello", in.readCString( 100, "ascii" ) );
    }

    @Test( expected = EOFException.class )
    public void test_readCString_EOF() throws IOException {
        in = makeStream( "hello world".getBytes( "ascii" ) );
        in.readCString( 100, "ascii" );
    }

    @Test
    public void test_readZeroFillString() throws IOException {
        in = makeStream( "hello\0world\0".getBytes( "ascii" ) );
        assertEquals( "hello", in.readZeroFillString( 10, "ascii" ) );
        assertEquals( 'd', in.readByte() );
    }

    @Test
    public void test_readZeroFillString_ExactLength() throws IOException {
        in = makeStream( "hello".getBytes( "ascii" ) );
        assertEquals( "hello", in.readZeroFillString( 5, "ascii" ) );
    }

    @Test( expected = EOFException.class )
    public void test_readZeroFillString_Short() throws IOException {
        in = makeStream( "hello\0wor".getBytes( "ascii" ) );
        in.readZeroFillString( 10, "ascii" );
    }

    @Test( expected = EOFException.class )
    public void test_readZeroFillString_EOF() throws IOException {
        in = makeStream( "hello".getBytes( "ascii" ) );
        in.readZeroFillString( 10, "ascii" );
    }

    @Test
    public void test_readFixedString() throws IOException {
        in = makeStream( "hello\0world".getBytes( "ascii" ) );
        assertEquals( "hello\0w", in.readFixedString( 7, "ascii" ) );
    }

    @Test( expected = EOFException.class )
    public void test_readFixedString_EOF() throws IOException {
        in = makeStream( "hello world".getBytes( "ascii" ) );
        in.readFixedString( 20, "ascii" );
    }

    public EndianDataInputStream makeStream( byte[] data ) {
        return new EndianDataInputStream( new ByteArrayInputStream( data ) );
    }
}
