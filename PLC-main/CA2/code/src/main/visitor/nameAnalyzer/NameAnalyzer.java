package main.visitor.nameAnalyzer;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.FunctionDeclaration;
import main.ast.nodes.declaration.MainDeclaration;
import main.ast.nodes.declaration.PatternDeclaration;
import main.ast.nodes.declaration.VarDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.value.FunctionPointer;
import main.ast.nodes.expression.value.ListValue;
import main.ast.nodes.statement.*;
import main.compileError.CompileError;
import main.compileError.nameErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExists;
import main.symbolTable.exceptions.ItemNotFound;
import main.symbolTable.item.FunctionItem;
import main.symbolTable.item.PatternItem;
import main.symbolTable.item.VarItem;
import main.visitor.Visitor;

import java.util.ArrayList;

public class NameAnalyzer extends Visitor<Void> {
    public ArrayList<CompileError> nameErrors = new ArrayList<>();


    private void processFunctionDeclarations(Program program,ArrayList<FunctionItem>functionItems) {
        int duplicateFunctionId = 0;

        for (FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()) {
            FunctionItem functionItem = new FunctionItem(functionDeclaration);
            try {
                SymbolTable.root.put(functionItem);
                functionItems.add(functionItem);
            } catch (ItemAlreadyExists e) {
                handleDuplicateFunction(functionDeclaration, functionItem, functionItems, duplicateFunctionId);
            }
        }
    }

    private void handleDuplicateFunction(FunctionDeclaration functionDeclaration, FunctionItem functionItem,
                                         ArrayList<FunctionItem> functionItems, int duplicateFunctionId) {
        nameErrors.add(new RedefinitionOfFunction(functionDeclaration.getLine(),
                functionDeclaration.getFunctionName().getName()));
        duplicateFunctionId++;
        String freshName = functionItem.getName() + "#" + duplicateFunctionId;
        Identifier newId = functionDeclaration.getFunctionName();
        newId.setName(freshName);
        functionDeclaration.setFunctionName(newId);
        FunctionItem newItem = new FunctionItem(functionDeclaration);
        functionItems.add(newItem);
        try {
            SymbolTable.root.put(newItem);
        } catch (ItemAlreadyExists ignored) {
        }
    }

    private void processPatternDeclarations(Program program, ArrayList<PatternItem> patternItems) {
        int duplicatePatternId = 0;
        for (PatternDeclaration patternDeclaration : program.getPatternDeclarations()) {
            PatternItem patternItem = new PatternItem(patternDeclaration);
            try {
                SymbolTable.root.put(patternItem);
                patternItems.add(patternItem);
            } catch (ItemAlreadyExists e) {
                handleDuplicatePattern(patternDeclaration, patternItem, patternItems, duplicatePatternId);
            }
        }
    }

    private void handleDuplicatePattern(PatternDeclaration patternDeclaration, PatternItem patternItem,
                                        ArrayList<PatternItem> patternItems, int duplicatePatternId) {
        nameErrors.add(new RedefinitionOfPattern(patternDeclaration.getLine(),
                patternDeclaration.getPatternName().getName()));
        duplicatePatternId += 1;
        String freshName = patternItem.getName() + "#" + duplicatePatternId;
        Identifier newId = patternDeclaration.getPatternName();
        newId.setName(freshName);
        patternDeclaration.setPatternName(newId);
        PatternItem newItem = new PatternItem(patternDeclaration);
        patternItems.add(newItem);
        try {
            SymbolTable.root.put(newItem);
        } catch (ItemAlreadyExists ignored) {
        }
    }

    private void visitFunctionBodies(Program program,ArrayList<FunctionItem>functionItems) {
        int visitingFunctionIndex = 0;


        for (FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()) {
            FunctionItem functionItem = functionItems.get(visitingFunctionIndex);
            SymbolTable functionSymbolTable = new SymbolTable();
            functionItem.setFunctionSymbolTable(functionSymbolTable);
            SymbolTable.push(functionSymbolTable);
            functionDeclaration.accept(this);
            SymbolTable.pop();
            visitingFunctionIndex += 1;
        }
    }

