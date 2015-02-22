package org.bidouille.binparsergen.constraint;

import java.io.PrintWriter;

import org.bidouille.binparsergen.ConstraintViolationException;
import static org.bidouille.binparsergen.data.DataBlock.escapeQuotes;

public class Constraint {
    public final String op;
    public final String value;

    public Constraint( String op, String value ) {
        this.op = op;
        this.value = value;
    }

    public void writeCheck( PrintWriter writer, String name ) {
        writer.print( "if(!(" );
        writeTest( writer, name );
        writer.println( ")) {" );
        writer.println( "    throw new " + ConstraintViolationException.class.getName() + "( \"" + name + op + escapeQuotes( value ) + "\", " + name + " );" );
        writer.println( "}" );
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
