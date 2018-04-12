package parserTests

import com.natpryce.hamkrest.assertion.assertThat
import exceptions.ImplicitTypeNotAllowed
import exceptions.WrongTokenTypeError
import lexer.Token
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import parser.Parser
import parser.TreeNode
import sun.reflect.generics.tree.Tree
import kotlin.coroutines.experimental.buildSequence

class FunctionCallTests {
    @Test
    fun canParseFunctionCallWithoutParameters() {
        assertThat(
                listOf(
                        Token.Type.Func,
                        Token.SpecialChar.SquareBracketStart,
                        Token.Type.Text,
                        Token.SpecialChar.SquareBracketEnd,
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.Equals,
                        Token.SpecialChar.ParenthesesStart,
                        Token.SpecialChar.ParenthesesEnd,
                        Token.SpecialChar.Colon,
                        Token.Type.Text,
                        Token.SpecialChar.BlockStart,
                        Token.SpecialChar.BlockEnd,
                        Token.SpecialChar.EndOfLine,
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.EndOfLine
                ),
                matchesAstChildren(
                        TreeNode.Command.Declaration(
                                TreeNode.Type.Func.ExplicitFunc(
                                        listOf(),
                                        TreeNode.Type.Text
                                ),
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                TreeNode.Command.Expression.LambdaExpression(
                                        listOf(),
                                        TreeNode.Type.Text,
                                        listOf()
                                )
                        ),
                        TreeNode.Command.Expression.FunctionCall(
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                listOf()
                        )
                )
        )
    }

    @Test
    fun canParseFunctionCallOneParameter() {
        assertThat(
                listOf(
                        Token.Type.Func,
                        Token.SpecialChar.SquareBracketStart,
                        Token.Type.Number,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.SpecialChar.SquareBracketEnd,
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.Equals,
                        Token.SpecialChar.ParenthesesStart,
                        Token.Type.Number,
                        Token.Identifier("myParam"),
                        Token.SpecialChar.ParenthesesEnd,
                        Token.SpecialChar.Colon,
                        Token.Type.Text,
                        Token.SpecialChar.BlockStart,
                        Token.SpecialChar.BlockEnd,
                        Token.SpecialChar.EndOfLine,
                        Token.Literal.Number(5.0),
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.EndOfLine
                ),
                matchesAstChildren(
                        TreeNode.Command.Declaration(
                                TreeNode.Type.Func.ExplicitFunc(
                                        listOf(TreeNode.Type.Number),
                                        TreeNode.Type.Text
                                ),
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                TreeNode.Command.Expression.LambdaExpression(
                                        listOf(
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Number,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam"))
                                        ),
                                        TreeNode.Type.Text,
                                        listOf()
                                )
                        ),
                        TreeNode.Command.Expression.FunctionCall(
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                listOf(TreeNode.Command.Expression.Value.Literal.Number(5.0)
                                )
                        )
                )
        )
    }

    @Test
    fun failsParseFunctionCallNeedsOneArgumentButGetsZero() {
        val lexer = DummyLexer(listOf(
                Token.Type.Func,
                Token.SpecialChar.SquareBracketStart,
                Token.Type.Number,
                Token.SpecialChar.ListSeparator,
                Token.Type.Text,
                Token.SpecialChar.SquareBracketEnd,
                Token.Identifier("myFunc"),
                Token.SpecialChar.Equals,
                Token.SpecialChar.ParenthesesStart,
                Token.Type.Number,
                Token.Identifier("myParam"),
                Token.SpecialChar.ParenthesesEnd,
                Token.SpecialChar.Colon,
                Token.Type.Text,
                Token.SpecialChar.BlockStart,
                Token.SpecialChar.BlockEnd,
                Token.SpecialChar.EndOfLine,
                Token.Identifier("myFunc"),
                Token.SpecialChar.EndOfLine
        ))
        assertThrows(Exception::class.java) { Parser(lexer).generateAbstractSyntaxTree() }
    }

