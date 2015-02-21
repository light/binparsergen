package org.bidouille.binparsergen.compile;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.bidouille.binparsergen.compile.MemoryCompiler;
import org.junit.Test;

public class MemoryCompilerTest {

    @Test
    public void test_compile() throws Exception {
        Class<?> clazz = MemoryCompiler.compile(
                "test.TestClass",
                "package test;"
                        + "public class TestClass {"
                        + "  public String testMethod() {"
                        + "    return \"result\";"
                        + "  }"
                        + "}" );
        Object instance = clazz.newInstance();
        Method method = clazz.getMethod( "testMethod" );
        Object result = method.invoke( instance );
        assertEquals( "result", result );
    }

    @Test
    public void test_compile_anonymous_class() throws Exception {
        Class<?> clazz = MemoryCompiler.compile( "test.TestClass",
                "package test;"
                        + "public class TestClass {"
                        + "  public class Inner {}"
                        + "  public Inner testMethod() {"
                        + "    return new Inner();"
                        + "  }"
                        + "}" );
        Object instance = clazz.newInstance();
        Method method = clazz.getMethod( "testMethod" );
        Object result = method.invoke( instance );
    }
}
