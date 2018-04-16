package parser.typechecker

import parser.AstNode
import parser.symboltable.ISymbolTable

interface ITypeChecker: ISymbolTable {
    fun getTypeOfExpression(expr: AstNode.Command.Expression): ExprResult
    fun checkExpressionTypeMatchesSymbolType(expr: AstNode.Command.Expression, symbol: String): Boolean
    fun checkExpressionTypesMatch(expr1: AstNode.Command.Expression, expr2: AstNode.Command.Expression): Boolean
    val AstNode.Command.Expression.type: ExprResult
    fun List<AstNode.Type.Func.ExplicitFunc>.getTypeDeclaration(types: List<AstNode.Type>):
            AstNode.Type.Func.ExplicitFunc?
}