    private void visitPatternBodies(Program program, ArrayList<PatternItem> patternItems) {
        int visitingPatternIndex = 0;
        for (PatternDeclaration patternDeclaration : program.getPatternDeclarations()) {
            PatternItem patternItem = patternItems.get(visitingPatternIndex);
            SymbolTable patternSymbolTable = new SymbolTable();
            patternItem.setPatternSymbolTable(patternSymbolTable);
            SymbolTable.push(patternSymbolTable);
            patternDeclaration.accept(this);
            SymbolTable.pop();
            visitingPatternIndex += 1;
        }
    }


    @Override
    public Void visit(Program program) {
        SymbolTable.root = new SymbolTable();
        SymbolTable.top = new SymbolTable();
        ArrayList<FunctionItem> functionItems = new ArrayList<>();
        ArrayList<PatternItem> patternItems = new ArrayList<>();
        processFunctionDeclarations(program,functionItems);
        processPatternDeclarations(program,patternItems);
        visitFunctionBodies(program,functionItems);
        visitPatternBodies(program,patternItems);
        program.getMain().accept(this);
        return (null);
    }

    @Override
    public Void visit(Identifier identifier){
        try {
            SymbolTable.top.getItem("VAR:" + identifier.getName());
        }
        catch (ItemNotFound ignored) {
            nameErrors.add(new VariableNotDeclared(identifier.getLine(), identifier.getName()));
        }
        return (null);
    }



    @Override
    public Void visit(VarDeclaration varDeclaration){
        VarItem varItem = new VarItem(varDeclaration.getName());
        try {
            SymbolTable.top.put(varItem);
        }
        catch (ItemAlreadyExists ignored) {
            nameErrors.add(new DuplicateArg(varDeclaration.getLine(), varDeclaration.getName().getName()));
        }

        return (null);
    }

    @Override
    public Void visit(FunctionDeclaration functionDeclaration){
        for (VarDeclaration currentDec : functionDeclaration.getArgs()) {
            VarItem varItem = new VarItem(currentDec.getName());
            String varName=varItem.getName();
            String functionDecName=functionDeclaration.getFunctionName().getName();

            if(functionDecName.equals(varName))
                nameErrors.add(new IdenticalArgFunctionName(functionDeclaration.getLine(), functionDeclaration.getFunctionName().getName()));

            currentDec.accept(this);
        }

        for (Statement currentStm : functionDeclaration.getBody())
            currentStm.accept(this);


        return (null);
    }

    @Override
    public Void visit(PatternDeclaration patternDeclaration){
        VarItem varItem = new VarItem(patternDeclaration.getTargetVariable());
        try {
            SymbolTable.top.put(varItem);
        }
        catch (ItemAlreadyExists ignored) {}

       String patternName= patternDeclaration.getTargetVariable().getName();
        String secondPatternName= patternDeclaration.getPatternName().getName();

        if(patternName.equals(secondPatternName))
            nameErrors.add(new IdenticalArgPatternName(patternDeclaration.getLine(), patternDeclaration.getPatternName().getName()));

        for(Expression currentCon : patternDeclaration.getConditions())
            currentCon.accept(this);

        for(Expression returnExp : patternDeclaration.getReturnExp())
            returnExp.accept(this);

        return (null);
    }

    @Override
    public Void visit(MainDeclaration mainDeclaration){
        for (Statement currentStm : mainDeclaration.getBody())
            currentStm.accept(this);

        return (null);
    }

    @Override
    public Void visit(ReturnStatement returnStatement){
        boolean hasRetExpr= returnStatement.hasRetExpression();

        if (hasRetExpr)
            returnStatement.getReturnExp().accept(this);

        return (null);
    }

