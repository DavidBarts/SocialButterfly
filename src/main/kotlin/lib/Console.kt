package name.blackcap.socialbutterfly.lib

fun readLine(prompt: String): String =
    doConsoleIo({ System.console()?.readLine(prompt) }, "unable to read line")

fun getPassword(prompt: String, verify: Boolean = false): CharArray {
    while (true) {
        val pw1 = _getPassword(prompt)
        if (!verify) {
            return pw1
        }
        val pw2 = _getPassword("Verification: ")
        if (pw1 contentEquals pw2) {
            return pw1
        }
        printError("mismatch, try again")
    }
}

private fun _getPassword(prompt: String): CharArray =
    doConsoleIo({ System.console()?.readPassword(prompt) }, "unable to read password")

private fun <T> must(getter: () -> T, checker: (T) -> Boolean): T {
    while (true) {
        val got = getter()
        if (checker(got)) {
            return got
        }
        printError("entry must not be empty, try again")
    }
}

private fun <T> doConsoleIo(getter: () -> T?, message: String): T {
    val ret = getter() ?: throw ConsoleException(message)
    return ret
}

private var myName = "(unnamed)"

fun setName(name: String) {
    myName = name
}

fun printError(message: String) {
    System.err.println("$myName: $message")
}

class ConsoleException(message: String, cause: Throwable? = null) : Exception(message, cause)
