package org.bidouille.binparsergen.data;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
                if( info.anonymous ) { // Anonymous data is not stored
                    continue;
                }
                if( info.comment != null ) {
                    writer.println( "/**" );
                    writer.println( " * " + info.comment );
                    writer.println( " */" );
                }
                writer.print( "public " );
                info.declaration( writer );
                writer.println( " " + info.name + ";" );
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
                // For arrays constraints are checked when reading each element (see writeHelpers()).
                boolean checkConstraints = !info.desc.constraints.isEmpty() && !(info instanceof DataArrayInfo);
                if( info.offsetExpr != null ) {
                    writer.println( "$sis.skipTo( " + info.offsetExpr + " );" );
                }
                if( info.anonymous && checkConstraints ) {
                    info.declaration( writer );
                    writer.println( " " + info.name + ";" );
                }
                if( !info.anonymous || checkConstraints ) {
                    writer.print( info.name + " = " );
                }
                info.extraction( writer );
                writer.println( ";" );
                if( checkConstraints ) {
                    info.constraints( writer, info.name );
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
                if( !info.anonymous ) {
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
                ReadHelperTemplate readHelper = new ReadHelperTemplate( arrayInfo );
                uniqueTypes.put( readHelper.declaration, arrayInfo.desc );
                readHelper.write( writer );
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

    public static class ReadHelperTemplate extends Template {
        private DataArrayInfo arrayInfo;
        private String declaration;

        public ReadHelperTemplate( DataArrayInfo arrayInfo ) {
            this.arrayInfo = arrayInfo;
            StringWriter out = new StringWriter();
            arrayInfo.desc.declaration( new PrintWriter( out ) );
            declaration = out.toString();
            out = new StringWriter();
            arrayInfo.desc.extraction( new PrintWriter( out ) );
            String extraction = out.toString();

            setParam( "name", arrayInfo.name );
            setParam( "type", declaration );
            setParam( "skipTo", arrayInfo.elementOffsetExpr != null ? "$sis.skipTo(" + arrayInfo.elementOffsetExpr + ");" : "" );
            setParam( "extractor", extraction );
            setParam( "cardinality", arrayInfo.cardinalityExpr );
        }

        public void write( IndentPrintWriter writer ) throws IOException {
            write( "/readArray.java.template", writer );
        }

        public void writeConstraints( IndentPrintWriter writer ) {
            arrayInfo.constraints( writer, "$array[$]" );
        }

    }

    public static String escapeQuotes( String s ) {
        return s.replace( "\"", "\\\"" );
    }

}
