package org.bidouille.binparsergen.compile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.JavaFileObject.Kind;

class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    private Map<String, ByteArrayJavaFileObject> classFiles = new HashMap<String, ByteArrayJavaFileObject>();
    private ClassLoader memoryClassLoader = new MemoryClassLoader();

    protected MemoryFileManager( StandardJavaFileManager fileManager ) {
        super( fileManager );
    }

    @Override
    public JavaFileObject getJavaFileForOutput( Location location, String className, Kind kind, FileObject sibling ) throws IOException {
        if( location == StandardLocation.CLASS_OUTPUT && kind == JavaFileObject.Kind.CLASS ) {
            ByteArrayJavaFileObject javaFile = new ByteArrayJavaFileObject( className, kind );
            classFiles.put( className, javaFile );
            return javaFile;
        }
        return super.getJavaFileForOutput( location, className, kind, sibling );
    }

    @Override
    public ClassLoader getClassLoader( Location location ) {
        if( location == StandardLocation.CLASS_OUTPUT ) {
            return memoryClassLoader;
        }
        return super.getClassLoader( location );
    }

    private class MemoryClassLoader extends ClassLoader {

        @Override
        public Class<?> findClass( String className ) throws ClassNotFoundException {
            ByteArrayJavaFileObject classFile = classFiles.get( className );
            if( classFile == null ) {
                throw new ClassNotFoundException( className );
            }
            byte[] bytes = classFile.getBytes();
            return defineClass( className, bytes, 0, bytes.length );
        }
    }
}
