package org.bidouille.binparsergen.util;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class SkipInputStream extends FilterInputStream {
    private long offset;
    private int offset_bits;
    private int b;
    private Stack<Long> offsets = new Stack<>();

    public SkipInputStream( InputStream in ) {
        super( in );
    }

    @Override
    public int read() throws IOException {
        if( offset_bits != 0 ) {
            throw new IOException( "Not on a byte boundary (" + offset_bits + ")" );
        }
        int read = super.read();
        if( read != -1 ) {
            offset++;
        }
        return read;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {
        if( offset_bits != 0 ) {
            throw new IOException( "Not on a byte boundary (" + offset_bits + ")" );
        }
        int read = super.read( b, off, len );
        if( read != -1 ) {
            offset += read;
        }
        return read;
    }

    @Override
    public long skip( long n ) throws IOException {
        if( offset_bits != 0 ) {
            throw new IOException( "Not on a byte boundary (" + offset_bits + ")" );
        }
        long skip = super.skip( n );
        offset += skip;
        return skip;
    }

    @Override
    public synchronized void mark( int readlimit ) {
        offsets.push( offset );
        super.mark( readlimit );
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        offset = offsets.pop();
    }

    /**
     * Moves the stream forward to the byte offset specified. Discards current bit position if any.
     * @param offset byte offset to skip to
     * @throws EOFException if the required offset could not be reached
     * @throws IOException
     */
    public void skipTo( long offset ) throws IOException {
        if( this.offset_bits != 0 ) {
            this.offset++;
            this.offset_bits = 0;
        }
        if( this.offset == offset ) {
            return;
        }

        boolean zeroBytesSkipped = false;
        while( this.offset != offset ) {
            long skipped = skip( offset - this.offset );
            if( skipped == 0 && zeroBytesSkipped ) {
                // Zero bytes skipped twice in a row ? Stop trying.
                break;
            }
            zeroBytesSkipped = skipped == 0;
        }

        if( this.offset != offset ) {
            // Zero bytes were skipped twice in a row, that's not good, try to advance by reading from the stream instead.
            while( this.offset != offset ) {
                int read = read();
                if( read == -1 ) {
                    throw new EOFException( "Failed to skip to offset " + offset + ", actual offset : " + this.offset );
                }
            }
        }
    }

    /**
     * Read bit fields. Must read a multiple of 8 bits before resuming byte read operations.
     * @param n number of bits to read
     * @return
     * @throws IOException
     */
    public int readBits( int n ) throws IOException {
        if( offset_bits == 0 ) {
            b = super.read();
            if( b == -1 ) {
                return -1;
            }
        }
        if( n > 8 - offset_bits ) {
            throw new IOException( "Reading bits across byte boundaries is not supported" );
        }
        int shift = 8 - offset_bits - n;
        int mask = 0xff >>> (8 - n);
        int bits = (b & (mask << shift)) >>> shift;

        offset_bits += n;
        if( offset_bits == 8 ) {
            offset_bits = 0;
            offset++;
        }
        return bits;
    }

    /**
     * Returns the current byte offset from the beginning of the stream.
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Pushes the current offset in a stack and behaves as if the current offset is now zero.
     * @throws IOException if attempting to push an offset while not on a byte boundary.
     */
    public void pushOffset() throws IOException {
        if( offset_bits != 0 ) {
            throw new IOException( "Not on a byte boundary" );
        }
        offsets.push( offset );
        offset = 0;
    }

    /**
     * Pops an offset previously pushed using {@link #pushOffset()}. The offset will be restored, and incremented to reflect the amount the stream
     * advanced since the last {@link #pushOffset()}.
     * @throws IOException if attempting to pop while not on a byte boundary. Will also be thrown when attempting to pop whithout a previous push.
     */
    public void popOffset() throws IOException {
        if( offset_bits != 0 ) {
            throw new IOException( "Not on a byte boundary" );
        }
        if( offsets.isEmpty() ) {
            throw new IOException( "Stack underflow" );
        }
        offset = offsets.pop() + offset;
    }

}
