package org.bidouille.binparsergen;

import java.util.List;

/**
 * Signifies that the data definition file contains errors.
 */
public class InvalidDefinitionException extends Exception {

    public InvalidDefinitionException( String errors ) {
        super( "Definition contains errors : " + errors );
    }

    public InvalidDefinitionException( List<String> errors ) {
        super( String.join( "\n", errors ) );
    }

}
