package main.visitor.nameAnalyzer;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.FunctionDeclaration;
import main.ast.nodes.expression.AccessExpression;
import main.ast.nodes.expression.AppendExpression;
import main.ast.nodes.expression.BinaryExpression;
import main.ast.nodes.expression.ChompStatement;
import main.ast.nodes.expression.ChopStatement;
import main.ast.nodes.expression.Expression;
import main.ast.nodes.expression.Identifier;
import main.ast.nodes.expression.LambdaExpression;
import main.ast.nodes.expression.LenStatement;
import main.ast.nodes.expression.MatchPatternStatement;
import main.ast.nodes.expression.UnaryExpression;
import main.ast.nodes.expression.value.ListValue;
import main.ast.nodes.statement.AssignStatement;
import main.ast.nodes.statement.ExpressionStatement;
import main.ast.nodes.statement.ForStatement;
import main.ast.nodes.statement.IfStatement;
import main.ast.nodes.statement.LoopDoStatement;
import main.ast.nodes.statement.PushStatement;
import main.ast.nodes.statement.PutStatement;
import main.ast.nodes.statement.ReturnStatement;
import main.ast.nodes.statement.Statement;
import main.compileError.CompileError;
import main.compileError.nameErrors.CircularDependency;
import main.visitor.Visitor;
import main.symbolTable.utils.Graph;

import java.util.ArrayList;
import java.util.List;

public class DependencyDetector extends Visitor<Void> {
    public ArrayList<CompileError> dependencyError = new ArrayList<>();
    private final Graph dependencyGraph = new Graph();
    private String currScope = null;


    @Override
    public Void visit(AccessExpression accessExpression){
        if(accessExpression.isFunctionCall() && accessExpression.getAccessedExpression() instanceof Identifier id)
            dependencyGraph.addEdge(currScope, id.getName());

        for(Expression currentDim : accessExpression.getDimentionalAccess())
            currentDim.accept(this);

        for(Expression currentArg : accessExpression.getArguments())
            currentArg.accept(this);




        return null;
    }

    @Override
    public Void visit(AssignStatement assignStatement){
        if(assignStatement.isAccessList())
            assignStatement.getAccessListExpression().accept(this);
        assignStatement.getAssignExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(AppendExpression appendExpression){
        appendExpression.getAppendee().accept(this);
        for(Expression currentApp : appendExpression.getAppendeds())
            currentApp.accept(this);

        return (null);
    }
    @Override
    public Void visit(BinaryExpression binaryExpression){
        binaryExpression.getFirstOperand().accept(this);
        binaryExpression.getSecondOperand().accept(this);
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
    public Void visit(ExpressionStatement expressionStatement){
        expressionStatement.getExpression().accept(this);
        return (null);
    }

    @Override
    public Void visit(FunctionDeclaration functionDeclaration){
        for (Statement currentStm : functionDeclaration.getBody())
            currentStm.accept(this);


        return (null);
    }

    @Override
    public Void visit(ForStatement forStatement){

        for(Statement currentStm : forStatement.getLoopBody())
            currentStm.accept(this);


        for(Expression currange : forStatement.getRangeExpressions())
            currange.accept(this);


        for(Expression currentCon : forStatement.getLoopBodyExpressions())
            currentCon.accept(this);



        return (null);
    }

    @Override
    public Void visit(LoopDoStatement loopDoStatement){

        for(Statement currentStm : loopDoStatement.getLoopBodyStmts())
            currentStm.accept(this);


        for(Expression currentCon : loopDoStatement.getLoopConditions())
            currentCon.accept(this);


        if(loopDoStatement.getLoopRetStmt() != null)
            loopDoStatement.getLoopRetStmt().accept(this);
        return null;
    }

    @Override
    public Void visit(LenStatement lenStatement){
        lenStatement.getExpression().accept(this);
        return null;
    }


    public void findDependency(){
        ArrayList<List<String>> cycles = dependencyGraph.findCycles();
        for(List<String> cycle : cycles)
            dependencyError.add(new CircularDependency(cycle));

    }


    @Override
    public Void visit(IfStatement ifStatement){



        for(Statement elseStm : ifStatement.getElseBody())
            elseStm.accept(this);


        for(Statement thenStm : ifStatement.getThenBody())
            thenStm.accept(this);



        for(Expression curCon : ifStatement.getConditions())
            curCon.accept(this);





        return (null);
    }


    @Override
    public Void visit(ReturnStatement returnStatement){
        if (returnStatement.hasRetExpression())
            returnStatement.getReturnExp().accept(this);

        return (null);
    }

    @Override
    public Void visit(Program program){
        for(FunctionDeclaration functionDeclaration : program.getFunctionDeclarations()){
            currScope = functionDeclaration.getFunctionName().getName();
            functionDeclaration.accept(this);
        }
        currScope = (null);
        return (null);
    }

    @Override
    public Void visit(PutStatement putStatement){
        putStatement.getExpression().accept(this);

        return (null);
    }

    @Override
    public Void visit(PushStatement pushStatement){

        pushStatement.getInitial().accept(this);

        pushStatement.getToBeAdded().accept(this);


        return (null);
    }



    @Override
    public Void visit(MatchPatternStatement matchPatternStatement){
        matchPatternStatement.getMatchArgument().accept(this);

        return (null);
    }





    @Override
    public Void visit(ListValue listValue){
        for(Expression currentExp : listValue.getElements())
            currentExp.accept(this);

        return (null);
    }



    @Override
    public Void visit(LambdaExpression lambdaExpression){
        
        for(Statement currentStm : lambdaExpression.getBody())
            currentStm.accept(this);

        return (null);
    }


    @Override
    public Void visit(UnaryExpression unaryExpression){
        unaryExpression.getExpression().accept(this);

        return (null);
    }


   

}
