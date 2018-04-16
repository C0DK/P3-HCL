package parser.typechecker

import exceptions.typeCheckerExceptions.UndeclaredIdentifierError
import parser.AstNode.Command.Expression
import parser.AstNode
import parser.symboltable.ISymbolTable
import parser.symboltable.SymbolTable

class TypeChecker: ITypeChecker, ISymbolTable by SymbolTable() {

    override fun checkExpressionTypeMatchesSymbolType(expr: Expression, symbol: String) =
            retrieveSymbol(symbol).handle({ true }, { it == expr.type }, { false })

    override fun checkExpressionTypesMatch(expr1: Expression, expr2: Expression) = expr1.type == expr2.type
    //TODO(delete checkExpressionTypesMatch if never used)

    override fun getTypeOfExpression(expr: Expression): AstNode.Type = when (expr) {
        is Expression.Value.Literal.Number -> AstNode.Type.Number
        is Expression.Value.Literal.Bool -> AstNode.Type.Bool
        is Expression.Value.Literal.List -> AstNode.Type.List(getTypeOfListExpression(expr))
        is Expression.Value.Literal.Text -> AstNode.Type.Text
        is Expression.Value.Literal.Tuple -> AstNode.Type.Tuple(getTypeOfTupleExpression(expr))
        is Expression.Value.Identifier -> retrieveSymbol(expr.name).handle(
                // todo get correct type and not just first
                { it.first() },
                { it },
                { throw UndeclaredIdentifierError(expr.name) }
        )
        is Expression.FunctionCall -> {
            val functionDeclarationsSymbol = retrieveSymbol(expr.identifier.name)
            if (!functionDeclarationsSymbol.isFunctions)
                throw UndeclaredIdentifierError(expr.identifier.name)
            val functionDeclarations = functionDeclarationsSymbol.functions
            val argumentTypes = expr.arguments.map { getTypeOfExpression(it) }
            val functionDeclaration = functionDeclarations.getTypeDeclaration(argumentTypes)
                    ?: throw UndeclaredIdentifierError(expr.identifier.name)
            functionDeclaration.returnType
        }
        is Expression.LambdaExpression -> AstNode.Type.Func.ExplicitFunc(
                expr.paramDeclarations.map { it.type },
                expr.returnType
        )
    }

    private fun getTypeOfListExpression(list: Expression.Value.Literal.List): AstNode.Type {
        return getTypeOfExpression(list.elements[0])
    }

    private fun getTypeOfTupleExpression(tuple: Expression.Value.Literal.Tuple) =
            tuple.elements.map { it.type }

    override fun List<AstNode.Type.Func.ExplicitFunc>.getTypeDeclaration(types: List<AstNode.Type>) =
            this.firstOrNull{ it.paramTypes == types }

    override val AstNode.Command.Expression.type get() = getTypeOfExpression(this)
}
