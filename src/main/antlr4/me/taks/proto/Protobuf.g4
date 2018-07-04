// Compatibility with Protocol Buffer defines
// https://developers.google.com/protocol-buffers/docs/proto
grammar Protobuf;

COMMENT
  :(  '//' ~('\n' | '\r')* END_OF_LINE
  |  '/*' .*? '*/' 
  )-> skip
  ;
fragment END_OF_LINE: '\r\n' | '\n' | '\r';
WHITESPACE : ('\t' | ' ' | '\r' | '\n' | '\u000C')+ -> skip;

PACKAGE_LITERAL : 'package' ;
IMPORT_LITERAL : 'import' ;
OPTION_LITERAL : 'option' ;
SYNTAX_LITERAL : 'syntax' ;

ENUM_LITERAL : 'enum' ;
MESSAGE_LITERAL : 'message' ;
EXTEND_LITERAL : 'extend' ;
EXTENSIONS_DEF_LITERAL : 'extensions' ;
EXTENSIONS_TO_LITERAL : 'to' ;
EXTENSIONS_MAX_LITERAL : 'max' ;

//GROUP_LITERAL : 'group' ;  // deprecated
//OPTIONAL_DEFAULT_LITERAL : 'default' ;
//OPTIONAL_DEPRECATED_LITERAL : 'deprecated' ;
//REPEATED_PACKED_LITERAL : 'packed' ;

SERVICE_LITERAL : 'service' ;
RETURNS_LITERAL : 'returns' ;
RPC_LITERAL : 'rpc' ;

BLOCK_OPEN : '{' ;
BLOCK_CLOSE : '}' ;
PAREN_OPEN : '(' ;
PAREN_CLOSE : ')' ;
BRACKET_OPEN : '[' ;
BRACKET_CLOSE : ']' ;
EQUALS : '=' ;
COLON : ':' ;
COMMA : ',' ;
ITEM_TERMINATOR : ';' ;

// Protobuf Scope ---------------------
PROTOBUF_SCOPE_LITERAL
  :  REQUIRED_PROTOBUF_SCOPE_LITERAL
  |  OPTIONAL_PROTOBUF_SCOPE_LITERAL
  |  REPEATED_PROTOBUF_SCOPE_LITERAL
  ;

fragment REQUIRED_PROTOBUF_SCOPE_LITERAL : 'required' ;
fragment OPTIONAL_PROTOBUF_SCOPE_LITERAL : 'optional' ;
fragment REPEATED_PROTOBUF_SCOPE_LITERAL : 'repeated' ;
// Protobuf Scope ---------------------

// Protobuf Type ----------------------
PROTOBUF_TYPE_LITERAL
  :  DOUBLE_PROTOBUF_TYPE_LITERAL
  |  FLOAT_PROTOBUF_TYPE_LITERAL
  |  INT32_PROTOBUF_TYPE_LITERAL
  |  INT64_PROTOBUF_TYPE_LITERAL
  |  UINT32_PROTOBUF_TYPE_LITERAL
  |  UINT64_PROTOBUF_TYPE_LITERAL
  |  SINT32_PROTOBUF_TYPE_LITERAL
  |  SINT64_PROTOBUF_TYPE_LITERAL
  |  FIXED32_PROTOBUF_TYPE_LITERAL
  |  FIXED64_PROTOBUF_TYPE_LITERAL
  |  SFIXED32_PROTOBUF_TYPE_LITERAL
  |  SFIXED64_PROTOBUF_TYPE_LITERAL
  |  BOOL_PROTOBUF_TYPE_LITERAL
  |  STRING_PROTOBUF_TYPE_LITERAL
  |  BYTES_PROTOBUF_TYPE_LITERAL
  ;

fragment DOUBLE_PROTOBUF_TYPE_LITERAL : 'double' ;
fragment FLOAT_PROTOBUF_TYPE_LITERAL : 'float' ;
fragment INT32_PROTOBUF_TYPE_LITERAL : 'int32' ;
fragment INT64_PROTOBUF_TYPE_LITERAL : 'int64' ;
fragment UINT32_PROTOBUF_TYPE_LITERAL : 'uint32' ;
fragment UINT64_PROTOBUF_TYPE_LITERAL : 'uint64' ;
fragment SINT32_PROTOBUF_TYPE_LITERAL : 'sint32' ;
fragment SINT64_PROTOBUF_TYPE_LITERAL : 'sint64' ;
fragment FIXED32_PROTOBUF_TYPE_LITERAL : 'fixed32' ;
fragment FIXED64_PROTOBUF_TYPE_LITERAL : 'fixed64' ;
fragment SFIXED32_PROTOBUF_TYPE_LITERAL : 'sfixed32' ;
fragment SFIXED64_PROTOBUF_TYPE_LITERAL : 'sfixed64' ;
fragment BOOL_PROTOBUF_TYPE_LITERAL : 'bool' ;
fragment STRING_PROTOBUF_TYPE_LITERAL : 'string' ;
fragment BYTES_PROTOBUF_TYPE_LITERAL : 'bytes' ;
// Protobuf Type ----------------------

// Integer ----------------------------
INTEGER_LITERAL
  :  HEX_LITERAL
  |  OCTAL_LITERAL
  |  DECIMAL_LITERAL
  ;
fragment HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;
fragment HEX_LITERAL : '-'? '0' ('x'|'X') HEX_DIGIT+ ;
fragment OCTAL_LITERAL : '-'? '0' ('0'..'7')+ ;
fragment DECIMAL_LITERAL : ('0' | '-'? '1'..'9' '0'..'9'*) ;
// Integer ----------------------------

// String -----------------------------
STRING_LITERAL
  :  '"' STRING_GUTS '"'
  ;
