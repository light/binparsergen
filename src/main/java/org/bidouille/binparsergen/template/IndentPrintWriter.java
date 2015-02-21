package org.bidouille.binparsergen.template;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Stack;

public class IndentPrintWriter extends PrintWriter {
    private boolean lineStart = true;
    private Stack<String> indents = new Stack<>();

    public IndentPrintWriter( OutputStream out ) {
        super( out );
    }

    public IndentPrintWriter( Writer out ) {
        super( out );
    }

    @Override
    public void println() {
        super.println();
        lineStart = true;
    }

    @Override
    public void write( int c ) {
        checkAndPrintIndent();
        super.write( c );
    }

    @Override
    public void write( char[] buf, int off, int len ) {
        checkAndPrintIndent();
        super.write( buf, off, len );
    }

    @Override
    public void write( String s, int off, int len ) {
        checkAndPrintIndent();
        super.write( s, off, len );
    }

    public void pushIndent( String indent ) {
        indents.push( indent );
    }

    public void popIndent() {
        indents.pop();
    }

    public IndentPrintWriter append( String s ) {
        print( s );
        return this;
    }

    public IndentPrintWriter append( int i ) {
        print( i );
        return this;
    }

    private void checkAndPrintIndent() {
        if( lineStart ) {
            lineStart = false;
            for( String indent : indents ) {
                print( indent );
            }
        }
    }

}
