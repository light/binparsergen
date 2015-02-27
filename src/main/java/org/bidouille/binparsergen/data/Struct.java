package org.bidouille.binparsergen.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bidouille.binparsergen.ConstraintViolationException;
import org.bidouille.binparsergen.template.IndentPrintWriter;
import org.bidouille.binparsergen.template.Template;
import org.bidouille.binparsergen.util.EndianDataInputStream;
import org.bidouille.binparsergen.util.SkipInputStream;

public class Struct extends Template {
    public Struct parent;
    public String name;
    public final DataBlock datas;
    public ArrayDeque<Struct> structs = new ArrayDeque<>();
    public Map<String, String> aliases = new HashMap<>();
    public List<String> params = new ArrayList<>();

    public Struct( Struct parent, String name ) {
        this.parent = parent;
        this.name = name;
        datas = new DataBlock( null );
    }

    @Override
    public String toString() {
        return name;
    }

    public void writeImports( IndentPrintWriter writer ) {
        for( Class<?> clazz : Arrays.asList( EndianDataInputStream.class, SkipInputStream.class, InputStream.class, IOException.class, ConstraintViolationException.class, StringBuilder.class ) ) {
            writer.println( "import " + clazz.getCanonicalName() + ";" );
        }
        for( Struct subStruct : structs ) {
            subStruct.writeImports( writer );
        }
    }

    public void writeParamFields( IndentPrintWriter writer ) {
        for( String name : params ) {
            writer.println( "int " + name + ";" );
        }
    }

    public void writeSaveParams( IndentPrintWriter writer ) {
        for( String name : params ) {
            writer.println( "this." + name + " =  " + name + ";" );
        }
    }

    public void writeFields( IndentPrintWriter writer ) {
        datas.writeFields( writer );
    }

    public void writeExtracts( IndentPrintWriter writer ) {
        datas.writeExtracts( writer );
    }

    public void writeStrings( IndentPrintWriter writer ) {
        datas.writeStrings( writer );
    }

    // Creates readArray_* helper functions for all arrays
    public void writeReadHelpers( IndentPrintWriter writer ) throws IOException {
        datas.writeReadHelpers( writer );
    }

    // Creates printArray_* helper functions for all data types
    public void writePrintHelpers( IndentPrintWriter writer ) throws IOException {
        datas.writePrintHelpers( writer );
    }

    public void writeStructs( IndentPrintWriter writer ) throws IOException {
        for( Struct struct : structs ) {
            struct.write( writer, null );
        }
    }

    public void writeEnums( IndentPrintWriter writer ) throws IOException {
        // for( Entry<String, EnumGen> entry : enums.entrySet() ) {
        // entry.getValue().setParam( "name", entry.getKey() );
        // entry.getValue().write( "/Enum.java.template", writer );
        // }
    }

    public void write( IndentPrintWriter writer, String packageName ) throws IOException {
        setParam( "name", name );
        setParam( "args", params.stream().map( p -> ", int " + p ).collect( Collectors.joining() ) );
        setParam( "package", packageName );
        super.write( parent == null ? "/Parser.java.template" : "/Struct.java.template", writer );
    }

}
