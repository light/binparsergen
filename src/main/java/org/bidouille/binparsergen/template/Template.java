package org.bidouille.binparsergen.template;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {
    private Map<String, String> params = new HashMap<>();

    public void setParam( String name, String value ) {
        params.put( name, value );
    }

    public void write( String template, IndentPrintWriter writer ) throws IOException {
        InputStream is = getClass().getResourceAsStream( template );
        try (BufferedReader reader = new BufferedReader( new InputStreamReader( is, "UTF-8" ) )) {
            Pattern indentPat = Pattern.compile( "(.*)##([^#]+)#" );
            String line;
            while( (line = reader.readLine()) != null ) {
                Matcher matcher = indentPat.matcher( line );
                if( matcher.matches() ) {
                    writer.pushIndent( matcher.group( 1 ) );
                    writeBlock( matcher.group( 2 ), writer );
                    writer.popIndent();
                } else {
                    writeLine( writer, line );
                }
            }
        }
    }

    private void writeBlock( String name, PrintWriter writer ) {
        String methodName = "write" + name.substring( 0, 1 ).toUpperCase() + name.substring( 1 );
        try {
            Method method = getClass().getDeclaredMethod( methodName, IndentPrintWriter.class );
            method.invoke( this, writer );
        } catch( NoSuchMethodException | SecurityException | IllegalAccessException e ) {
            throw new RuntimeException( "No " + methodName + " method found.", e );
        } catch( InvocationTargetException e ) {
            throw new RuntimeException( "Exception while writing " + name, e );
        }
    }

    private void writeLine( IndentPrintWriter writer, String line ) {
        Pattern pat = Pattern.compile( "(^|[^#])#([^# ]+)#" );
        Matcher matcher = pat.matcher( line );
        StringBuffer sb = new StringBuffer();
        while( matcher.find() ) {
            String paramName = matcher.group( 2 );
            if( !params.containsKey( paramName ) ) {
                throw new RuntimeException( "Missing parameter '" + paramName + "'" );
            }
            String replacement = matcher.group( 1 ) + params.get( paramName );
            matcher.appendReplacement( sb, replacement.replaceAll( "\\$", "\\\\\\$" ) ); // avoid interpreting $x as backreferences
        }
        matcher.appendTail( sb );
        writer.println( sb.toString() );
    }

}
