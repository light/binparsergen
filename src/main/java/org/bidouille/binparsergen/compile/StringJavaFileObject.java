package org.bidouille.binparsergen.compile;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

class StringJavaFileObject extends SimpleJavaFileObject {
    private String contents;

    public StringJavaFileObject( String className, String contents ) {
        super( URI.create( "mem:///" + className.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
        this.contents = contents;
    }

    @Override
    public CharSequence getCharContent( boolean ignoreEncodingErrors ) {
        return contents;
    }
}