    @Test
    fun failsParseFunctionCallNeedsOneArgumentButGetsTwo() {
        val lexer = DummyLexer(listOf(
                Token.Type.Func,
                Token.SpecialChar.SquareBracketStart,
                Token.Type.Number,
                Token.SpecialChar.ListSeparator,
                Token.Type.Text,
                Token.SpecialChar.SquareBracketEnd,
                Token.Identifier("myFunc"),
                Token.SpecialChar.Equals,
                Token.SpecialChar.ParenthesesStart,
                Token.Type.Number,
                Token.Identifier("myParam"),
                Token.SpecialChar.ParenthesesEnd,
                Token.SpecialChar.Colon,
                Token.Type.Text,
                Token.SpecialChar.BlockStart,
                Token.SpecialChar.BlockEnd,
                Token.SpecialChar.EndOfLine,
                Token.Literal.Number(5.0),
                Token.Identifier("myFunc"),
                Token.Literal.Number(5.0),
                Token.SpecialChar.EndOfLine
        ))
        assertThrows(WrongTokenTypeError::class.java) { Parser(lexer).generateAbstractSyntaxTree() }
    }

    @Test
    fun failsToParseFunctionCallWith_FunctionCallWithArguments_AsRightHandSideArgumentWithoutParentheses() {
        val lexer = DummyLexer(listOf(
                Token.Type.Func,
                Token.SpecialChar.SquareBracketStart,
                Token.Type.Number,
                Token.SpecialChar.ListSeparator,
                Token.Type.Number,
                Token.SpecialChar.ListSeparator,
                Token.Type.Number,
                Token.SpecialChar.SquareBracketEnd,
                Token.Identifier("myFunc"),
                Token.SpecialChar.Equals,
                Token.SpecialChar.ParenthesesStart,
                Token.Type.Number,
                Token.Identifier("myParam1"),
                Token.SpecialChar.ListSeparator,
                Token.Type.Number,
                Token.Identifier("myParam2"),
                Token.SpecialChar.ParenthesesEnd,
                Token.SpecialChar.Colon,
                Token.Type.Number,
                Token.SpecialChar.BlockStart,
                Token.SpecialChar.BlockEnd,
                Token.SpecialChar.EndOfLine,
                Token.Literal.Number(5.0),
                Token.Identifier("myFunc"),
                Token.Identifier("myFunc"),
                Token.Literal.Number(5.0),
                Token.SpecialChar.EndOfLine
        ))
        assertThrows(Exception::class.java) { Parser(lexer).generateAbstractSyntaxTree() }
    }

    @Test
    fun canParseFunctionCallWith_FunctionCallWithoutArguments_AsRightHandSideArgumentWithoutParentheses() {
        assertThat(
                listOf(
                        Token.Type.Func,
                        Token.SpecialChar.SquareBracketStart,
                        Token.Type.Number,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Number,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Number,
                        Token.SpecialChar.SquareBracketEnd,
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.Equals,
                        Token.SpecialChar.ParenthesesStart,
                        Token.Type.Number,
                        Token.Identifier("myParam1"),
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Number,
                        Token.Identifier("myParam2"),
                        Token.SpecialChar.ParenthesesEnd,
                        Token.SpecialChar.Colon,
                        Token.Type.Number,
                        Token.SpecialChar.BlockStart,
                        Token.SpecialChar.BlockEnd,
                        Token.SpecialChar.EndOfLine,
                        Token.Type.Func,
                        Token.SpecialChar.SquareBracketStart,
                        Token.Type.Number,
                        Token.SpecialChar.SquareBracketEnd,
                        Token.Identifier("myFunc2"),
                        Token.SpecialChar.Equals,
                        Token.SpecialChar.ParenthesesStart,
                        Token.SpecialChar.ParenthesesEnd,
                        Token.SpecialChar.Colon,
                        Token.Type.Number,
                        Token.SpecialChar.BlockStart,
                        Token.SpecialChar.BlockEnd,
                        Token.SpecialChar.EndOfLine,
                        Token.Literal.Number(5.0),
                        Token.Identifier("myFunc"),
                        Token.Identifier("myFunc2"),
                        Token.SpecialChar.EndOfLine
                ),
                matchesAstChildren(
                        TreeNode.Command.Declaration(
                                TreeNode.Type.Func.ExplicitFunc(
                                        listOf(TreeNode.Type.Number, TreeNode.Type.Number),
                                        TreeNode.Type.Number
                                ),
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                TreeNode.Command.Expression.LambdaExpression(
                                        listOf(
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Number,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam1")),
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Number,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam2"))
                                        ),
                                        TreeNode.Type.Number,
                                        listOf()
                                )
                        ),
                        TreeNode.Command.Declaration(
                                TreeNode.Type.Func.ExplicitFunc(
                                        listOf(),
                                        TreeNode.Type.Number
                                ),
                                TreeNode.Command.Expression.Value.Identifier("myFunc2"),
                                TreeNode.Command.Expression.LambdaExpression(
                                        listOf(),
                                        TreeNode.Type.Number,
                                        listOf()
                                )
                        ),
                        TreeNode.Command.Expression.FunctionCall(
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                listOf(
                                        TreeNode.Command.Expression.Value.Literal.Number(5.0),
                                        TreeNode.Command.Expression.FunctionCall(
                                                TreeNode.Command.Expression.Value.Identifier("myFunc2"),
                                                listOf()
                                        )
                                )
                        )
                )
        )
    }

