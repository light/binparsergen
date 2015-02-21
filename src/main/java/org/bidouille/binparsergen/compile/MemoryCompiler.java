package org.bidouille.binparsergen.compile;

import java.util.Arrays;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

// http://blogs.helion-prime.com/2008/06/13/on-the-fly-compilation-in-java6.html
// http://www.javabeat.net/articles/73-the-java-60-compiler-api-1.html
// http://www.ibm.com/developerworks/java/library/j-jcomp/index.html
// http://fivedots.coe.psu.ac.th/~ad/jg/javaArt1/index.html
// Include tools.jar in classpath.
public class MemoryCompiler {

    public static Class<?> compile( String className, String sourceCode ) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if( compiler == null ) {
            throw new RuntimeException( "Java compiler not found. Make sure tools.jar is in the classpath." );
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        MemoryFileManager fileMgr = new MemoryFileManager( compiler.getStandardFileManager( diagnostics, null, null ) );

        JavaFileObject sourceFile = new StringJavaFileObject( className, sourceCode );
        Boolean ok = compiler.getTask( null, fileMgr, diagnostics, null, null, Arrays.asList( sourceFile ) ).call();
        if( !ok ) {
            String message = "Compilation failure :";
            for( Diagnostic<?> d : diagnostics.getDiagnostics() ) {
                String msg = d.toString();
                message += "\n" + msg.substring( msg.indexOf( ' ' ) + 1 );
            }
            throw new RuntimeException( message );
        }

        ClassLoader classLoader = fileMgr.getClassLoader( StandardLocation.CLASS_OUTPUT );
        try {
            return classLoader.loadClass( className );
        } catch( ClassNotFoundException e ) {
            throw new RuntimeException( "Cannot load the class we just compiled!", e ); // Probably can't happen
        }
    }

}
