package org.bidouille.binparsergen.data;

import java.io.PrintWriter;

public class DataArrayInfo extends DataInfo {
    public String cardinalityExpr;
    public String elementOffsetExpr;

    public DataArrayInfo( DataDesc desc, String cardinalityExpr ) {
        super( desc );
        this.cardinalityExpr = cardinalityExpr;
    }

    @Override
    protected void declaration( PrintWriter writer ) {
        super.declaration( writer );
        writer.print( "[]" );
    }

    @Override
    protected void extraction( PrintWriter writer ) {
        writer.print( "readArray_" );
        writer.print( name );
        writer.print( "($sis, $eis)" );
    }

    @Override
    protected void string( PrintWriter writer ) {
        writer.print( "printArray_" );
        super.declaration( writer );
        writer.print( "($sb, $indent, " );
        writer.print( name );
        writer.print( ");" );
    }

}
