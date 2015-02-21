package org.bidouille.binparsergen.constraint;

import java.io.PrintWriter;

public class Constraint {
    public final String op;
    public final String value;

    public Constraint( String op, String value ) {
        this.op = op;
        this.value = value;
    }

    public void writeTest( PrintWriter writer, String name ) {
        writer.write( name );
        if( "=".equals( op ) ) {
            writer.write( "=" );
        }
        writer.write( op );
        writer.write( value );
    }
}
