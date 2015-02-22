# binparsergen - Binary parser generator

This project aims to ease the creation of parser for binary data formats. It is comprised of a data definition language,
that allows to specify the data format in a readable and declarative way, and a code generatior (compiler) that generates
Java classes from data definitions.

## Data definition language

The basic data definition structure looks like this :

```
struct <Name of the format> {
   <data type>    <field name>    <constraints>    <description>;
   ...
}
```

```
struct MyFormat {
   string(5)   magic ="HELLO";
   int32       length             "";
   bits(3)     some_value;
   bits(5)     another_value;
}
```

### Primitive data types

The recognized elementary data types are as follows :

DDL type        |     Description    |  Java Type
--------------- | ------------------ | -------
int8            | Single byte signed integer     | byte
int16           | Two-byte signed integer        | short
int24           | 3-byte signed integer          | int
int32           | 4-byte signed integer          | int
int64           | 8-byte signed integer          | long
bits(n)         | n-bits unsigned integer        | int
string(n)       | ASCII string of n bytes        | String

All integer types are big-endian and bits are read in MSB to LSB order.

The `bits(n)` type allows to read integers smaller than one byte. `n` must be between 1 and 7, inclusive. A multiple
of 8 bits must always be read at any one time. For example the following is valid :

```
struct OnByteBoundary {
   bits(7)     foo;
   bits(1)     bar;
   int3        baz;
}
```

But the following is not :



### Composite data types

### Constraints

### Conditionals

## Code generation

## Remarks

This project is considered in progress, syntax might change at a later time. I welcome suggestions, especially regarding syntax and features.