    @Test
    fun failsParseFunctionCallNeedsTwoArgumentButGetsThree() {
        val lexer = DummyLexer(listOf(
                Token.Type.Func,
                Token.SpecialChar.SquareBracketStart,
                Token.Type.Number,
                Token.SpecialChar.ListSeparator,
                Token.Type.Number,
                Token.SpecialChar.ListSeparator,
                Token.Type.Text,
                Token.SpecialChar.SquareBracketEnd,
                Token.Identifier("myFunc"),
                Token.SpecialChar.Equals,
                Token.SpecialChar.ParenthesesStart,
                Token.Type.Number,
                Token.Identifier("myParam1"),
                Token.SpecialChar.ListSeparator,
                Token.Type.Number,
                Token.Identifier("myParam2"),
                Token.SpecialChar.ParenthesesEnd,
                Token.SpecialChar.Colon,
                Token.Type.Text,
                Token.SpecialChar.BlockStart,
                Token.SpecialChar.BlockEnd,
                Token.SpecialChar.EndOfLine,
                Token.Literal.Number(5.0),
                Token.Identifier("myFunc"),
                Token.Literal.Number(5.0),
                Token.Literal.Text("hej"),
                Token.SpecialChar.EndOfLine
        ))
        assertThrows(WrongTokenTypeError::class.java) { Parser(lexer).generateAbstractSyntaxTree() }
    }

    @Test
    fun canParseTwoCallsWithOneParameterEach() {
        assertThat(
                listOf(
                        Token.Type.Func,
                        Token.SpecialChar.SquareBracketStart,
                        Token.Type.Number,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Number,
                        Token.SpecialChar.SquareBracketEnd,
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.Equals,
                        Token.SpecialChar.ParenthesesStart,
                        Token.Type.Number,
                        Token.Identifier("myParam"),
                        Token.SpecialChar.ParenthesesEnd,
                        Token.SpecialChar.Colon,
                        Token.Type.Number,
                        Token.SpecialChar.BlockStart,
                        Token.SpecialChar.BlockEnd,
                        Token.SpecialChar.EndOfLine,
                        Token.Literal.Number(5.0),
                        Token.Identifier("myFunc"),
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.EndOfLine
                ),
                matchesAstChildren(
                        TreeNode.Command.Declaration(
                                TreeNode.Type.Func.ExplicitFunc(
                                        listOf(TreeNode.Type.Number),
                                        TreeNode.Type.Number
                                ),
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                TreeNode.Command.Expression.LambdaExpression(
                                        listOf(
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Number,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam"))
                                        ),
                                        TreeNode.Type.Number,
                                        listOf()
                                )
                        ),
                        TreeNode.Command.Expression.FunctionCall(
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                listOf(
                                        TreeNode.Command.Expression.FunctionCall(
                                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                                listOf(TreeNode.Command.Expression.Value.Literal.Number(5.0))
                                        )
                                )
                        )
                )
        )
    }

