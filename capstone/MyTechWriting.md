## Introduction

Catscript is a statically typed language that compiles to JVM bytecode. Catscript is a relatviely simple language, with only 8 statements, 26 expressions, and 7 types. However, catscript adds several useful tools such as type inference, and a list literal.

---

## Features

### Statements

#### For Statement

A for statement allows code to be executed multiple times. One or more `statements` are executed a number of times based on the contents of an `expression`.

Grammar:

`for_statement = 'for', '(', IDENTIFIER, 'in', expression ')', '{', { statement }, '}'`

Example:

```javascript
//For statement
for(x in [1, 2, 3]) {
    print(x)
}
```

```javascript
//Output
1
2
3
```

#### If Statement

An if statement is a conditional that only executes a series of `statements` if an `expression` evaluates to true.
Alternative if statements can be evaluated using `else`.

Grammar:

`if_statement = 'if', '(', expression, ')', '{', { statement }, '}' [ 'else', ( if_statement | '{', { statement }, '}' ) ];`

Example:

```javascript
var x = True
//If statement
if (x) {
    print("x is true")
}
```

```javascript
x is true //Output
```

Example including else:

```javascript
var x = False
if (x) {
    print("x is true")
//Else
} else {
    print("x is false")
}
```

```javascript
x is false //Output
```

#### Assignment Statement

An assignment statement copies the value of an `EXPRESSION` into a variable marked by an `IDENTIFIER`.

Grammar:

`assignment_statement = IDENTIFIER, '=', expression`

Example:

```javascript
var x = 0
x = 5 //Assignment statement
print(x)
```
```javascript
5 //Output
```

#### Print Statement

A print statement outputs an `expression` to the console.

Grammar:

`print_statement = 'print', '(', expression, ')'`

Example:

```javascript
print("hello world") //Print statement
```

```javascript
hello world //Output
```

#### Function Call Statement

A function call statement invokes a function, by specifying the function `IDENTIFIER` and passing in a list of arguments.

Grammar:

`function_call = IDENTIFIER, '(', argument_list , ')'`

Example:

```javascript
function foo(x) {
    print(x + 1)
}

foo(2) //Function call statement
```

```javascript
3 //Output
```

#### Function Definition Statement

A function definition statement specifies a series of `statements` to be executed when the function is called. 
It often specifies what input type should be accepted and what operations should be performed on that input.

Grammar:

`function_declaration = 'function', IDENTIFIER, '(', parameter_list, ')' + [ ':' + type_expression ], '{',  { function_body_statement },  '}'`

Example:

```javascript
function foo(x) { //Function defintion statement
    print(x + 1)
}

foo(2) 
```

```javascript
3 //Output
```

Example with explicit input type.

```javascript
function foo(x : int) { //Function defintion statement
    print(x + 1)
}

foo(2)
```

```javascript
3 //Output
```

#### Return Statement

A return statement returns a value to the parent function.

Grammar:

`return_statement = 'return' [, expression];`

Example:

```javascript
function foo(x) {
    return x + 1 //Return statement
}
print(foo(2))
```

```javascript
3 //Output
```

#### Variable Statement

A variable statement declares a variable and links it to an `IDENTIFIER`.  
Variables can be global - accessible anywhere, or local - accessible only from within a function.

Grammar:

`variable_statement = 'var', IDENTIFIER, [':', type_expression, ] '=', expression;`

Example of global variable:

```javascript
var x = "hello world" //Global variable statement
function foo() {
    print(x)
}
foo()
```

```javascript
hello world //Output
```

Example of global variable with explicit type cast:

```javascript
var x : string = "hello world" //Global variable statement
function foo() {
    print(x)
}
foo()
```

```javascript
hello world //Output
```

Example of local variable:

```javascript
function foo() {
    var x = hello world //Local variable statement
}

print(x)
```

```javascript
//Output will error, as the variable is defined outside of the print statement's scope
```

---

### Expressions

Catscript expressions:

* Equality expressions `==` and `!=`
* Comparison expressions `>`, `<`, `>=`, and `<=`
* Additive expressions `+` and `-`
* Factor expressions `*` and `/`
* Unary expressions `not` and `-`
* Primary expressions `IDENTIFIER`, `STRING`, `INTEGER`, `true`, `false`, `null`, `list_literal`, `function_call`, and `( expression )`
* Type expressions `int`, `string`, `bool`, `object`, `list`

---

### Types

Catscipt is statically typed, meaning the type of every variable is known at compile time.

Catscript types:

* int - numbers
* string - letters
* bool - true or false
* object - an object
* list - a list of objects
* null - the null value
* void - no type

To determine assignability between types, the rules are:

* Nothing is assignable from void
* Everything is assignable from null
* Otherwise, check the assignability of the backing java classes I.e int is assignable to string but string is not assignable to int
* For lists check the type of components of the list

___

