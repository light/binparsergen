package org.bidouille.binparsergen.data;

import java.io.PrintWriter;

import org.bidouille.binparsergen.constraint.Constraint;
import org.bidouille.binparsergen.map.DataMapping;

public class DataInfo {
    public DataDesc desc;
    public boolean anonymous;
    public String name;
    public String comment;
    public String offsetExpr;
    private DataMapping mapping = DataMapping.IDENTITY;

    public DataInfo( DataDesc desc ) {
        this.desc = desc;
    }

    protected void declaration( PrintWriter writer ) {
        mapping.declaration( writer, desc );
    }

    protected void extraction( PrintWriter writer ) {
        mapping.extraction( writer, desc );
    }

    protected void string( PrintWriter writer ) {
        desc.repr( writer, name );
    }

    protected void constraints( PrintWriter writer, String name ) {
        for( Constraint constraint : desc.constraints ) {
            constraint.writeCheck( writer, name );
        }
    }

}
