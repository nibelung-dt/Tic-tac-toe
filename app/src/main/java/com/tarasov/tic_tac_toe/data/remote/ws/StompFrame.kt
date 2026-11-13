package com.tarasov.tic_tac_toe.data.remote.ws


/**
 * Один STOMP-фрейм: команда, заголовки и тело.
 */
data class StompFrame(
    val command: String,
    val headers: Map<String, String>,
    val body: String,
) {
    companion object {

        /**
         * Разбирает фрейм, пришедший от сервера.
         * Строки могут приходить и с \n, и с \r\n — нормализуем заранее.
         */
        fun parse(raw: String): StompFrame? {
            if (raw.isBlank()) return null

            val normalized = raw.replace("\r\n", "\n")
            val headerEnd = normalized.indexOf("\n\n")
            val head = if (headerEnd >= 0) normalized.substring(0, headerEnd) else normalized
            val body = if (headerEnd >= 0) normalized.substring(headerEnd + 2) else ""

            val lines = head.split('\n')
            val command = lines.firstOrNull()?.trim().orEmpty()
            if (command.isEmpty()) return null

            val headers = LinkedHashMap<String, String>()
            for (line in lines.drop(1)) {
                if (line.isEmpty()) continue
                val separator = line.indexOf(':')
                if (separator <= 0) continue
                headers[unescape(line.substring(0, separator))] =
                    unescape(line.substring(separator + 1))
            }

            return StompFrame(command = command, headers = headers, body = body)
        }

        private fun unescape(value: String): String {
            if ('\\' !in value) return value

            val result = StringBuilder(value.length)
            var index = 0
            while (index < value.length) {
                val char = value[index]
                if (char == '\\' && index + 1 < value.length) {
                    when (val next = value[index + 1]) {
                        'n' -> result.append('\n')
                        'r' -> result.append('\r')
                        'c' -> result.append(':')
                        '\\' -> result.append('\\')
                        else -> result.append(next)
                    }
                    index += 2
                } else {
                    result.append(char)
                    index++
                }
            }
            return result.toString()
        }
    }
}