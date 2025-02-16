package name.blackcap.socialbutterfly.tools.aescrypt

// This is more a debugging/development utility than anything. It is not
// really suited for general-purpose use, because it behaves poorly on
// large files.

import name.blackcap.socialbutterfly.lib.*
import org.apache.commons.cli.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import kotlin.system.exitProcess

const val CAT = "cat"
const val DECRYPT = "decrypt"
const val HELP = "help"
const val OUTPUT = "output"

const val EXTENSION = ".aes"

fun main(argv: Array<String>) {
    setName("aescrypt")
    val (commandLine, file) = parseCommandLine(argv)
    try {
        getInputStream(commandLine, file).use { inputStream ->
            getOutputStream(commandLine, file).use { outputStream ->
                if (commandLine.hasOption(DECRYPT) || commandLine.hasOption(CAT)) {
                    decrypt(inputStream, outputStream)
                } else {
                    encrypt(inputStream, outputStream)
                }
            }
        }
        if (!commandLine.hasOption(CAT)) {
            Files.delete(Path.of(file))
        }
    } catch (e: ConsoleException) {
        printError(e.message ?: "console I/O error")
        exitProcess(1)
    } catch (e: GeneralSecurityException) {
        printError(e.message ?: "crypto error")
        exitProcess(1)
    } catch (e: IOException) {
        printError(e.message ?: "I/O error")
        exitProcess(1)
    }
}

fun parseCommandLine(argv: Array<String>): Pair<CommandLine, String> {
    val options = Options().apply {
        addOption("c", CAT, false, "Copy password into clipboard.")
        addOption("d", DECRYPT, false, "Decrypt instead of encrypting.")
        addOption("h", HELP, false, "Print this help message.")
        addOption("o", OUTPUT, true, "Name of output file.")
    }
    val commandLine = try {
        DefaultParser().parse(options, argv)
    } catch (e: ParseException) {
        printError(e.message ?: "syntax error")
        exitProcess(2)
    }
    if (commandLine.hasOption(HELP)) {
        HelpFormatter().printHelp("aescrypt [options] file", options)
        exitProcess(0)
    }
    if (commandLine.hasOption(CAT) && commandLine.hasOption(OUTPUT)) {
        printError("--$CAT and --$OUTPUT options are mutually exclusive")
        exitProcess(2)
    }
    if (commandLine.hasOption(CAT) && commandLine.hasOption(DECRYPT)) {
        printError("--$CAT and --$DECRYPT options are mutually exclusive")
        exitProcess(2)
    }
    val files = commandLine.args
    if (files.isEmpty()) {
        printError("expecting file name")
        exitProcess(2)
    }
    if (files.size != 1) {
        printError("only one file name allowed")
        exitProcess(2)
    }
    return Pair<CommandLine, String>(commandLine, files[0])
}

fun getInputStream(commandLine: CommandLine, fileName: String): InputStream {
    return BufferedInputStream(FileInputStream(fileName))
}

fun getOutputStream(commandLine: CommandLine, fileName: String): OutputStream {
    if (commandLine.hasOption(CAT)) {
        return System.out
    }
    var outputFileName = commandLine.getOptionValue(OUTPUT)
    if (outputFileName == null) {
        outputFileName = if (commandLine.hasOption(DECRYPT)) {
            if (!fileName.endsWith(EXTENSION)) {
                printError("${see(fileName)} does not end with $EXTENSION")
                exitProcess(1)
            }
            fileName.substring(0, fileName.length - EXTENSION.length)
        } else {
            fileName + EXTENSION
        }
    }
    return BufferedOutputStream(FileOutputStream(outputFileName))
}

fun encrypt(inputStream: InputStream, outputStream: OutputStream) {
    val secureRandom = SecureRandom()
    val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
    outputStream.write(salt)
    val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
    outputStream.write(iv)
    val secretKey = getSecretKey(getPassword("Encryption key: ", true), salt)
    val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
    outputStream.write(cipher.doFinal(inputStream.readBytes()))
}

fun decrypt(inputStream: InputStream, outputStream: OutputStream) {
    val salt = ByteArray(SALT_LENGTH).also { inputStream.read(it) }
    val iv = ByteArray(IV_LENGTH).also { inputStream.read(it) }
    val secretKey = getSecretKey(getPassword("Decryption key: "), salt)
    val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
    outputStream.write(cipher.doFinal(inputStream.readBytes()))
}
