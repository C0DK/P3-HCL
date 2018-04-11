package parser

import exceptions.*
import parser.typechecker.ITypeChecker
import parser.typechecker.TypeChecker
import lexer.ILexer
import lexer.PositionalToken
import lexer.Token
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import utils.BufferedLaabStream
import utils.IBufferedLaabStream

class Parser(val lexer: ILexer): IParser, ITypeChecker by TypeChecker(),
        IBufferedLaabStream<PositionalToken> by BufferedLaabStream(lexer.getTokenSequence()) {
    override fun generateAbstractSyntaxTree() = AbstractSyntaxTree().apply {
        while (hasNext()) {
            if (current.token != Token.SpecialChar.EndOfLine) {
                children.add(parseCommand())
            }
        }
    }

    private fun parseCommand(): TreeNode.Command {
        val command = when (current.token) {
            is Token.Type -> parseDeclaration()
            is Token.Identifier -> {
                if (peek().token is Token.SpecialChar.Equals) {
                    parseAssignment()
                }
                else parseExpression()
            }
            is Token.Literal, //Fallthrough
            is Token.SpecialChar.BlockStart,
            is Token.SpecialChar.SquareBracketStart,
            is Token.SpecialChar.ParenthesesStart
                -> parseExpression()
            is Token.Return -> parseReturnStatement()
            else -> throw UnexpectedTokenError(current.lineNumber, current.lineIndex,
                                               lexer.inputLine(current.lineNumber), current.token)
        }
        acceptEndOfLines()
        return command
    }

    private fun parseReturnStatement(): TreeNode.Command.Return {
        accept<Token.Return>()
        val expression = parseExpression()
        return TreeNode.Command.Return(expression)
    }

    private inline fun<reified T: Token> accept(): T {
        val token = current.token
        val currentLineNumber = current.lineNumber
        val currentIndex = current.lineIndex
        moveNext()
        if (token is T) {
            return token
        } else {
            throw WrongTokenTypeError(currentLineNumber, currentIndex,
                    lexer.inputLine(currentLineNumber), T::class.simpleName, token)
        }
    }

    private inline fun<reified T> tryAccept(): Boolean {
        val token = current.token
        val result = token is T
        if(result)
            moveNext()
        return result
    }

    private fun acceptEndOfLines() {
        if (current.token != Token.SpecialChar.EndOfLine && peek().token == Token.SpecialChar.EndOfLine)
            moveNext()
        while (current.token == Token.SpecialChar.EndOfLine && hasNext())
            accept<Token.SpecialChar.EndOfLine>()

    }

    private fun acceptIdentifier() =
            TreeNode.Command.Expression.Value.Identifier(accept<Token.Identifier>().value)

    private fun acceptLiteral(): TreeNode.Command.Expression.Value.Literal {
        val litToken = accept<Token.Literal>()
        return when (litToken) {
            is Token.Literal.Number -> TreeNode.Command.Expression.Value.Literal.Number(litToken.value)
            is Token.Literal.Text -> TreeNode.Command.Expression.Value.Literal.Text(litToken.value)
            is Token.Literal.Bool -> TreeNode.Command.Expression.Value.Literal.Bool(litToken.value)
        }
    }

    private fun parseSingleParameter(): TreeNode.ParameterDeclaration {
        val type = parseType()
        val identifier = acceptIdentifier()
        if (current.token == Token.SpecialChar.Equals)
            throw InitializedFunctionParameterError(current.lineNumber,
                    current.lineIndex,
                    lexer.inputLine(current.lineNumber))
        return TreeNode.ParameterDeclaration(type, identifier)
    }

    private fun parseFunctionParameters(): List<TreeNode.ParameterDeclaration> {
        val parameters = mutableListOf<TreeNode.ParameterDeclaration>()

        accept<Token.SpecialChar.ParenthesesStart>()
        while (current.token != Token.SpecialChar.ParenthesesEnd) {
            parameters.add(parseSingleParameter())
            if (!tryAccept<Token.SpecialChar.ListSeparator>()) break
        }
        accept<Token.SpecialChar.ParenthesesEnd>()
        return parameters
    }

    private fun parseDeclaration(): TreeNode.Command.Declaration {
        val type = parseType(implicitAllowed = true)
        val identifier = acceptIdentifier()
        enterSymbol(identifier.name, type)
        val expression = if (current.token == Token.SpecialChar.Equals) {
            moveNext()
            parseExpression()
        } else null
        if (expression != null) {
            if (!checkExpressionTypeMatchesSymbolType(expression, identifier.name))
                throw UnexpectedTypeError(current.lineNumber, current.lineIndex, lexer.inputLine(current.lineNumber),
                                          type.toString(), getTypeOfExpression(expression).toString())
        }
        return TreeNode.Command.Declaration(type, identifier, expression)
    }

    private fun parseAssignment(): TreeNode.Command.Assignment {
        val identifier = acceptIdentifier()
        accept<Token.SpecialChar.Equals>()
        val expression = parseExpression()
        if (!checkExpressionTypeMatchesSymbolType(expression, identifier.name))
            throw UnexpectedTypeError(current.lineNumber, current.lineIndex, lexer.inputLine(current.lineNumber),
                                      retrieveSymbol(identifier.name).identifier.toString(),
                                      getTypeOfExpression(expression).toString())
        return TreeNode.Command.Assignment(identifier, expression)
    }

    //region Type declarations
    private fun  parseType(implicitAllowed: Boolean = false): TreeNode.Type {
        val currentPosToken = current
        moveNext()
        return when (currentPosToken.token) {
            is Token.Type.Number -> TreeNode.Type.Number
            is Token.Type.Text -> TreeNode.Type.Text
            is Token.Type.Bool -> TreeNode.Type.Bool
            is Token.Type.Func -> parseFuncType(implicitAllowed)
            is Token.Type.Tuple -> parseTupleType()
            is Token.Type.List -> parseListType()
            else -> {
                if(implicitAllowed && tryAccept<Token.Type.Var>())
                    throw NotImplementedException()
                else
                    throw UnexpectedTokenError(currentPosToken, lexer.inputLine(current.lineNumber))
            }
        }
    }

    private fun parseTypes(parsingMethod:()->TreeNode.Type): List<TreeNode.Type>{
        val elementTypes = mutableListOf<TreeNode.Type>()

        while (true) {
            elementTypes.add(parsingMethod())
            if(!tryAccept<Token.SpecialChar.ListSeparator>())
                return elementTypes
        }
    }



    private fun parseFuncType(implicitAllowed: Boolean): TreeNode.Type.Func {
        return if (current.token == Token.SpecialChar.SquareBracketStart) {
            accept<Token.SpecialChar.SquareBracketStart>()

            val parameters = parseTypes({
                if (peek().token == Token.SpecialChar.SquareBracketEnd && tryAccept<Token.Type.None>())
                    TreeNode.Type.None
                else parseType()
            })
            accept<Token.SpecialChar.SquareBracketEnd>()
            val returnType = parameters.last()


            TreeNode.Type.Func.ExplicitFunc(parameters.dropLast(1), returnType)
        }
        else
        {
            if(implicitAllowed)
                TreeNode.Type.Func.ImplicitFunc
            else
                throw ImplicitTypeNotAllowed(current.lineNumber, current.lineIndex, lexer.inputLine(current.lineNumber))
        }
    }

    private fun parseLambdaDeclaration(): TreeNode.Command.Expression.LambdaExpression {
        val parameters = parseFunctionParameters()
        accept<Token.SpecialChar.Colon>()
        val returnType = if(current.token == Token.Type.None){
            moveNext()
            TreeNode.Type.None
        }else parseType()

        acceptEndOfLines()
        val body = parseLambdaBody()
        return TreeNode.Command.Expression.LambdaExpression(parameters, returnType, body)
    }

    private fun parseLambdaBody(): List<TreeNode.Command> {
        val commands = mutableListOf<TreeNode.Command>()
        accept<Token.SpecialChar.BlockStart>()
        while (current.token != Token.SpecialChar.BlockEnd) {
            commands.add(parseCommand())
        }
        accept<Token.SpecialChar.BlockEnd>()
        return commands
    }

    private fun parseTupleType(): TreeNode.Type.Tuple {
        accept<Token.SpecialChar.SquareBracketStart>()
        val elementTypes =parseTypes({parseType()})
        accept<Token.SpecialChar.SquareBracketEnd>()
        return TreeNode.Type.Tuple(elementTypes)
    }

    private fun parseListType(): TreeNode.Type.List {
        accept<Token.SpecialChar.SquareBracketStart>()
        val elementType: TreeNode.Type = parseType()
        accept<Token.SpecialChar.SquareBracketEnd>()
        return TreeNode.Type.List(elementType)
    }
//endregion

    //region ExpressionParsing
    private fun parseExpression(): TreeNode.Command.Expression {
        // Be aware that below is not correct for the full implementation. Here we expect that if there is only one token
        // the token will be a literal, but it could also be an identifier.
        when(current.token) {
            Token.SpecialChar.SquareBracketStart -> return parseListDeclaration()
            Token.SpecialChar.ParenthesesStart -> {
                if (peek().token is Token.Type || peek().token == Token.SpecialChar.ParenthesesEnd) {
                    return parseLambdaDeclaration()
                }
                else {
                    var counter = 1
                    while (true) {
                        if (lookAhead(counter).token == Token.SpecialChar.ListSeparator) return parseTupleExpression()
                        else if (lookAhead(counter++).token == Token.SpecialChar.ParenthesesEnd) break
                    }
                }
            }
            is Token.Literal.Text -> {
                return if (isLiteral(peek().token)) acceptLiteral()
                else parseFunctionCall()
            }
            is Token.Literal.Bool -> {
                return if (isLiteral(peek().token)) acceptLiteral()
                else parseFunctionCall()
            }
            is Token.Literal.Number -> {
                return if (isLiteral(peek().token)) acceptLiteral()
                else parseFunctionCall()
            }
            is Token.Identifier -> {
                val identifier = retrieveSymbol(current.token.value).handle(
                        { TreeNode.Command.Expression.FunctionCall(
                                TreeNode.Command.Expression.Value.Identifier(current.token.value),
                                listOf()
                        )
                        },
                        { it },
                        { throw Exception("Not declared identifier") }
                )
                return if (identifier is TreeNode.Type) TreeNode.Command.Expression.Value.Identifier(current.token.value)
                else parseFunctionCall()

            }
            else ->TODO ("Make a function call get and identifier ")
        }
        throw Exception("Unrecognized expression")
    }

    private fun isLiteral(token: Token): Boolean {
        if (token == Token.SpecialChar.EndOfLine || token == Token.SpecialChar.ListSeparator
                || token == Token.SpecialChar.ParenthesesEnd) return true
        return false
    }

    private fun parseTupleExpression(): TreeNode.Command.Expression {
        val elements = mutableListOf<TreeNode.Command.Expression>()
        moveNext()
        while (true){
            when(current.token) {
                is Token.Literal -> elements.add(acceptLiteral())
                is Token.Identifier -> elements.add(acceptIdentifier())
                else -> throw UnexpectedTokenError(current.lineNumber, current.lineIndex,
                        lexer.inputLine(current.lineNumber), current.token)
            }
            if (current.token == Token.SpecialChar.ListSeparator) moveNext()
            else break
        }
        if (current.token != Token.SpecialChar.ParenthesesEnd)
            throw WrongTokenTypeError(current.lineNumber, current.lineIndex, lexer.inputLine(current.lineNumber),
                    Token.SpecialChar.ParenthesesEnd::class.simpleName, current.token)
        moveNext()
        return TreeNode.Command.Expression.Value.Literal.Tuple(elements)
    }

    private fun parseListDeclaration(): TreeNode.Command.Expression {
        val elements = mutableListOf<TreeNode.Command.Expression>()
        moveNext()
        while (true) {
            when(current.token) {
                is Token.Literal -> elements.add(acceptLiteral())
                is Token.Identifier -> elements.add(acceptIdentifier())
                else -> throw UnexpectedTokenError(current.lineNumber, current.lineIndex,
                                                   lexer.inputLine(current.lineNumber), current.token)
            }
            if (current.token == Token.SpecialChar.ListSeparator) moveNext()
            else break
        }
        if (current.token != Token.SpecialChar.SquareBracketEnd)
            throw WrongTokenTypeError(current.lineNumber, current.lineIndex, lexer.inputLine(current.lineNumber),
                    Token.SpecialChar.SquareBracketEnd::class.simpleName, current.token)
        moveNext()
        return TreeNode.Command.Expression.Value.Literal.List(elements)
    }

    private fun parseFunctionCall(): TreeNode.Command.Expression.FunctionCall {
        TODO()
    }

    //endregion ExpressionParsing
}
