package edu.montana.csci.csci468.parser.statements;

import com.sun.xml.internal.ws.org.objectweb.asm.Opcodes;
import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import edu.montana.csci.csci468.parser.expressions.ListLiteralExpression;

public class VariableStatement extends Statement {
    private Expression expression;
    private String variableName;
    private CatscriptType explicitType;
    private CatscriptType type;

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public void setExplicitType(CatscriptType type) {
        this.explicitType = type;
    }

    public CatscriptType getExplicitType() {
        return explicitType;
    }

    public boolean isGlobal() {
        return getParent() instanceof CatScriptProgram;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            // TODO if there is an explicit type, ensure it is correct
            //      if not, infer the type from the right hand side expression
            if(this.explicitType != null){
                if(explicitType == expression.getType() || explicitType == CatscriptType.OBJECT) {
                    type = explicitType;
                    symbolTable.registerSymbol(variableName, type);
                }else if(explicitType.equals(CatscriptType.getListType(CatscriptType.INT))){
                    type = explicitType;
                    symbolTable.registerSymbol(variableName, type);
                }else{
                    addError(ErrorType.INCOMPATIBLE_TYPES);
                }
            }else{
                type = expression.getType();
                symbolTable.registerSymbol(variableName, type);
            }
        }
    }

    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        runtime.pushScope();
        runtime.setValue(variableName, expression.evaluate(runtime));
        runtime.popScope();
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        if(explicitType == CatscriptType.INT || explicitType == CatscriptType.BOOLEAN) {
            if (isGlobal()) {
                code.addField(variableName, "I");
                expression.compile(code);
                code.addFieldInstruction(Opcodes.PUTFIELD, getVariableName(), "I", code.getProgramInternalName());
            } else {
                Integer localStorageSlotFor = code.createLocalStorageSlotFor(variableName);
                expression.compile(code);
                code.addVarInstruction(Opcodes.ISTORE, localStorageSlotFor);
            }
        }else{
            if (isGlobal()) {
                code.addField(variableName, "L" + ByteCodeGenerator.internalNameFor(getType().getJavaType()) + ";");
                expression.compile(code);
                code.addFieldInstruction(Opcodes.PUTFIELD, getVariableName(), "L" + ByteCodeGenerator.internalNameFor(getType().getJavaType()) + ";", code.getProgramInternalName());
            } else {
                Integer localStorageSlotFor = code.createLocalStorageSlotFor(variableName);
                expression.compile(code);
                code.addVarInstruction(Opcodes.ASTORE, localStorageSlotFor);
            }

        }
    }
}
