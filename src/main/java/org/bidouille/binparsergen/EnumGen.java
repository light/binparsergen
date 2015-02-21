package org.bidouille.binparsergen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bidouille.binparsergen.template.IndentPrintWriter;
import org.bidouille.binparsergen.template.Template;

class EnumGen extends Template {
    private String className;
    private List<Value> values = new ArrayList<>();

    EnumGen( String className ) {
        this.className = className;
    }

    public void value( int i, String name, String description ) {
        if( name == null ) {
            throw new NullPointerException( "Name cannot be null" );
        }
        for( Value value : values ) {
            if( value.i == i ) {
                throw new IllegalArgumentException( "Duplicate id" );
            }
            if( value.name.equals( name ) ) {
                throw new IllegalArgumentException( "Duplicate name" );
            }
        }
        values.add( new Value( i, name, description ) );
    }

    public void write( File genDir, String genPackage ) throws IOException {
        setParam( "name", className );
        setParam( "package", genPackage );

        File parserSourceFile = new File( genDir, genPackage.replace( '.', '/' ) + "/" + className + ".java" );
        try (IndentPrintWriter writer = new IndentPrintWriter( new FileOutputStream( parserSourceFile ) )) {
            write( "/Enum.java.template", writer );
        }
    }

    protected void writeValues( IndentPrintWriter writer ) {
        for( Value value : values ) {
            writer.append( value.name ).append( "(" )
                    .append( value.i ).append( "," )
                    .append( "\"" ).append( value.description ).append( "\"" )
                    .append( ")," ).println();
        }
    }

    private class Value {
        final int i;
        final String name;
        final String description;

        public Value( int i, String name, String description ) {
            this.i = i;
            this.name = name;
            this.description = description;
        }

    }
}
