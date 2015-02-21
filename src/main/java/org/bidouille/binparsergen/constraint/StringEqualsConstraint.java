package org.bidouille.binparsergen.constraint;

import java.io.PrintWriter;

public class StringEqualsConstraint extends Constraint {

    public StringEqualsConstraint( String value ) {
        super( "=", value );
    }

    @Override
    public void writeTest( PrintWriter writer, String name ) {
        writer.write( value );
        writer.write( ".equals(" );
        writer.write( name );
        writer.write( ")" );
    }
}
