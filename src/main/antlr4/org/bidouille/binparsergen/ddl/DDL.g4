grammar DDL;

NL: ('\r'? '\n') -> skip;
WS: [ \t]+ -> skip;
COMMENT: '#' ~[\r\n]* -> skip;
NAME : [a-zA-Z_][a-zA-Z_0-9]*;
INT_LITERAL : [0-9_]+ | '0x' [0-9a-fA-F_]+ | '0b' [01_]+;
STRING_LITERAL : '"' ~["]* '"';
OP : ( '=' | '<=' | '>=' | '<' | '>' | '!=' );
BIN_OP : ( '&&' | '||' );
COLON : ':';
COMMA : ',';
SEMICOLON : ';';

definitions : struct*;
struct : 'struct' NAME ('(' argList ')')? '{' item* '}';
item : data | struct | conditional;

data : offset? (type | arrayType) NAME? constraint* description? SEMICOLON;
type : NAME ('(' argList ')')? ;
arrayType : shortArrayForm | longArrayForm;
shortArrayForm : type '[' arraySpec ']';
longArrayForm : 'array' ('(' arraySpec ')')? '{' offset? type constraint* '}';
argList : ((expr COMMA)* expr)?;
arraySpec : expr;
offset : '@' expr;
constraint : OP value;
value : STRING_LITERAL | INT_LITERAL;
description : STRING_LITERAL;

conditional : 'if' '(' expr ')' '{' item* '}';

expr :
   NAME | value | '$'
   | expr ( OP | BIN_OP | '==' | '/' | '+' | '-' | '*' ) expr
   | expr '?' expr ':' expr
   | '(' expr ')'
   | expr '[' expr ']'
   | ('-'|'+') expr
   | expr '.' expr
;
