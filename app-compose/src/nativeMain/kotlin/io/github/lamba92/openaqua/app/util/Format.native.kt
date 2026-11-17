package io.github.lamba92.openaqua.app.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

actual fun String.format(vararg args: Any?): String {
    if (args.isEmpty()) return this
    val regex = Regex("%(\\+)?(?:\\.(\\d+))?([fds])")
    var argIndex = 0
    return regex.replace(this) { match ->
        if (argIndex >= args.size) return@replace match.value
        val arg = args[argIndex++]
        val forcePlus = match.groupValues[1] == "+"
        val precision = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull()
        val specifier = match.groupValues[3]
        when (specifier) {
            "s" -> {
                arg.toString()
            }

            "d" -> {
                val num = (arg as? Number)?.toLong() ?: 0L
                val sign =
                    when {
                        num < 0 -> ""
                        forcePlus -> "+"
                        else -> ""
                    }
                "$sign$num"
            }

            "f" -> {
                val d = (arg as? Number)?.toDouble() ?: 0.0
                val p = precision ?: 6
                val sign =
                    when {
                        d < 0 -> "-"
                        forcePlus -> "+"
                        else -> ""
                    }
                val absVal = abs(d)
                val factor = 10.0.pow(p)
                val rounded = round(absVal * factor) / factor
                val whole = rounded.toLong()
                when (p) {
                    0 -> {
                        "$sign$whole"
                    }

                    else -> {
                        val fracVal = round((rounded - whole) * factor).toLong()
                        val fracStr = fracVal.toString().padStart(p, '0')
                        "$sign$whole.$fracStr"
                    }
                }
            }

            else -> {
                match.value
            }
        }
    }
}
