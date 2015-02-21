package org.bidouille.binparsergen.data;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.bidouille.binparsergen.constraint.Constraint;

public class DataDesc {
    protected String declaration;
    protected String extractor;
    public List<Constraint> constraints = new ArrayList<>();

    public DataDesc( String declaration, String extraction ) {
        this.declaration = declaration;
        this.extractor = extraction;
    }

    public void declaration( PrintWriter writer ) {
        writer.print( declaration );
    }

    public void extraction( PrintWriter writer ) {
        writer.print( extractor );
    }

    public void repr( PrintWriter writer, String name ) {
        writer.print( "$sb.append(" + name + ");" );
    }
}