package logger

import exceptions.CompilationException
import exceptions.ErrorTypes

class Logger: ILogger {
    override fun logCompilationError(error: CompilationException) {
        print("- ERROR: ")
        when(error.errorType){
            ErrorTypes.TYPE_ERROR -> print("${ErrorTypes.TYPE_ERROR}")
            ErrorTypes.UNINITIALIZED_ERROR -> print("${ErrorTypes.UNINITIALIZED_ERROR}")
            ErrorTypes.UNDECLARED_IDENTIFIER_ERROR -> print("${ErrorTypes.UNDECLARED_IDENTIFIER_ERROR}")
            ErrorTypes.ZERO_DIVISION -> print("${ErrorTypes.ZERO_DIVISION}")
            ErrorTypes.MISSING_PARENTHESIS -> print("${ErrorTypes.MISSING_PARENTHESIS}")
            ErrorTypes.MISSING_BRACKETS -> print("${ErrorTypes.MISSING_BRACKETS}")
            ErrorTypes.MISSING_ARGUMENT -> print("${ErrorTypes.MISSING_ARGUMENT}")
        }
        print(" found at line ${error.lineNumber} index ${error.lineIndex}:\n")
        println("| ${error.lineText}")
        println("|" + " " * error.lineIndex + "^--")
        println("| ${error.errorMessage}.")

    }
    private operator fun String.times (num: Int) = (0 .. num).joinToString("") { this }
}
