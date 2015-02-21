package org.bidouille.binparsergen.compile;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

class ByteArrayJavaFileObject extends SimpleJavaFileObject {
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    protected ByteArrayJavaFileObject( String className, Kind kind ) {
        super( URI.create( "mem:///" + className.replace( '.', '/' ) + kind.extension ), kind );
    }

    @Override
    public OutputStream openOutputStream() {
        return bos;
    }

    byte[] getBytes() {
        return bos.toByteArray();
    }

}