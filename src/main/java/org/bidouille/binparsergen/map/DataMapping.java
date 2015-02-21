package org.bidouille.binparsergen.map;

import java.io.PrintWriter;

import org.bidouille.binparsergen.data.DataDesc;

public interface DataMapping {
    public static final DataMapping IDENTITY = new DataMapping() {};

    default void declaration( PrintWriter writer, DataDesc desc ) {
        desc.declaration( writer );
    }

    default void extraction( PrintWriter writer, DataDesc desc ) {
        desc.extraction( writer );
    }

}
