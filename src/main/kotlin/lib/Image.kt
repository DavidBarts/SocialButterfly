package name.blackcap.socialbutterfly.lib

import java.io.File
import javax.swing.filechooser.FileNameExtensionFilter

/* What we support IS NOT the same as what Java supports, because we don't need
   to understand an image file, only to read it and send it to the social media
   site.  What matters here is what browsers and social media sites support. */

private val EXT_TO_MIME_TYPE = mapOf<String, String>(
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "png" to "image/png",
    "webp" to "image/webp"
)

val EXTENSION_FILTER =
    FileNameExtensionFilter("Supported Image Types", *EXT_TO_MIME_TYPE.keys.toTypedArray())

/* returns null if passed file appears to be a valid image, or a string explaining
   why it appears to be invalid otherwise */
fun verifyImage(file: File): String? {
    if (!file.exists()) {
        return "does not exist"
    }
    if (!file.isFile) {
        return "not a file"
    }

    if (getExtension(file) !in EXT_TO_MIME_TYPE) {
        return "not a supported image type"
    }
    return null
}

fun getMimeType(file: File): String = EXT_TO_MIME_TYPE.getValue(getExtension(file))

/* file name with only ASCII letters, digits, dot, dash, underscore
   and hyphen characters */
fun sanitizedName(file: File): String {
    val accum = StringBuilder()
    for (ch in file.name) {
        if (Character.isHighSurrogate(ch)) {
            continue
        }
        accum.append(if (isLegalChar(ch)) ch else '_')
    }
    return accum.toString()
}

private fun isLegalChar(ch: Char): Boolean =
    ch in '0'..'9' || ch in 'A' .. 'Z' || ch in 'a' .. 'z' || ch == '-' || ch == '_' || ch == '.'

private var memoizedInput: File? = null
private var memoizedOutput: String = ""

private fun getExtension(file: File): String {
    if (file == memoizedInput) {
        return memoizedOutput
    }
    memoizedInput = file
    val fileName = file.name
    val lastDot = fileName.lastIndexOf('.')
    memoizedOutput = if (lastDot < 0) "" else fileName.substring(lastDot + 1).lowercase()
    return memoizedOutput
}