    @Override
    public Void visit(IfStatement ifStatement){
        for(Expression currentCond : ifStatement.getConditions())
            currentCond .accept(this);



        createTempTable();

        for(Statement currentThenStmt : ifStatement.getThenBody())
            currentThenStmt.accept(this);

        SymbolTable.pop();

        createTempTable();
        for(Statement currentElseStmt : ifStatement.getElseBody()){
            currentElseStmt.accept(this);
        }

        SymbolTable.pop();

        return (null);
    }

    @Override
    public Void visit(PutStatement putStatement){
        putStatement.getExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(LenStatement lenStatement){
        lenStatement.getExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(PushStatement pushStatement){
        pushStatement.getInitial().accept(this);
        pushStatement.getToBeAdded().accept(this);

        return (null);
    }



    private void createTempTable(){
        SymbolTable tempTable = SymbolTable.top.getCurrentSymbolTable();
        SymbolTable.push(tempTable);
    }

    @Override
    public Void visit(LoopDoStatement loopDoStatement){ 

        createTempTable();


        for(Expression currentCon : loopDoStatement.getLoopConditions())
            currentCon.accept(this);

        for(Statement currentStm : loopDoStatement.getLoopBodyStmts())
            currentStm.accept(this);

        ReturnStatement loopRetStm=(loopDoStatement.getLoopRetStmt());

        if ( loopRetStm != null)
            loopRetStm.accept(this);

        SymbolTable.pop();

        return (null);
    }

    @Override
    public Void visit(ForStatement forStatement){
        
        for(Expression range : forStatement.getRangeExpressions())
            range.accept(this);


        createTempTable();

        try {
            SymbolTable.top.put(new VarItem(forStatement.getIteratorId()));
        }
        catch (ItemAlreadyExists ignored) {}

        for(Expression currentCon : forStatement.getLoopBodyExpressions())
            currentCon.accept(this);

        for(Statement currentStm : forStatement.getLoopBody())
            currentStm.accept(this);


        SymbolTable.pop();

        return (null);
    }

    @Override
    public Void visit(MatchPatternStatement matchPatternStatement){
        matchPatternStatement.getMatchArgument().accept(this);

        return (null);
    }

    @Override
    public Void visit(ChopStatement chopStatement){
        chopStatement.getChopExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(ChompStatement chompStatement){
        chompStatement.getChompExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(AssignStatement assignStatement){
        VarItem varItem = new VarItem(assignStatement.getAssignedId());
        try {
            SymbolTable.top.put(varItem);
        } catch (ItemAlreadyExists ignored){}
        if(assignStatement.isAccessList())
            assignStatement.getAccessListExpression().accept(this);
        assignStatement.getAssignExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(ExpressionStatement expressionStatement){
        expressionStatement.getExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(AppendExpression appendExpression){
        appendExpression.getAppendee().accept(this);
        for(Expression currentApend : appendExpression.getAppendeds())
            currentApend.accept(this);

        return (null);
    }

    @Override
    public Void visit(BinaryExpression binaryExpression){
        binaryExpression.getFirstOperand().accept(this);
        binaryExpression.getSecondOperand().accept(this);


        return (null);
    }

    @Override
    public Void visit(UnaryExpression unaryExpression){
        unaryExpression.getExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(AccessExpression accessExpression){
        if(accessExpression.isFunctionCall() && accessExpression.getAccessedExpression() instanceof Identifier id){
            try {
                if(SymbolTable.root.getItem("Function:" + id.getName()) instanceof FunctionItem functionItem){
                    int defaultArgCounter = 0;
                    for(VarDeclaration arg : functionItem.getFunctionDeclaration().getArgs()){
                        if(arg.getDefaultVal() != null) defaultArgCounter++;
                    }
                    int differenceOfArgs = functionItem.getFunctionDeclaration().getArgs().size() -
                                                                accessExpression.getArguments().size();
                    if(differenceOfArgs > defaultArgCounter || differenceOfArgs < 0){
                        nameErrors.add(new ArgMisMatch(accessExpression.getLine(), functionItem.getName()));
                    }
                }
                else{
                    nameErrors.add(new FunctionNotDeclared(accessExpression.getLine(),
                        id.getName()));
                }
            } catch (ItemNotFound e) {
                nameErrors.add(new FunctionNotDeclared(accessExpression.getLine(),
                        id.getName()));
            }

        }
        else if (accessExpression.isFunctionCall() && accessExpression.getAccessedExpression() instanceof LambdaExpression lambdaExpression){
            int defaultArgCounter = 0;
            for(VarDeclaration arg : lambdaExpression.getDeclarationArgs()){
                if(arg.getDefaultVal() != null)
                    defaultArgCounter++;
            }
            int differenceOfArgs = lambdaExpression.getDeclarationArgs().size() -
                                                        accessExpression.getArguments().size();
            if(differenceOfArgs > defaultArgCounter || differenceOfArgs < 0){
                nameErrors.add(new ArgMisMatch(accessExpression.getLine(), "lambda"));
            }
        }
        else if(!accessExpression.isFunctionCall() && accessExpression.getAccessedExpression() instanceof Identifier id){
            id.accept(this);
        }

        for(Expression arg : accessExpression.getArguments()){
            arg.accept(this);
        }

        for(Expression dimAccess : accessExpression.getDimentionalAccess()){
            dimAccess.accept(this);
        }

        return (null);
    }

//    @Override
//    public Void visit(AccessExpression accessExpression) {
//
//        if (accessExpression.isFunctionCall())
//            handleFunctionCall(accessExpression);
//        else
//            accessExpression.getAccessedExpression().accept(this);
//
//
//        for (Expression currentExpr : accessExpression.getDimentionalAccess())
//            currentExpr.accept(this);
//
//        for (Expression Expr : accessExpression.getArguments())
//            Expr.accept(this);
//
//        return (null);
//    }
//
//    private void handleFunctionCall(AccessExpression accessExpression) {
//        String idName="";
//        try {
//            if (accessExpression.getAccessedExpression() instanceof Identifier id) {
//                idName=id.getName();
//                FunctionItem functionItem = (FunctionItem) SymbolTable.top.getItem( "Function:" + idName);
//
//                int argCounter = 0;
//                    for(VarDeclaration currentArgum : functionItem.getFunctionDeclaration().getArgs()){
//                        if(currentArgum.getDefaultVal() != null)
//                            argCounter++;
//                    }
//                    int argDiff = functionItem.getFunctionDeclaration().getArgs().size() - accessExpression.getArguments().size();
//                    if(argDiff > argCounter || argDiff < 0)
//                        nameErrors.add(new ArgMisMatch(accessExpression.getLine(), functionItem.getName()));
//
//            } else
//                throw new ItemNotFound();
//
//        }
//        catch (ItemNotFound ignored) {
//            nameErrors.add(
//                    new FunctionNotDeclared(
//                            accessExpression.getLine(),
//                           idName
//                    )
//            );
//        }
//    }

    @Override
    public Void visit(LambdaExpression lambdaExpression){
        SymbolTable.push(SymbolTable.top.getCurrentSymbolTable());
        for(VarDeclaration currentArg : lambdaExpression.getDeclarationArgs())
            currentArg.accept(this);

        for(Statement currentStm : lambdaExpression.getBody())
            currentStm.accept(this);

        SymbolTable.pop();

        return (null);
    }

    @Override
    public Void visit(ListValue listValue){
        for(Expression currentExpr : listValue.getElements())
            currentExpr.accept(this);

        return (null);
    }
    @Override
    public Void visit(FunctionPointer functionPointer){
        try{
            if(!(SymbolTable.root.getItem("Function:" + functionPointer.getId().getName()) instanceof FunctionItem)){
                nameErrors.add(new FunctionNotDeclared(functionPointer.getLine(),
                        functionPointer.getId().getName()));
            }
        }
        catch(ItemNotFound e){
            nameErrors.add(new FunctionNotDeclared(functionPointer.getLine(),
                        functionPointer.getId().getName()));
        }
        return (null);
    }

}