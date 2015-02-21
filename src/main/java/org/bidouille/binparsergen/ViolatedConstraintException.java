package org.bidouille.binparsergen;

import java.io.IOException;

/**
 * Signifies that the parsed data does not correspond to the parser constraints.
 */
public class ViolatedConstraintException extends IOException {

    public ViolatedConstraintException( String constraint, Object value ) {
        super( "Violated constraint : " + constraint + " (" + value + ")" );
    }

}
