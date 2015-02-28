package org.bidouille.binparsergen.ddl;

import static org.junit.Assert.assertThat;

import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

public class DDLParserTest {

    @Test
    public void test_empty() {
        ANTLRInputStream is = new ANTLRInputStream( "struct Test { }" );
        assertNoParsingErrors( is );
    }

    @Test
    public void test_data() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type name;\n"
                + "}" );
    }

    @Test
    public void test_data_same_line() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type name; type2 name2;\n"
                + "}" );
    }

    @Test
    public void test_description() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type name \"Comment\";\n"
                + "}" );
    }

    @Test
    public void test_multiple_data() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type name \"Comment\";\n"
                + "   type2 name2;\n"
                + "   type3 name3;\n"
                + "}" );
    }

    @Test
    public void test_same_line() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type name \"Comment\"; type2 name2;\n"
                + "}" );
    }

    @Test
    public void test_sub_struct() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   struct SubStruct {\n"
                + "      type  name;\n"
                + "   }\n"
                + "   SubStruct name2;\n"
                + "}" );
    }

    @Test
    public void test_inline_struct() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   struct SubStruct { type  name; }\n"
                + "   SubStruct name2;\n"
                + "}" );
    }

    @Test
    @Ignore
    public void test_alias() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   alias key1 123;\n"
                + "}" );
    }

    @Test
    public void test_constraint() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type name =5;\n"
                + "}" );
    }

    @Test
    public void test_offset() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   @0x123 type name;\n"
                + "}" );
    }

    @Test
    public void test_mutiple_attributes() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   @10 type name >2 !=5 <=8;\n"
                + "}" );
    }

    @Test
    public void test_array_type() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type[2] name;\n"
                + "}" );
    }

    @Test
    public void test_parametetrized_type() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type(2) name;\n"
                + "}" );
    }

    @Test
    public void test_parametetrized_array_type() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   type(2)[3] name;\n"
                + "}" );
    }

    @Test
    public void test_conditional() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   if( 5 != 6 ) {\n "
                + "      type name;\n"
                + "   }\n"
                + "}" );
    }

    @Test
    public void test_comment() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                + "   # comment\n"
                + "}" );
    }

    @Test
    public void test_all() {
        assertNoParsingErrors( ""
                + "struct Test {\n"
                // + "   alias key1 123;\n"
                // + "   alias key2 SubStruct;\n"
                // + "   alias key3 key2;\n"
                + "   \n"
                + "   int8    data1;\n"
                + "   # comment\n"
                + "   int16   data2 \"Commentaire\";\n"
                + "   bits(2) =0b10 \"marker\";\n"
                + "   bits(6) data3 \"Some value\";\n"
                + "   @0x20 string(10) data4 \"A string\";\n"
                + "   \n"
                + "   struct SubStruct {\n"
                + "      int24   dataa;\n"
                + "      int16   datab \"Data b\";\n"
                + "   }\n"
                + "   \n"
                + "   key3[data3] data5;\n"
                + "   \n"
                + "   if( data7[0].dataa == key1 ) {\n"
                + "      int32   data6;\n"
                + "   }\n"
                + "   \n"
                + "}" );
    }

    private void assertNoParsingErrors( String s ) {
        assertNoParsingErrors( new ANTLRInputStream( s ) );
    }

    // From https://github.com/antlr/antlr4/issues/374
    private void assertNoParsingErrors( ANTLRInputStream is ) {
        DDLLexer lexer = new DDLLexer( is );
        CommonTokenStream tokens = new CommonTokenStream( lexer );
        DDLParser parser = new DDLParser( tokens );

        TestErrorListener errorListener = new TestErrorListener();
        parser.addErrorListener( errorListener );

        parser.getInterpreter().setPredictionMode( PredictionMode.SLL );
        try {
            parser.struct(); // STAGE 1
        } catch( Exception ex ) {
            tokens.reset(); // rewind input stream
            parser.reset();
            parser.getInterpreter().setPredictionMode( PredictionMode.LL );
            parser.struct(); // STAGE 2
            // if we parse ok, it's LL not SLL
        }

        assertThat( errorListener, isNoError() );
    }

    private static Matcher<TestErrorListener> isNoError() {
        return new BaseMatcher<TestErrorListener>() {

            @Override
            public boolean matches( Object item ) {
                return !((TestErrorListener) item).hadErrors;
            }

            @Override
            public void describeTo( Description description ) {
                description.appendText( "No errors" );
            }
        };
    }

    private static final class TestErrorListener extends BaseErrorListener {
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

        @Override
        public String toString() {
            return hadErrors ? "There were errors :\n" + errors : "No errors";
        }
    }

}