fragment STRING_GUTS : ( ESCAPE_SEQUENCE | ~('\\'|'"'|'\n'|'\r') )* ;

fragment ESCAPE_SEQUENCE
  :  '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
  |  OCTAL_ESCAPE
  |  UNICODE_ESCAPE
  ;

fragment OCTAL_ESCAPE
  :  '\\' ('0'..'3') ('0'..'7') ('0'..'7')
  |  '\\' ('0'..'7') ('0'..'7')
  |  '\\' ('0'..'7')
  ;

fragment UNICODE_ESCAPE
  :  '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
  ;
// String -----------------------------

// Bool -------------------------------
BOOL_LITERAL : 'true' | 'false';
// Bool -------------------------------

// Float-------------------------------
FLOAT_LITERAL
  :  '-'? ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
  |  '-'? '.' ('0'..'9')+ EXPONENT?
  |  '-'? ('0'..'9')+ EXPONENT
  ;

fragment EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;
// Float-------------------------------

IDENTIFIER : '_'* ('a'..'z' | 'A'..'Z' ) ('a'..'z' | 'A'..'Z' | '_' | '0'..'9')* ;
QUALIFIED_IDENTIFIER : IDENTIFIER ('.' IDENTIFIER)+ ;
FIELD_IDENTIFIER : '.' IDENTIFIER ;


// Predefines
all_identifier
  :  IDENTIFIER
  |  QUALIFIED_IDENTIFIER
  ;

all_value
  :  IDENTIFIER
  |  literal_value
  ;

literal_value
  :  INTEGER_LITERAL
  |  STRING_LITERAL
  |  BOOL_LITERAL
  |  FLOAT_LITERAL
  ;

proto_type
  :  PROTOBUF_TYPE_LITERAL
  |  all_identifier
  ;

// Proto ------------------------------
proto: (
	pkg=package_def | 
	imports+=import_def | 
	options+=option_line | 
	syntax=syntax_line | 
	enums+=enum_def | 
	exts+=ext_def | 
	messages+=message | 
	services+=service_def
)* EOF  
  	 // Only one package define is allowed - will handle that in the compiler
  ;

package_def:  
	PACKAGE_LITERAL 
	name=all_identifier
	ITEM_TERMINATOR
  ;

import_def:  
	IMPORT_LITERAL 
	file=STRING_LITERAL
	ITEM_TERMINATOR
  ;

syntax_line: 
	SYNTAX_LITERAL
	EQUALS
	version=STRING_LITERAL
	ITEM_TERMINATOR
  ;

option_line:
	OPTION_LITERAL 
	name=option_name 
	EQUALS 
	value=option_all_value 
	ITEM_TERMINATOR
  ;

option_field:  
	BRACKET_OPEN 
	item+=option_field_item 
	(COMMA 
		item+=option_field_item
	)* 
	BRACKET_CLOSE
  ;

option_field_item: 
	name=option_name 
	EQUALS 
	value=option_all_value
  ;

option_all_value
  : all_value
  | option_value_object
  ;

option_value_object
  :  BLOCK_OPEN option_value_item* BLOCK_CLOSE
  ;

option_value_item
  :  IDENTIFIER COLON option_all_value
  ;

option_name
  :  IDENTIFIER
  |  PAREN_OPEN all_identifier PAREN_CLOSE FIELD_IDENTIFIER*
  ;
// Option in line----------------------

// Enum -------------------------------
enum_def:  
	ENUM_LITERAL 
	name=IDENTIFIER
	BLOCK_OPEN 
	(
		options+=option_line | 
		items+=enum_item
	)*
	BLOCK_CLOSE
  ;

enum_item: 
	name=IDENTIFIER 
	EQUALS 
	value=INTEGER_LITERAL 
	options=option_field? 
	ITEM_TERMINATOR
  ;
// Enum -------------------------------

// Message ----------------------------
message:  
	MESSAGE_LITERAL 
	name=message_name 
	BLOCK_OPEN 
	(
		options+=option_line | 
		items+=message_item | 
		messages+=message | 
		enums+=enum_def | 
		ext=message_ext_def
	)+
	BLOCK_CLOSE
  ;

message_name : IDENTIFIER ;

message_item: 
	scope=PROTOBUF_SCOPE_LITERAL? 
	type=proto_type 
	name=IDENTIFIER 
	EQUALS 
	id=INTEGER_LITERAL 
	opts=option_field? 
	ITEM_TERMINATOR
  ;

message_ext_def: 
	EXTENSIONS_DEF_LITERAL 
	INTEGER_LITERAL 
	EXTENSIONS_TO_LITERAL 
	(v=INTEGER_LITERAL | v=EXTENSIONS_MAX_LITERAL) 
	ITEM_TERMINATOR
  ;

ext_def:  
	EXTEND_LITERAL 
	name=all_identifier 
	BLOCK_OPEN 
	ext_content? 
	BLOCK_CLOSE
  ;

ext_content : (option_line | message_item | message | enum_def)+ ;

service_def:  
	SERVICE_LITERAL 
	name=IDENTIFIER
	BLOCK_OPEN 
	(
		options+=option_line | 
		rpcs+=rpc_def 
	)+
	BLOCK_CLOSE
  ;

rpc_def:  
	RPC_LITERAL 
	name=IDENTIFIER
	PAREN_OPEN 
	req=all_identifier 
	PAREN_CLOSE 
	RETURNS_LITERAL 
	PAREN_OPEN 
	resp=all_identifier 
	PAREN_CLOSE 
	(
		BLOCK_OPEN 
		options+=option_line* 
		BLOCK_CLOSE 
		ITEM_TERMINATOR? 
		| ITEM_TERMINATOR
	)
    ;
