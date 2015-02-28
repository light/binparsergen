package org.bidouille.binparsergen.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class SkipInputStreamTest {
    private SkipInputStream in;

    @Before
    public void setUp() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( "hello world".getBytes( "ascii" ) ) );
    }

    // //////////////////// skipTo

    @Test
    public void test_skipTo() throws IOException {
        in.skip( 4 );
        in.skipTo( 6 );
        assertEquals( 'w', in.read() );
    }

    @Test
    public void test_skipTo_zero() throws IOException {
        in.skip( 6 );
        in.skipTo( 6 );
        assertEquals( 'w', in.read() );
    }

    @Test( expected = EOFException.class )
    public void test_skipTo_EOF() throws IOException {
        in.skipTo( 20 );
    }

    @Test( expected = IOException.class )
    public void test_skipTo_backwards() throws IOException {
        in.skip( 6 );
        in.skipTo( 4 );
    }

    @Test( expected = IOException.class )
    public void test_skipTo_negative() throws IOException {
        in.skipTo( -4 );
    }

    // //////////////////// readBits

    @Test
    public void test_readBits() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        assertEquals( 2, in.readBits( 2 ) );
        assertEquals( 3, in.readBits( 2 ) );
        assertEquals( 0, in.readBits( 3 ) );
        assertEquals( 1, in.readBits( 1 ) );
        assertEquals( 0xa, in.readBits( 4 ) );
    }

    @Test
    public void test_readBits_across_byte_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        in.readBits( 6 );
        assertEquals( 0b011010, in.readBits( 6 ) );
    }

    @Test
    public void test_readBits_on_byte_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        assertEquals( 0xb1, in.readBits( 8 ) );
        assertEquals( 0xa5, in.read() );
    }

    @Test
    public void test_readBits_two_byte_boundaries() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5, (byte) 0xc5 } ) );
        in.readBits( 6 );
        assertEquals( 0b011010010111, in.readBits( 12 ) );
    }

    @Test( expected = IOException.class )
    public void test_read_non_byte_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        in.readBits( 2 );
        in.read();
    }

    @Test( expected = IOException.class )
    public void test_read_array_non_byte_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        in.readBits( 2 );
        in.read( new byte[2] );
    }

    @Test( expected = IOException.class )
    public void test_skip_non_byte_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        in.readBits( 2 );
        in.skip( 1 );
    }

    @Test
    public void test_skipTo_non_byte_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        in.readBits( 2 );
        in.skipTo( 1 );
        assertEquals( 0xa5, in.read() );
    }

    @Test
    public void test_readBits_end_of_stream() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1, (byte) 0xa5 } ) );
        in.skipTo( 1 );
        in.readBits( 8 );
        assertEquals( -1, in.readBits( 1 ) );
    }

    @Test( expected = IOException.class )
    public void test_readBits_straddling_end_of_stream() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { (byte) 0xb1 } ) );
        in.readBits( 2 );
        in.readBits( 8 );
    }

    // //////////////////// getOffset

    @Test
    public void test_getOffset() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        assertEquals( 0, in.getOffset() );
        in.read();
        assertEquals( 1, in.getOffset() );
        in.readBits( 8 );
        assertEquals( 2, in.getOffset() );
    }

    // //////////////////// getBitOffset

    @Test
    public void test_getBitOffset() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        assertEquals( 0, in.getBitOffset() );
        in.read();
        assertEquals( 0, in.getBitOffset() );
        in.readBits( 4 );
        assertEquals( 4, in.getBitOffset() );
        in.readBits( 12 );
        assertEquals( 0, in.getBitOffset() );
    }

    @Test
    public void test_getBitOffset_after_skip() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        in.readBits( 2 );
        in.skipTo( 1 );
        assertEquals( 0, in.getBitOffset() );
    }

    // //////////////////// pushOffset

    @Test
    public void test_pushOffset() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        in.read();
        in.pushOffset();
        in.skipTo( 1 );
        assertEquals( 0x03, in.read() );
        assertEquals( 2, in.getOffset() );
    }

    @Test
    public void test_pushOffset_multiple() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 } ) );
        in.read();
        in.pushOffset();
        in.skipTo( 1 );
        in.read();
        in.pushOffset();
        in.skipTo( 1 );
        assertEquals( 0x05, in.read() );
        assertEquals( 2, in.getOffset() );
    }

    @Test( expected = IOException.class )
    public void test_pushOffset_not_on_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        in.readBits( 2 );
        in.pushOffset();
    }

    // //////////////////// popOffset

    @Test
    public void test_popOffset() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        in.read();
        in.pushOffset();
        in.skipTo( 1 );
        in.popOffset();
        assertEquals( 2, in.getOffset() );
        assertEquals( 0x03, in.read() );
    }

    @Test
    public void test_popOffset_multiple() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 } ) );
        in.read();
        in.pushOffset(); // delta +1
        in.skipTo( 1 );
        in.read();
        in.pushOffset(); // delta +3
        in.skipTo( 1 );
        in.read();
        assertEquals( 2, in.getOffset() );
        in.popOffset(); // delta +1
        assertEquals( 4, in.getOffset() );
        assertEquals( 0x06, in.read() );
        in.popOffset(); // delta +0
        assertEquals( 6, in.getOffset() );
        assertEquals( 0x07, in.read() );
    }

    @Test( expected = IOException.class )
    public void test_popOffset_not_on_boundary() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        in.pushOffset();
        in.readBits( 2 );
        in.popOffset();
    }

    @Test( expected = IOException.class )
    public void test_popOffset_stack_underflow() throws IOException {
        in = new SkipInputStream( new ByteArrayInputStream( new byte[] { 0x01, 0x02, 0x03 } ) );
        in.pushOffset();
        in.popOffset();
        in.popOffset();
    }

}
