package org.bidouille.binparsergen.data;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bidouille.binparsergen.constraint.Constraint;
import org.bidouille.binparsergen.template.IndentPrintWriter;
import org.bidouille.binparsergen.template.Template;

public class DataBlock {
    public final DataBlock parent;
    private List<Object> datas = new ArrayList<>(); // Contains a list of DataInfo and DataBlock

    public DataBlock( DataBlock parent ) {
        this.parent = parent;
    }

    public void add( DataInfo data ) {
        datas.add( data );
    }

    public void addBlock( DataBlock block ) {
        datas.add( block );
    }

    public void writeFields( IndentPrintWriter writer ) {
        for( Object data : datas ) {
            if( data instanceof DataInfo ) {
                DataInfo info = (DataInfo) data;
                if( info.name == null ) { // Datas without a name are simply read and discarded
                    continue;
                }
                if( info.comment != null ) {
                    writer.println( "/**" );
                    writer.println( " * " + info.comment );
                    writer.println( " */" );
                }
                if( info.name != null ) {
                    writer.print( "public " );
                    info.declaration( writer );
                    writer.println( " " + info.name + ";" );
                }
                writer.println();
            } else {
                ((DataBlock) data).writeFields( writer );
            }
        }
    }

    public void writeExtracts( IndentPrintWriter writer ) {
        for( Object data : datas ) {
            if( data instanceof DataInfo ) {
                DataInfo info = (DataInfo) data;
                boolean hasConstraints = ! info.desc.constraints.isEmpty();
                String name = info.name;
                if( info.offsetExpr != null ) {
                    writer.println( "$sis.skipTo( " + info.offsetExpr + " );" );
                }
                if( name == null && hasConstraints ) {
                    writer.println("{");
                    writer.pushIndent( "    " );
                    name = "$tmp";
                    info.declaration( writer );
                    writer.println( " " + name + ";" );
                }
                if( name != null ) {
                    writer.print( name + " = " );
                }
                info.extraction( writer );
                writer.println( ";" );
                for( Constraint constraint : info.desc.constraints ) {
                    writer.print( "if(!(" );
                    constraint.writeTest( writer, name );
                    writer.println( ")) {" );
                    writer.println( "    throw new ViolatedConstraintException( \"" + name + constraint.op + escapeQuotes( constraint.value ) + "\", "+name+" );" );
                    writer.println( "}" );
                }
                if( info.name == null && hasConstraints ) {
                    writer.popIndent();
                    writer.println("}");
                }
            } else {
                ((DataBlock) data).writeExtracts( writer );
            }
        }
    }

    public void writeStrings( IndentPrintWriter writer ) {
        for( Object data : datas ) {
            if( data instanceof DataInfo ) {
                DataInfo info = (DataInfo) data;
                if( info.name != null ) {
                    String desc = (info.comment != null ? escapeQuotes( info.comment ) : info.name);
                    writer.println( "$desc = \"" + desc + " : \";" );
                    writer.print( "$sb.append(\"\\n\");" );
                    writer.print( "$sb.append($indent); $sb.append($desc);" );
                    info.string( writer );
                    writer.println();
                }
            } else {
                ((DataBlock) data).writeStrings( writer );
            }
        }
    }

    // Creates readArray_* and printArray_* helper functions for all arrays
    public void writeHelpers( IndentPrintWriter writer ) throws IOException {
        Map<String, DataDesc> uniqueTypes = new HashMap<>();
        for( Object data : datas ) {
            if( data instanceof DataArrayInfo ) {
                DataArrayInfo arrayInfo = (DataArrayInfo) data;
                StringWriter out = new StringWriter();
                arrayInfo.desc.declaration( new PrintWriter( out ) );
                String declaration = out.toString();
                out = new StringWriter();
                arrayInfo.desc.extraction( new PrintWriter( out ) );
                String extraction = out.toString();
                uniqueTypes.put( declaration, arrayInfo.desc );

                Template template = new Template();
                template.setParam( "name", arrayInfo.name );
                template.setParam( "type", declaration );
                template.setParam( "skipTo", arrayInfo.elementOffsetExpr != null ? "$sis.skipTo(" + arrayInfo.elementOffsetExpr + ");" : "" );
                template.setParam( "extractor", extraction );
                template.setParam( "cardinality", arrayInfo.cardinalityExpr );
                template.write( "/readArray.java.template", writer );
            }
        }
        for( Entry<String, DataDesc> entry : uniqueTypes.entrySet() ) {
            Template template = new Template();
            template.setParam( "type", entry.getKey() );
            StringWriter out = new StringWriter();
            entry.getValue().repr( new PrintWriter( out ), "$array[$]" );
            template.setParam( "printer", out.toString() );
            template.write( "/printArray.java.template", writer );
        }
    }

    private static String escapeQuotes( String s ) {
        return s.replace( "\"", "\\\"" );
    }

}
