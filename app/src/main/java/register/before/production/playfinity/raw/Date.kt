package register.before.production.playfinity.raw

import java.text.SimpleDateFormat
import java.util.*

fun Date.format(pattern: String): String {
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return simpleDateFormat.format(this)
}