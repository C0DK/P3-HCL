package hclCodeTests

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.startsWith
import exceptions.CompilationException
import generation.FilePair
import generation.cpp.ProgramGenerator
import hclTestFramework.codegen.compileAndExecuteCpp
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.system.exitProcess
import kotlin.test.assertEquals

fun generateFilesFromCode(code: String): List<FilePair> {
    val lexer = lexer.Lexer(code)
    val parser = parser.Parser(lexer)
    val logger = logger.Logger()
    val ast = try {
        parser.generateAbstractSyntaxTree()
    } catch (exception: CompilationException) {
        logger.logCompilationError(exception)
        exitProcess(-1)
    }
    return ProgramGenerator().generate(ast)
}

object TestHclPrograms : Spek({
    val resourceReader = ReadResources()
    val files = resourceReader.getResourceFiles("./")
    files.filter { it.endsWith(".hcl") }.forEach { file ->
        given(file) {
            val fileContent = javaClass.classLoader.getResource(file).readText()
            val constraints = fileContent.split("\n").first().split("should ").drop(1).map { it.split(" ") }
            val expectedReturn = constraints.firstOrNull { it.first() == "return" }?.get(1)?.toInt() ?: 0
            val expectedPrint = constraints.firstOrNull { it.first() == "print" }?.drop(1)?.joinToString(" ") ?: ""
            it("should return $expectedReturn and print \"$expectedPrint\"") {
                val outputFiles = generateFilesFromCode(fileContent)
                val output = compileAndExecuteCpp(outputFiles)!!
                assertEquals(expectedReturn, output.returnValue)
                assertThat(output.string, startsWith(expectedPrint))
            }
        }
    }
})
