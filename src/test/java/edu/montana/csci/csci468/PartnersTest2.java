package edu.montana.csci.csci468;

import edu.montana.csci.csci468.CatscriptTestBase;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PartnersTest2 extends CatscriptTestBase {

    @Test
    public void forStatementEnsuresClosingParen() {
        ForStatement expr = parseStatement("for(x in [1, 2, 3]{ print(x) }", false);
        assertNotNull(expr);
        assertTrue(expr.hasErrors());
    }

    @Test
    public void varStatementWithImplicitStringType() {
        VariableStatement expr = parseStatement("var x = \"hello world\"");
        assertNotNull(expr);
        assertEquals("x", expr.getVariableName());
        assertTrue(expr.getExpression() instanceof StringLiteralExpression);
    }

    @Test
    void doubleUnaryExpressionEvaluatesProperly() {
        assertEquals(1, evaluateExpression("--1"));
        assertEquals(true, evaluateExpression("not not true"));
    }
}
