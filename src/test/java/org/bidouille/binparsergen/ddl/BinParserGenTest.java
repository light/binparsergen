package org.bidouille.binparsergen.ddl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.bidouille.binparsergen.BinParserGen;
import org.bidouille.binparsergen.ConstraintViolationException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

public class BinParserGenTest {
    private static final byte[] BYTES = new byte[] { 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0 };

    @Test
    public void test_empty() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "}",
                BYTES );
    }

    @Test
    public void test_int8() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int8 name;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "name", (byte) 0x12 ) );
    }

    @Test
    public void test_int16() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int16 name;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "name", (short) 0x1234 ) );
    }

    @Test
    public void test_int24() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int24 name;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "name", 0x123456 ) );
    }

    @Test
    public void test_int32() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int32 name;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "name", 0x12345678 ) );
    }

    @Test
    public void test_int64() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int64 name;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "name", 0x123456789abcdef0L ) );
    }

    @Test
    public void test_bits() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   bits(1) bits_1;\n"
                + "   bits(2) bits_2;\n"
                + "   bits(5) bits_5;\n"
                + "}",
                new byte[] { (byte) 0xa5, (byte) 0xc3 } );

        assertThat( instance, hasField( "bits_1", 0b1 ) );
        assertThat( instance, hasField( "bits_2", 0b01 ) );
        assertThat( instance, hasField( "bits_5", 0b00101 ) );
    }

    @Test
    public void test_string() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   string(3) name;\n"
                + "}",
                new byte[] { 65, 66, 67 } );

        assertThat( instance, hasField( "name", "ABC" ) );
    }

    @Test
    public void test_autoadvance() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int16 v1;\n"
                + "   int16 v2;\n"
                + "}",
                BYTES );

        assertThat( instance, hasField( "v1", (short) 0x1234 ) );
        assertThat( instance, hasField( "v2", (short) 0x5678 ) );
    }

    @Test
    public void test_offset() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   @3 int16 v1;\n"
                + "}",
                BYTES );

        assertThat( instance, hasField( "v1", (short) 0x789a ) );
    }

    @Test
    public void test_struct() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   struct Sub {\n"
                + "      int32  v1;\n"
                + "      int32  v2;\n"
                + "   }\n"
                + "   Sub name;\n"
                + "}",
                BYTES );

        assertThat( instance, hasField( "name.v1", 0x12345678 ) );
        assertThat( instance, hasField( "name.v2", 0x9abcdef0 ) );
    }

    @Test
    public void test_parametrized_struct() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   struct Sub(n) {\n"
                + "      int8    v1;\n"
                + "      int8[n] v2;\n"
                + "   }\n"
                + "   Sub(2) name;\n"
                + "}",
                BYTES );

        byte[] array = (byte[]) getField( instance, "name.v2" );
        assertThat( array.length, is( 2 ) );
        assertArrayEquals( array, new byte[] { 0x34, 0x56 } );
        assertThat( instance, hasField( "name.v1", (byte) 0x12 ) );
    }

    @Test
    public void test_short_form_array() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int16[3] v1;\n"
                + "   int16    v2;\n"
                + "}",
                BYTES );

        assertArrayEquals( (short[]) getField( instance, "v1" ), new short[] { (short) 0x1234, (short) 0x5678, (short) 0x9abc } );
        assertThat( instance, hasField( "v2", (short) 0xdef0 ) );
    }

    @Test
    public void test_short_form_array_parametrized_type() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   string(2)[3] v1;\n"
                + "   int16    v2;\n"
                + "}",
                new byte[] { 65, 66, 67, 68, 69, 70, 71, 72 } );

        assertArrayEquals( (String[]) getField( instance, "v1" ), new String[] { "AB", "CD", "EF" } );
        assertThat( instance, hasField( "v2", (short) (71 * 256 + 72) ) );
    }

    @Test
    public void test_anonymous_array() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int8[3];\n"
                + "   int8[2];\n"
                + "   int8    v1;\n"
                + "}",
                BYTES );

        assertThat( instance, hasField( "v1", (byte) 0xbc ) );
    }

    @Test
    public void test_long_form_array() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   array(2) { int8 } name;\n"
                + "}",
                BYTES );

        assertArrayEquals( (byte[]) getField( instance, "name" ), new byte[] { 0x12, 0x34 } );
    }

    @Test
    public void test_long_form_array_with_constraints_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   array(2) { int8 !=0x56 } name;\n"
                + "}",
                BYTES );

        assertArrayEquals( (byte[]) getField( instance, "name" ), new byte[] { 0x12, 0x34 } );
    }

    @Test( expected = ConstraintViolationException.class )
    public void test_long_form_array_with_constraints_not_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   array(2) { int8 !=0x34 } name;\n"
                + "}",
                BYTES );
    }

    @Test
    public void test_long_form_array_struct() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   struct Sub {\n"
                + "      int8   v1;\n"
                + "      int16  v2;\n"
                + "   }\n"
                + "   array(2) { Sub } name;\n"
                + "}",
                BYTES );

        Object[] array = (Object[]) getField( instance, "name" );
        assertThat( array.length, is( 2 ) );
        assertThat( array[0], hasField( "v1", (byte) 0x12 ) );
        assertThat( array[0], hasField( "v2", (short) 0x3456 ) );
        assertThat( array[1], hasField( "v1", (byte) 0x78 ) );
        assertThat( array[1], hasField( "v2", (short) 0x9abc ) );
    }

    @Test
    public void test_long_form_array_with_offset() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   struct Sub {\n"
                + "      int8   v1;\n"
                + "      int16  v2;\n"
                + "   }\n"
                + "   array(2) { @$*5 Sub } name;\n"
                + "}",
                BYTES );

        Object[] array = (Object[]) getField( instance, "name" );
        assertThat( array.length, is( 2 ) );
        assertThat( array[0], hasField( "v1", (byte) 0x12 ) );
        assertThat( array[0], hasField( "v2", (short) 0x3456 ) );
        assertThat( array[1], hasField( "v1", (byte) 0xbc ) );
        assertThat( array[1], hasField( "v2", (short) 0xdef0 ) );
    }

    @Test
    public void test_long_form_array_with_parametrized_struct_with_offset() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   struct Sub(n) {\n"
                + "      int8    v1;\n"
                + "      int8[n] v2;\n"
                + "   }\n"
                + "   array(2) { @$*4 Sub($+1) } name;\n"
                + "}",
                BYTES );

        Object[] array = (Object[]) getField( instance, "name" );
        assertThat( array.length, is( 2 ) );
        assertThat( array[0], hasField( "v1", (byte) 0x12 ) );
        assertArrayEquals( (byte[]) getField( array[0], "v2" ), new byte[] { 0x34 } );
        assertThat( array[1], hasField( "v1", (byte) 0x9a ) );
        assertArrayEquals( (byte[]) getField( array[1], "v2" ), new byte[] { (byte) 0xbc, (byte) 0xde } );
    }

    @Test
    public void test_constraint_equals_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int32 name =0x12345678;\n"
                + "}",
                BYTES );
    }

    @Test( expected = ConstraintViolationException.class )
    public void test_constraint_equals_not_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int32 name =0x12345679;\n"
                + "}",
                BYTES );
    }

    @Test
    public void test_constraint_string_equals_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   string(3) name =\"ABC\";\n"
                + "}",
                new byte[] { 65, 66, 67 } );
    }

    @Test( expected = ConstraintViolationException.class )
    public void test_constraint_string_equals_not_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   string(3) name =\"ABD\";\n"
                + "}",
                new byte[] { 65, 66, 67 } );
    }

    @Test
    public void test_constraint_anonymous_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   string(3) =\"ABC\";\n"
                + "}",
                new byte[] { 65, 66, 67 } );
    }

    @Test( expected = ConstraintViolationException.class )
    public void test_constraint_anonymous_not_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   string(3) =\"ABD\";\n"
                + "}",
                new byte[] { 65, 66, 67 } );
    }

    @Test
    public void test_constraint_multiple_ok() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int32 name >0x12345670 <0x12345680 !=5;\n"
                + "}",
                BYTES );
    }

    @Test( expected = ConstraintViolationException.class )
    public void test_constraint_multiple_no_match() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int32 name <0x12345670 >0x12345680 =5;\n"
                + "}",
                BYTES );
    }

    @Test
    public void test_conditional_match() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int8 v1;\n"
                + "   if( v1 != 1 && v1 != 3 ) {\n"
                + "      int8 v2;\n"
                + "   }\n"
                + "   int8 v3;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "v2", (byte) 0x34 ) );
        assertThat( instance, hasField( "v3", (byte) 0x56 ) );
    }

    @Test
    public void test_conditional_no_match() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int8 v1;\n"
                + "   if( v1 == 1 ) {\n"
                + "      int8 v2;\n"
                + "   }\n"
                + "   int8 v3;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "v3", (byte) 0x34 ) );
    }

    @Test
    public void test_nested_conditional() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int8 v1;\n"
                + "   if( v1 != 0x12 ) {\n"
                + "      int8 v2;\n"
                + "   }\n"
                + "   if( v1 == 0x12 ) {\n"
                + "      int8 v3;\n"
                + "      if( v3 != 0x34 ) {\n"
                + "         int8 v4;\n"
                + "      }\n"
                + "      if( v3 == 0x34 ) {\n"
                + "         int8 v5;\n"
                + "      }\n"
                + "   }\n"
                + "   int8 v6;\n"
                + "}",
                BYTES );
        assertThat( instance, hasField( "v2", (byte) 0x00 ) );
        assertThat( instance, hasField( "v3", (byte) 0x34 ) );
        assertThat( instance, hasField( "v4", (byte) 0x00 ) );
        assertThat( instance, hasField( "v5", (byte) 0x56 ) );
        assertThat( instance, hasField( "v6", (byte) 0x78 ) );
    }

    @Test
    public void test_conditional_with_array() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int8 v1;\n"
                + "   if( v1 == 0x12 ) {\n"
                + "      int8[2] v2;\n"
                + "   }\n"
                + "}",
                BYTES );
        assertArrayEquals( (byte[]) getField( instance, "v2" ), new byte[] { 0x34, 0x56 } );
    }

    @Test
    public void test_constraint_on_anonymous_fields() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int32 =0x12345678;\n"
                + "}",
                BYTES );
    }

    @Test( expected = ConstraintViolationException.class )
    public void test_constraint_on_anonymous_fields_no_match() throws Throwable {
        Object instance = matchAgainst( ""
                + "struct Test {\n"
                + "   int32 =0x12345679;\n"
                + "}",
                BYTES );
    }

    public static Object matchAgainst( String source, byte[] bytes ) throws Throwable {
        Map<String, Class<?>> classes = BinParserGen.generateClasses( new ByteArrayInputStream( source.getBytes() ), BinParserGenTest.class.getPackage().getName() );
        assertThat( classes.size(), is( 1 ) );

        Class<?> clazz = classes.values().iterator().next();
        Object instance = clazz.getConstructor().newInstance();
        Method method = clazz.getMethod( "parse", InputStream.class );
        try {
            method.invoke( instance, new ByteArrayInputStream( bytes ) );
        } catch( InvocationTargetException e ) {
            throw e.getCause();
        }

        return instance;
    }

    private static Matcher hasField( String expectedField, Object expectedValue ) {
        return new FieldMatcher( expectedField, expectedValue );
    }

    private static final class FieldMatcher extends BaseMatcher {
        private String expectedField;
        private Object expectedValue;
        private Exception ex;

        public FieldMatcher( String expectedField, Object expectedValue ) {
            this.expectedField = expectedField;
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean matches( Object item ) {
            try {
                return expectedValue.equals( getField( item, expectedField ) );
            } catch( NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e ) {
                ex = e;
            }
            return false;
        }

        @Override
        public void describeTo( Description description ) {
            description.appendText( "Field '" + expectedField + "' = " + expectedValue );
        }

        @Override
        public void describeMismatch( Object item, Description description ) {
            if( ex != null ) {
                description.appendText( "encoutered exception: " ).appendValue( ex );
            } else {
                description.appendText( "was " ).appendValue( item );
            }
        }
    }

    private static Object getField( Object item, String name ) throws NoSuchFieldException, IllegalAccessException {
        for( String f : name.split( "\\." ) ) {
            Field field = item.getClass().getDeclaredField( f );
            item = field.get( item );
        }
        return item;
    }

}