    @Test
    fun canParseFunctionCallTwoParameters() {
        assertThat(
                listOf(
                        Token.Type.Func,
                        Token.SpecialChar.SquareBracketStart,
                        Token.Type.Number,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.SpecialChar.SquareBracketEnd,
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.Equals,
                        Token.SpecialChar.ParenthesesStart,
                        Token.Type.Number,
                        Token.Identifier("myParam1"),
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.Identifier("myParam2"),
                        Token.SpecialChar.ParenthesesEnd,
                        Token.SpecialChar.Colon,
                        Token.Type.Text,
                        Token.SpecialChar.BlockStart,
                        Token.SpecialChar.BlockEnd,
                        Token.SpecialChar.EndOfLine,
                        Token.Literal.Number(5.0),
                        Token.Identifier("myFunc"),
                        Token.Literal.Text("hej"),
                        Token.SpecialChar.EndOfLine
                ),
                matchesAstChildren(
                        TreeNode.Command.Declaration(
                                TreeNode.Type.Func.ExplicitFunc(
                                        listOf(TreeNode.Type.Number, TreeNode.Type.Text),
                                        TreeNode.Type.Text
                                ),
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                TreeNode.Command.Expression.LambdaExpression(
                                        listOf(
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Number,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam1")),
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Text,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam2"))
                                        ),
                                        TreeNode.Type.Text,
                                        listOf()
                                )
                        ),
                        TreeNode.Command.Expression.FunctionCall(
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                listOf(TreeNode.Command.Expression.Value.Literal.Number(5.0),
                                        TreeNode.Command.Expression.Value.Literal.Text("hej")
                                )
                        )
                )
        )
    }

    @Test
    fun canParseFunctionCallFourParameters() {
        assertThat(
                listOf(
                        Token.Type.Func,
                        Token.SpecialChar.SquareBracketStart,
                        Token.Type.Number,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.SpecialChar.SquareBracketEnd,
                        Token.Identifier("myFunc"),
                        Token.SpecialChar.Equals,
                        Token.SpecialChar.ParenthesesStart,
                        Token.Type.Number,
                        Token.Identifier("myParam1"),
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.Identifier("myParam2"),
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.Identifier("myParam3"),
                        Token.SpecialChar.ListSeparator,
                        Token.Type.Text,
                        Token.Identifier("myParam4"),
                        Token.SpecialChar.ParenthesesEnd,
                        Token.SpecialChar.Colon,
                        Token.Type.Text,
                        Token.SpecialChar.BlockStart,
                        Token.SpecialChar.BlockEnd,
                        Token.SpecialChar.EndOfLine,
                        Token.Literal.Number(5.0),
                        Token.Identifier("myFunc"),
                        Token.Literal.Text("hej"),
                        Token.Literal.Text("med"),
                        Token.Literal.Text("dig"),
                        Token.SpecialChar.EndOfLine
                ),
                matchesAstChildren(
                        TreeNode.Command.Declaration(
                                TreeNode.Type.Func.ExplicitFunc(
                                        listOf(TreeNode.Type.Number, TreeNode.Type.Text, TreeNode.Type.Text, TreeNode.Type.Text),
                                        TreeNode.Type.Text
                                ),
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                TreeNode.Command.Expression.LambdaExpression(
                                        listOf(
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Number,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam1")),
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Text,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam2")),
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Text,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam3")),
                                                TreeNode.ParameterDeclaration(
                                                        TreeNode.Type.Text,
                                                        TreeNode.Command.Expression.Value.Identifier("myParam4"))
                                        ),
                                        TreeNode.Type.Text,
                                        listOf()
                                )
                        ),
                        TreeNode.Command.Expression.FunctionCall(
                                TreeNode.Command.Expression.Value.Identifier("myFunc"),
                                listOf(TreeNode.Command.Expression.Value.Literal.Number(5.0),
                                        TreeNode.Command.Expression.Value.Literal.Text("hej"),
                                        TreeNode.Command.Expression.Value.Literal.Text("med"),
                                        TreeNode.Command.Expression.Value.Literal.Text("dig")
                                )
                        )
                )
        )
    }
}
