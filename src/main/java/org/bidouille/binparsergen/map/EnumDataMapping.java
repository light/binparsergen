package org.bidouille.binparsergen.map;

import java.io.PrintWriter;

import org.bidouille.binparsergen.data.DataDesc;

public class EnumDataMapping implements DataMapping {
    private String enumName;

    public EnumDataMapping( String enumName ) {
        this.enumName = enumName;
    }

    @Override
    public void declaration( PrintWriter writer, DataDesc desc ) {
        writer.print( enumName );
    }

    @Override
    public void extraction( PrintWriter writer, DataDesc desc ) {
        writer.print( enumName + ".fromInt(" );
        desc.extraction( writer );
        writer.print( ")" );
    }
}
