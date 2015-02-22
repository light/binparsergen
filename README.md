# binparsergen - Binary parser generator

This project aims to ease the creation of parsers for binary data formats. It is comprised of a data definition language,
that allows to specify the data format in a readable and declarative way, and a code generatior (compiler) that generates
Java classes from data definitions.

## Data definition language

The basic data definition structure looks like this :

    struct <Name of the format> {
       <@offset>  <data type>  <field name>  <constraints>  "<description>";
       ...
    }

The data type is either a built-in primitive type or a user defined type.

The field name can be any valid Java identifier, with the exception that it must not start with a `$` character.

The description is an optional string, enclosed in double quotes, that will be used when formatting the `toString()`
output of the parser.

Example :

    struct MyFormat {
       string(4)   magic ="HELO";
       int32       length             "Length of the file in bytes";
       bits(3)     some_value;
       bits(5)     another_value;
    }

### Primitive data types

The recognized elementary data types are as follows :

DDL type        |     Description    |  Java Type
--------------- | ------------------ | -------
`int8`          | Single byte signed integer    | `byte`
`int16`         | Two-byte signed integer       | `short`
`int24`         | 3-byte signed integer         | `int`
`int32`         | 4-byte signed integer         | `int`
`int64`         | 8-byte signed integer         | `long`
`bits(n)`       | n-bits unsigned integer       | `int`
`string(n)`     | ASCII string of n bytes       | `String`

All integer types are big-endian and bits are read in MSB to LSB order.

The `bits(n)` type allows to read integers smaller than one byte. `n` must be between 1 and 7, inclusive. A multiple
of 8 bits must always be read at any one time before reading more non-bits data. For example the following is valid :

    struct OnByteBoundary {
       bits(7)     foo;
       bits(1)     bar;
       int8        baz;
    }

But the following is not :

    struct NotOnByteBoundary {
       bits(7)     foo;
       int8        baz;
    }

### Composite data types

Nested structures can be defined and used as data types. Example :


    struct MyFormat {
       struct Version {
          int8    major;
          int8    minor;
       }
       
       string(4)   magic;
       Version     version;
    }
    
Structures can also have parameters which are specified in parenthesis after the struct name, and that must be 
specified when using the type (see *arrays* below) :

    struct MyFormat {
       struct Collection(size) {
          int8    type;
          string(4)[size]    elements;
       }
       
       int32             size;
       Collection(size)  items;
    }

### Offsets

It is possible to specify the offset at which a piece of data should be read by prefixing the data declaration by an
`@` character followed by the offset value. The offset is understood as the number of bytes from the start of the
current struct. The value can be a constant or an expression which can reference the current value of other fields. Note
that if a field has not yet been read its value will be zero. Offsets should always go forward and never go back in
the data stream. Example :

    struct MyFormat {
       int32       offset;
       @offset*4   int32     foo;
    }

### Constraints

Constraints can be specified on primitive data elements :

    struct MyFormat {
       int32       min    >0;
       int32       max    <1000;
       int32       value  >=min <=max;
    }

Valid operators on numeric types are `=`, `<=`, `>=`, `<`, `>`, `!=`. Only `=` is valid on strings. If data does not conform
to a constraint, a `ConstraintViolationException` will be thrown when parsing it.

### Arrays

#### Short form arrays

Arrays can be specified in two forms. The short form is the familiar square bracket syntax :

    struct MyFormat {
       int32        items;
       int32[items] values;
    }
    
Arrays specified that way will be read consecutively, without gaps, from the byte stream.

#### Long form arrays

The second *long form* array syntax is as follows :

    array(<size>) { <offset>  <type>  <constraints> }
    
The offset, type and constraints are specified the same way as for a single data type. In addition, the special value
`$` represents the index of the array element being read. This allows to describe the common case of data format which
use a list of offsets to index array elements.

    struct MyFormat {
       struct Collection(size) {
          int8    type;
          string(4)[size]    elements;
       }
       
       int32             size;
       int32             offsets;
       int8[size]        sizes;
       array(size) { @offsets[$] Collection(sizes[$]) } items;
    }

### Conditionals

Conditionals allow to read data only if a condition on previous data applies. They are specified with an `if` block.
The condition must be a valid Java expression. Example :

    struct MyFormat {
       int24       prefix = 0x000001;
       int8        stream_id;
       if ( stream_id == 0b1011_1100 ) {
          int8     control;
       }
    }

### Comments

All text after an `#` character is a comment and is ignored. Example :

    struct MyFormat {
       string(4)   magic;
       int32       length;   # Length of data after the header
    }

### Ignoring data

Quite often data present in the format must be ignored, because it is unused or its role is unknown. In that case, simply
do not specify a name for the data element. Constraints, if present, will still be checked.

    struct MyFormat {
       bits(2)         flags;
       bits(2);        # reserved
       bits(4)         more_flags;
    }

## Code generation

To generate Java classes from a data definition (DDL) file, use the following code :

```Java
    String packageName = "com.example.parser.gen";
    File targetDir = new File( "/path/to/src/main/java" );
    BinParserGen.generateParser( getClass().getResourceAsStream( "MyFormat.ddl" ), packageName, targetDir );
```

Subdirectories will be created in the target directory corresponding to the Java package name. A Java source file 
containing the parser code will be created for each top-level struct in the DDL file. The parser can then be used 
like this :

```Java
    File file = new File( "/path/to/my/file.myformat" );
    InputStream is = new FileInputStream( file );
    MyFormat myFormat = new MyFormat();
    myFormat.parse( is );
    System.out.println( myFormat );
```

## Remarks

This project is in progress, syntax or interfaces might change at a later time. I am very much open to suggestions, especially regarding syntax and features.
