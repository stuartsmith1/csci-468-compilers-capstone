package edu.montana.csci.csci468;

import edu.montana.csci.csci468.CatscriptTestBase;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PartnersTest extends CatscriptTestBase{

    //Test ifStatement inside forStatement
    @Test
    public void forIfStatementParses() {
        ForStatement expr = parseStatement("for(x in [1, 2, 3]){ if(x == 2){ print(x) } else { print(\"not 2\") }}");
        assertNotNull(expr);
        assertEquals("x", expr.getVariableName());
        assertTrue(expr.getExpression() instanceof ListLiteralExpression);
        assertEquals(1, expr.getBody().size());
    }

    //Test nested statement error
    @Test
    void duplicateNameParseErrors() {
        assertEquals(ErrorType.DUPLICATE_NAME, getParseError("function x(a) { if (false){ var x = a }}"));
        assertEquals(ErrorType.DUPLICATE_NAME, getParseError("function x(a) { if (false){ var a = 10 }}"));
        assertEquals(ErrorType.DUPLICATE_NAME, getParseError("function x(a) { for(x in [1, 2, 3]){ if(x == 2){ print(x) } else { print(\"not 2\") }}}"));
        assertEquals(ErrorType.DUPLICATE_NAME, getParseError("var x = 10 function x(z : int) { if (z != 10){ print(z) }}"));
    }

    //Test implementation of assignmentStatements
    @Test
    void assignmentStatementWorks() {
        assertEquals("[1, 2, 3]\n", executeProgram("function foo() : list<int> { var x = [1, 2, 3] var count = 0 for(n in x) { count = count + 1 } if (count != 3) { print(\"error\") return null } else { return x }}" + "print(foo())"));
        assertEquals("[1, 2, 3]\n", compile("function foo() : list<int> { var x = [1, 2, 3] var count = 0 for(n in x) { count = count + 1 } if (count != 3) { print(\"error\") return null } else { return x }}" + "print(foo())"));
    }

}
