package org.bidouille.binparsergen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.bidouille.binparsergen.compile.MemoryCompiler;
import org.bidouille.binparsergen.constraint.Constraint;
import org.bidouille.binparsergen.constraint.StringEqualsConstraint;
import org.bidouille.binparsergen.data.DataArrayInfo;
import org.bidouille.binparsergen.data.DataBlock;
import org.bidouille.binparsergen.data.DataDesc;
import org.bidouille.binparsergen.data.DataInfo;
import org.bidouille.binparsergen.data.IfBlock;
import org.bidouille.binparsergen.data.Struct;
import org.bidouille.binparsergen.ddl.DDLBaseListener;
import org.bidouille.binparsergen.ddl.DDLLexer;
import org.bidouille.binparsergen.ddl.DDLParser;
import org.bidouille.binparsergen.ddl.DDLParser.ArgListContext;
import org.bidouille.binparsergen.ddl.DDLParser.ArrayTypeContext;
import org.bidouille.binparsergen.ddl.DDLParser.ConditionalContext;
import org.bidouille.binparsergen.ddl.DDLParser.ConstraintContext;
import org.bidouille.binparsergen.ddl.DDLParser.DataContext;
import org.bidouille.binparsergen.ddl.DDLParser.DefinitionsContext;
import org.bidouille.binparsergen.ddl.DDLParser.ExprContext;
import org.bidouille.binparsergen.ddl.DDLParser.LongArrayFormContext;
import org.bidouille.binparsergen.ddl.DDLParser.ShortArrayFormContext;
import org.bidouille.binparsergen.ddl.DDLParser.StructContext;
import org.bidouille.binparsergen.ddl.DDLParser.TypeContext;
import org.bidouille.binparsergen.template.IndentPrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinParserGen {
    private static Logger log = LoggerFactory.getLogger( BinParserGen.class );

    /**
     * Generate parser source code for the specified data definition in the target folder and with the specified root package.
     *
     * @param source DDL source
     * @param packageName root package of Java classes
     * @param targetDirectory output directory. Will create a hierarchy corresponding to the package.
     * @throws InvalidDefinitionException if the data definition is invalid
     * @throws IOException if there was an error writing the destination files
     */
    public static void generateParser( InputStream source, String packageName, File targetDirectory ) throws InvalidDefinitionException, IOException {
        VisitListener visitor = doParse( source );

        File packageDir = new File( targetDirectory, packageName.replace( '.', '/' ) );
        packageDir.mkdirs();
        for( Struct struct : visitor.topLevelStructs ) {
            File sourceFile = new File( packageDir, struct.name + ".java" );
            IndentPrintWriter writer = new IndentPrintWriter( new FileOutputStream( sourceFile ) );
            struct.write( writer, packageName );
            writer.close();
        }
    }

    /**
     * Generate and compile classes for all the top-level structs defined in the source definition.
     * @param source DDL source
     * @param packageName root package Java classes should be generated in
     * @return map of <Struct name, Class instance>
     * @throws InvalidDefinitionException
     * @throws IOException
     */
    public static Map<String, Class<?>> generateClasses( InputStream source, String packageName ) throws InvalidDefinitionException, IOException {
        VisitListener visitor = doParse( source );

        Map<String, Class<?>> classes = new HashMap<>();
        for( Struct struct : visitor.topLevelStructs ) {
            StringWriter out = new StringWriter();
            IndentPrintWriter writer = new IndentPrintWriter( new PrintWriter( out ) );
            struct.write( writer, packageName );
            writer.close();
            String java = out.toString();
            System.out.println( java );

            Class<?> clazz = MemoryCompiler.compile( packageName + "." + struct.name, java );
            classes.put( struct.name, clazz );
        }

        return classes;

    }

    private static VisitListener doParse( InputStream source ) throws InvalidDefinitionException, IOException {
        BinParserGen parserGen = new BinParserGen();
        DefinitionsContext tree = parserGen.parse( new ANTLRInputStream( source ) );
        VisitListener visitor = new VisitListener();
        ParseTreeWalker.DEFAULT.walk( visitor, tree );

        if( !visitor.errors.isEmpty() ) {
            throw new InvalidDefinitionException( visitor.errors );
        }
        return visitor;
    }

    // Two-stage parsing per https://github.com/antlr/antlr4/issues/374
    private DefinitionsContext parse( ANTLRInputStream is ) throws InvalidDefinitionException {
        DDLLexer lexer = new DDLLexer( is );
        CommonTokenStream tokens = new CommonTokenStream( lexer );
        DDLParser parser = new DDLParser( tokens );

        ErrorListener errorListener = new ErrorListener();
        parser.addErrorListener( errorListener );

        DefinitionsContext tree;
        parser.getInterpreter().setPredictionMode( PredictionMode.SLL );
        try {
            tree = parser.definitions(); // STAGE 1
        } catch( Exception ex ) {
            tokens.reset(); // rewind input stream
            parser.reset();
            errorListener.clear();
            parser.getInterpreter().setPredictionMode( PredictionMode.LL );
            tree = parser.definitions(); // STAGE 2
            // if we parse ok, it's LL not SLL
        }
        if( errorListener.hadErrors ) {
            throw new InvalidDefinitionException( errorListener.errors );
        }

        return tree;
    }

    private static final class VisitListener extends DDLBaseListener {
        List<Struct> topLevelStructs = new ArrayList<>();
        Struct currentStruct;
        DataBlock currentBlock;
        private List<String> errors = new ArrayList<>();

        @Override
        public void enterStruct( StructContext ctx ) {
            String name = ctx.NAME().getText();
            log.info( "Struct {}", name );
            Struct struct = new Struct( currentStruct, name );
            if( ctx.argList() != null ) {
                for( ExprContext arg : ctx.argList().expr() ) {
                    struct.params.add( arg.getText() );
                }
            }
            if( currentStruct != null ) {
                currentStruct.structs.push( struct );
            } else {
                topLevelStructs.add( struct );
            }
            currentStruct = struct;
            currentBlock = currentStruct.datas;
        }

        @Override
        public void exitStruct( StructContext ctx ) {
            currentStruct = currentStruct.parent;
            if( currentStruct != null ) {
                currentBlock = currentStruct.datas;
            }
        }

        @Override
        public void enterConditional( ConditionalContext ctx ) {
            IfBlock ifBlock = new IfBlock( currentBlock, ctx.expr().getText() );
            currentBlock.addBlock( ifBlock );
            currentBlock = ifBlock;
        }

        @Override
        public void exitConditional( ConditionalContext ctx ) {
            currentBlock = currentBlock.parent;
        }

        @Override
        public void enterData( DataContext ctx ) {
            log.info( "[{}] data {}", currentStruct, ctx.getText() );
            DataDesc desc;
            TypeContext typeCtx = ctx.type();
            DataInfo data;
            if( typeCtx != null ) { // Non-array type
                String type = resolveName( typeCtx.NAME().getText() );
                ArgListContext argList = typeCtx.argList();
                desc = getTypeDesc( type, argList, ctx.constraint() );
                data = new DataInfo( desc );
            } else { // Array type
                ArrayTypeContext arrayCtx = ctx.arrayType();
                String cardinality;
                ShortArrayFormContext shortForm = arrayCtx.shortArrayForm();
                LongArrayFormContext longForm = arrayCtx.longArrayForm();
                String type;
                ArgListContext argList;
                List<ConstraintContext> constraints;
                if( shortForm != null ) {
                    cardinality = shortForm.arraySpec().expr().getText();
                    type = shortForm.type().NAME().getText();
                    argList = shortForm.type().argList();
                    constraints = null;
                } else {
                    cardinality = longForm.arraySpec().expr().getText();
                    type = longForm.type().NAME().getText();
                    argList = longForm.type().argList();
                    constraints = longForm.constraint();
                }
                desc = getTypeDesc( type, argList, constraints );
                DataArrayInfo dataArray = new DataArrayInfo( desc, cardinality );
                if( longForm != null && longForm.offset() != null ) {
                    dataArray.elementOffsetExpr = longForm.offset().expr().getText();
                }
                data = dataArray;
            }
            data.name = ctx.NAME() != null ? ctx.NAME().getText() : null;
            data.comment = ctx.description() != null ? ctx.description().getText() : null;
            data.offsetExpr = ctx.offset() != null ? ctx.offset().expr().getText() : null;
            currentBlock.add( data );
        }

        private DataDesc getTypeDesc( String type, ArgListContext argList, List<ConstraintContext> constraintList ) {
            DataDesc desc;
            if( "int8".equals( type ) ) {
                desc = new INT8_DataDesc();
            } else if( "int16".equals( type ) ) {
                desc = new INT16_DataDesc();
            } else if( "int24".equals( type ) ) {
                desc = new INT24_DataDesc();
            } else if( "int32".equals( type ) ) {
                desc = new INT32_DataDesc();
            } else if( "int64".equals( type ) ) {
                desc = new INT64_DataDesc();
            } else if( "bits".equals( type ) ) {
                int bits = getInt( argList, 0 );
                desc = new BITS_DataDesc( bits );
            } else if( "string".equals( type ) ) {
                int bytes = getInt( argList, 0 );
                desc = new FIXED_STRING_DataDesc( bytes );
            } else {
                // Assume user type here, don't check defined struct to allow for externaly provided types and other top-level structs
                // throw new SyntaxErrorException( "Unknown type " + type );
                if( argList != null ) {
                    String[] params = argList.expr().stream().map( expr -> expr.getText() ).toArray( String[]::new );
                    desc = new STRUCT_DataType( type, params );
                } else {
                    desc = new STRUCT_DataType( type );
                }
            }
            if( constraintList != null ) {
                for( ConstraintContext ctx : constraintList ) {
                    String op = ctx.OP().getText();
                    String value = ctx.value().getText();
                    if( "string".equals( type ) ) {
                        if( !"=".equals( op ) ) {
                            errors.add( "Invalid constraint on string : " + op + "value" );
                        } else {
                            desc.constraints.add( new StringEqualsConstraint( value ) );
                        }
                    } else {
                        desc.constraints.add( new Constraint( op, value ) );
                    }
                }
            }
            return desc;
        }

        // Goes through any aliases
        private String resolveName( String name ) {
            Struct block = currentStruct;
            do {
                while( currentStruct.aliases.containsKey( name ) ) {
                    name = currentStruct.aliases.get( name );
                }
                block = block.parent;
            } while( block != null );
            return name;
        }

        private int getInt( ArgListContext paramList, int i ) {
            if( paramList == null || paramList.expr().size() < i + 1 ) {
                throw new SyntaxErrorException( "Not enough parameters" );
            }
            return Integer.parseInt( paramList.expr( i ).getText() );
        }
    }

    //@formatter:off
    private static class INT8_DataDesc  extends DataDesc { public INT8_DataDesc()  { super( "byte", "$eis.readByte()"   ); } }
    private static class INT16_DataDesc extends DataDesc { public INT16_DataDesc() { super( "short", "$eis.readShort()" ); } }
    private static class INT24_DataDesc extends DataDesc { public INT24_DataDesc() { super( "int", "$eis.readInt24()"   ); } }
    private static class INT32_DataDesc extends DataDesc { public INT32_DataDesc() { super( "int", "$eis.readInt()"     ); } }
    private static class INT64_DataDesc extends DataDesc { public INT64_DataDesc() { super( "long", "$eis.readLong()"   ); } }
    private static class BITS_DataDesc  extends DataDesc { public BITS_DataDesc( int bits ) { super( "int", "$sis.readBits( " + bits + " )" ); } }
    //@formatter:on

    private static class FIXED_STRING_DataDesc extends DataDesc {
        public FIXED_STRING_DataDesc( int bytes ) {
            super( "String", "$eis.readFixedString( " + bytes + ", \"ASCII\" )" );
        }
    }

    private static class STRUCT_DataType extends DataDesc {
        public STRUCT_DataType( String structName, String... params ) {
            super( structName, null );
            extractor = "new " + structName + "($sis, $eis " + Arrays.stream( params ).map( p -> ", " + p ).collect( Collectors.joining() ) + ")";
        }

        @Override
        public void repr( PrintWriter writer, String name ) {
            writer.print( "$sb.append(" + name + ".toString($indent + \"|  \"));" );
        }
    }

    private static class SyntaxErrorException extends RuntimeException {

        public SyntaxErrorException( String message ) {
            super( message );
        }

    }

    private static final class ErrorListener extends BaseErrorListener {
        boolean hadErrors;
        String errors = "";

        @Override
        public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, org.antlr.v4.runtime.RecognitionException e ) {
            hadErrors = true;
            errors += "syntaxError : " + offendingSymbol + " " + msg + "\n";
        }

        @Override
        public void reportContextSensitivity( Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs ) {
            hadErrors = true;
            errors += "reportContextSensitivity\n";
        }

        @Override
        public void reportAttemptingFullContext( Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs ) {
            hadErrors = true;
            errors += "reportAttemptingFullContext\n";
        }

        @Override
        public void reportAmbiguity( Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs ) {
            hadErrors = true;
            errors += "reportAmbiguity\n";
        }

        private void clear() {
            hadErrors = false;
            errors = "";
        }

        @Override
        public String toString() {
            return hadErrors ? "There were errors :\n" + errors : "No errors";
        }
    }
}
