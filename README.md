# tinyproto
The aim of this tool to build small fast protobuf parsers for Typescript which lead to minimal page bloat, while allowing the addition of extensions which are specified in
the proto file. Parsers generated by the tool will be naive - there is no 64-bit number support, and no attempt at validation. These parsers are not guaranteed to follow the Protobuf spec at all. 

If you have any issues with the output of this tool then switch to using a more complete and compliant tool such as the google-provided JS one or 
https://github.com/dcodeIO/ProtoBuf.js/

## Usage
First you need to make sure you have java 8 installed. Then download the parser from https://github.com/notak/tinyproto/releases

Then you just run it with the following syntax:

```
java -jar target/proto-0.0.1-SNAPSHOT.jar [OPTIONS] <PROTO FILE PATH/FILENAME> 
```
Options:
```
	--proto-out=<MODIFIED PROTO FILE PATH/NAME>
	--ts-out=<TYPESCRIPT FILE PATH/NAME>
	--ts-imports=<TYPESCRIPT FILE PATH/NAME>
	--[proto|ts]-include-classes=<LIST OF MESSAGES TO OUTPUT CLASSES FOR>
	--ts-include-builders=<LIST OF MESSAGES TO OUTPUT BUILDERS FOR>
	--ts-include-parsers=<LIST OF MESSAGES TO OUTPUT PARSERS FOR>
	--[proto|ts]-include-classes=<LIST OF MESSAGES NOT TO OUTPUT CLASSES FOR>
	--ts-exclude-builders=<LIST OF MESSAGES NOT TO OUTPUT BUILDERS FOR>
	--ts-exclude-parsers=<LIST OF MESSAGES NOT TO OUTPUT PARSERS FOR>
```

This will parse the proto file and (hopefully) output a file containing a module with the same name as the proto package, containing a ```<Message>``` class and a ```<Message>Parser``` class for each message. It will also save a file called proto.ts containing the base parser which all the message parsers extend.

Taking the example of a message of type MyBase which you had encoded in an array buffer called myBuffer. You could parse it by calling:

```
  let myBase: MyBase;
  myBase = new MyBaseParser().decode(new DataView(myBuffer));
```

## Features
* Small file size, and fast run speed which is crucial to using protobuf format data in bulk on page load.

* The parsers and therefore the created entity classes can be extended, making it easier to inject additional functionality to them

* ```divide``` and ```subtract``` options allow you to create tighter encoding of integers. For example if you want to encode milliseconds, which are standard time units in Java and JS, but only actually need 30-second accuracy you can just set a divisor of 30000, or if you have are passing a positive integer value result where -1 represents an error condition you can set a subtract of -1, saving the need to expensively use signed varints. Divide happens before subtract.

* The ```encoding``` option lets you declare an encoder class with static functions or a module. The required ```encode``` function which take in unencoded data and output encoded data suitable for putting into the message, and a ```decode``` function which does the opposite. If you use this option you will also need to use the ```-ts-imports``` command line option to pass in the .ts files the modules or classes live in. You will also need to use the ```decodedType``` option to declare the type (using the proto type system) of the decoded data.

* As these options are non-standard, the google protoc compiler will fail if they are present in the .proto file. In order to allow you to use both tools, you can use the ```-proto-out=<OUTPUT FILENAME>``` option to generate a proto file which is stripped of these options.

* Not implemented yet: a ```decimalPlaces``` option will allow you to code decimals as integers. Minimal implementation in Java will use BigDecimal. In JS will just expose the decimal places as a constant, but ideally the ability will be added to instantiate a type from a library.

## Architecture
Generation is done by feeding a .proto file into a Java parser. This was developed using Antlr 4. The .g4 file for parsing .proto files was adapted from an Antlr 3 grammar provided in another OS project, which will be credited as soon as I find a link. This approach should allow addition of other languages. I'd like to produce a smaller footprint Java library with Java 8 support for example

## Build
* Obtain the source
* bulid using ```mvn package```	

## To do:
* Support a 64-bit library for very large numbers
* Support Fixed-width numbers
* Add decimal support
* Tests on the ts generation code
* Javascript code generator (should be pretty easy)
* Java code generator
