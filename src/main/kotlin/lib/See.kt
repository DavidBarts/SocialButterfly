package name.blackcap.socialbutterfly.lib

import java.util.Formatter

private const val DELIM = '"'
private val ALWAYS_ALLOW = setOf<Char>(' ')
private val ALWAYS_BAN = setOf<Char>(DELIM, '\\')
private val FORBIDDEN = setOf<Byte>(Character.CONTROL, Character.FORMAT,
    Character.SURROGATE, Character.PRIVATE_USE, Character.UNASSIGNED,
    Character.SPACE_SEPARATOR)
private val STD_ESC_MAP = mapOf<Char, Char>('\t' to 't', '\b' to 'b', '\n' to 'n',
    '\r' to 'r', '"' to '"', '\\' to '\\')

fun see(input: String): String {
    val accum = Formatter()
    accum.format("%c", DELIM)
    for (ch in input) {
        if (ch in ALWAYS_ALLOW) {
            accum.format("%c", ch)
            continue
        }
        if (ch in ALWAYS_BAN || Character.getType(ch).toByte() in FORBIDDEN) {
            val mapped = STD_ESC_MAP[ch]
            if (mapped != null) {
                accum.format("\\%c", mapped)
            } else {
                accum.format("\\u%04x", ch.code)
            }
            continue
        }
        accum.format("%c", ch)
    }
    accum.format("%c", DELIM)
    return accum.toString()
}
