package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        if (tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement statement = null;
        if (currentFunctionDefinition != null){
            statement = parseReturnStatement();
        }
        if(statement != null) { return statement;}
        statement = parseStatement();
        if (statement != null) { return statement; }
        statement = parseFunctionDeclaration();
        if (statement != null) { return statement; }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseReturnStatement() {
        if(tokens.match(RETURN)){
            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setStart(tokens.consumeToken());
            returnStatement.setFunctionDefinition(currentFunctionDefinition);
            if(!tokens.match(RIGHT_BRACE)) {
                Expression expression = parseExpression();
                returnStatement.setExpression(expression);
                returnStatement.setEnd(expression.getEnd());
            }
            return returnStatement;
        }
        return null;
    }

    private Statement parseFunctionDeclaration(){
        if(tokens.match(FUNCTION)){
            FunctionDefinitionStatement functionDef = new FunctionDefinitionStatement();
            functionDef.setStart(tokens.consumeToken());
            Token functionName = require(IDENTIFIER, functionDef);
            functionDef.setName(functionName.getStringValue());
            require(LEFT_PAREN, functionDef);
            if (!tokens.matchAndConsume(RIGHT_PAREN)){
                do {
                    TypeLiteral type = null;
                    Token identifier = require(IDENTIFIER, functionDef);
                    if(tokens.matchAndConsume(COLON)){
                        type = parseTypeExpression();
                    }
                    functionDef.addParameter(identifier.getStringValue(), type);
                }while (tokens.matchAndConsume(COMMA));
                require(RIGHT_PAREN, functionDef);
            }
            if(tokens.matchAndConsume(COLON)){
                functionDef.setType(parseTypeExpression());
            }else{
                functionDef.setType(null);
            }
            require(LEFT_BRACE, functionDef);
            currentFunctionDefinition = functionDef;
            try{
                ArrayList<Statement> statements = new ArrayList<>();
                while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                    Statement statement = parseProgramStatement();
                    statements.add(statement);
                }
                functionDef.setEnd(require(RIGHT_BRACE, functionDef));
                functionDef.setBody(statements);
            }finally {
                currentFunctionDefinition = null;
            }
            return functionDef;
        }
        return null;
    }

    private Statement parseStatement(){
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        if (tokens.match(FOR)){
            return parseForStatement();
        }
        if (tokens.match(IF)){
            return parseIfStatement();
        }
        if (tokens.match(VAR)){
            return parseVarStatement();
        }
        if (tokens.match(IDENTIFIER)){
            Token identifier = tokens.consumeToken();
            if(tokens.matchAndConsume(EQUAL)){
                AssignmentStatement assignmentStatement = new AssignmentStatement();
                assignmentStatement.setStart(identifier);
                Expression expression = parseExpression();
                assignmentStatement.setVariableName(identifier.getStringValue());
                assignmentStatement.setExpression(expression);
                assignmentStatement.setEnd(expression.getEnd());
                return assignmentStatement;
            }else if(tokens.match(LEFT_PAREN)){
                Token start = tokens.consumeToken();
                ArrayList arguments = new ArrayList();
                if(!tokens.match(RIGHT_PAREN)) {
                    Expression expression = parseExpression();
                    arguments.add(expression);
                    while (tokens.matchAndConsume(COMMA)) {
                        expression = parseExpression();
                        arguments.add(expression);
                    }
                }
                FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifier.getStringValue(), arguments);
                FunctionCallStatement functionCallStatement = new FunctionCallStatement(functionCallExpression);
                Token end = require(RIGHT_PAREN, functionCallExpression);
                functionCallExpression.setStart(start);
                functionCallExpression.setEnd(end);
                functionCallStatement.setStart(start);
                functionCallStatement.setEnd(end);
                return functionCallStatement;
            }
        }
        return null;
    }

    private Statement parseVarStatement() {
        VariableStatement variableStatement = new VariableStatement();
        Token start = tokens.consumeToken();
        variableStatement.setStart(start);
        Token varIdentifier = require(IDENTIFIER, variableStatement);
        variableStatement.setVariableName(varIdentifier.getStringValue());
        if(tokens.matchAndConsume(COLON)){
            variableStatement.setExplicitType(parseTypeExpression().getType());
        }
        require(EQUAL, variableStatement);
        Expression expression = parseExpression();
        variableStatement.setExpression(expression);
        variableStatement.setEnd(expression.getEnd());
        //variableStatement.validate();
        return variableStatement;
    }

    private Statement parseIfStatement() {
        IfStatement ifStatement = new IfStatement();
        ifStatement.setStart(tokens.consumeToken());
        require(LEFT_PAREN, ifStatement);
        Expression expression = parseExpression();
        require(RIGHT_PAREN, ifStatement);
        require(LEFT_BRACE, ifStatement);
        ArrayList<Statement> statements = new ArrayList<>();
        Statement statement = parseStatement();
        statements.add(statement);
        while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
            tokens.consumeToken();
            statement = parseStatement();
            statements.add(statement);
        }
        require(RIGHT_BRACE, ifStatement);
        ArrayList<Statement> elseStatements = new ArrayList<>();
        if(tokens.match(ELSE)){
            tokens.consumeToken();
            if(tokens.match(LEFT_BRACE)) {
                tokens.consumeToken();
                if (!tokens.match(EOF)) {
                    Statement elseStatement = parseStatement();
                    elseStatements.add(elseStatement);
                    while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                        tokens.consumeToken();
                        elseStatement = parseStatement();
                        elseStatements.add(elseStatement);
                    }
                }
                require(RIGHT_BRACE, ifStatement);
            }else{
                parseIfStatement();
            }
        }
        ifStatement.setExpression(expression);
        ifStatement.setTrueStatements(statements);
        ifStatement.setElseStatements(elseStatements);
        return ifStatement;
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    private Statement parseForStatement(){
        Token start = tokens.consumeToken(); // consume 'for'
        ForStatement forStatement = new ForStatement();
        forStatement.setStart(start);
        require(LEFT_PAREN, forStatement);
        Token loopIdentifier = require(IDENTIFIER, forStatement);
        require(IN, forStatement);
        Expression expression = parseExpression();
        require(RIGHT_PAREN, forStatement);
        require(LEFT_BRACE, forStatement);
        ArrayList<Statement> statements = new ArrayList<>();
        Statement statement = parseStatement();
        statements.add(statement);
        while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
            tokens.consumeToken();
            statement = parseStatement();
            statements.add(statement);
        }
        forStatement.setEnd(require(RIGHT_BRACE, forStatement));
        forStatement.setVariableName(loopIdentifier.getStringValue());
        forStatement.setExpression(expression);
        forStatement.setBody(statements);
        return forStatement;
    }

    private TypeLiteral parseTypeExpression(){
        TypeLiteral typeLiteral = new TypeLiteral();
        String type = tokens.getCurrentToken().getStringValue();
        tokens.consumeToken();
        if(type.equals("int")){
            typeLiteral.setType(CatscriptType.INT);
        }else if(type.equals("bool")){
            typeLiteral.setType(CatscriptType.BOOLEAN);
        }else if(type.equals("string")){
            typeLiteral.setType(CatscriptType.STRING);
        }else if(type.equals("object")){
            typeLiteral.setType(CatscriptType.OBJECT);
        }else if(type.equals("list")){
            require(LESS, typeLiteral);
            typeLiteral.setType(CatscriptType.ListType.getListType(parseTypeExpression().getType()));
            require(GREATER, typeLiteral);
        }
        return typeLiteral;
    }


    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();
        while (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rightHandSide.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rightHandSide.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
            return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(STAR, SLASH)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if(tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        }else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        }else if (tokens.match(IDENTIFIER)) {
            Token identifierToken = tokens.consumeToken();
            if(tokens.match(LEFT_PAREN)){
                tokens.consumeToken();
                ArrayList<Expression> expr = new ArrayList<>();
                if(!tokens.match(RIGHT_PAREN)) {
                    Expression val = parseExpression();
                    expr.add(val);
                    while (tokens.match(COMMA)) {
                        tokens.consumeToken();
                        val = parseExpression();
                        expr.add(val);
                    }
                }
                FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifierToken.getStringValue(), expr);
                if(tokens.match(RIGHT_PAREN)) {
                    tokens.consumeToken();
                    return functionCallExpression;
                }else{
                    functionCallExpression.addError(ErrorType.UNTERMINATED_ARG_LIST);
                    return functionCallExpression;
                }
            }else {
                IdentifierExpression identifierExpression = new IdentifierExpression(identifierToken.getStringValue());
                identifierExpression.setToken(identifierToken);
                return identifierExpression;
            }
        }else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        }else if (tokens.match(TRUE, FALSE)) {
            Token booleanToken = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(booleanToken.getType() == TRUE);
            booleanLiteralExpression.setToken(booleanToken);
            return booleanLiteralExpression;
        }else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        }else if (tokens.match(LEFT_PAREN)) {
            Token start = tokens.consumeToken();
            ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(parseExpression());
            boolean rightParen = tokens.match(RIGHT_PAREN);
            parenthesizedExpression.setStart(start);
            parenthesizedExpression.setEnd(parenthesizedExpression.getEnd());
            if(rightParen) {
                tokens.consumeToken();
                return parenthesizedExpression;
            }else{
                SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression();
                syntaxErrorExpression.setToken(tokens.consumeToken());
                return syntaxErrorExpression;
            }
        }else if (tokens.match(LEFT_BRACKET)) {
            tokens.consumeToken();
            ArrayList<Expression> expr = new ArrayList<>();
            if(!tokens.match(RIGHT_BRACKET)) {
                Expression val = parseExpression();
                expr.add(val);
                while (tokens.match(COMMA)) {
                    tokens.consumeToken();
                    val = parseExpression();
                    expr.add(val);
                }
            }
            ListLiteralExpression listLiteralExpression = new ListLiteralExpression(expr);
            if(tokens.match(RIGHT_BRACKET)) {
                tokens.consumeToken();
                return listLiteralExpression;
            }else{
                listLiteralExpression.addError(ErrorType.UNTERMINATED_LIST);
                return listLiteralExpression;
            }
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression();
            syntaxErrorExpression.setToken(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
