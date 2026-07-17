package app.anima.core.cloudmind

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The OpenAI-compatible chat wire format (ADR-011), as pure functions —
 * request-body assembly and SSE line parsing carry the entire protocol
 * knowledge and are unit-tested without a socket anywhere near.
 */
object SseChat {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun requestBody(
        model: String,
        system: String,
        user: String,
        stream: Boolean,
        temperature: Double = CHAT_TEMPERATURE,
    ): String =
        buildJsonObject {
            put("model", model)
            put("stream", stream)
            put("temperature", temperature)
            put("max_tokens", MAX_OUTPUT_TOKENS)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put("content", system)
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", user)
                        },
                    )
                },
            )
        }.toString()

    /** True for the SSE stream terminator `data: [DONE]`. */
    fun isDone(line: String): Boolean = line.removePrefix(DATA_PREFIX).trim() == DONE_MARKER

    /**
     * Content delta from one SSE `data:` line; null for empty deltas,
     * comments, non-data lines and anything unparseable (tolerant by
     * design — a provider quirk must degrade, not crash).
     */
    fun chunkFromLine(line: String): String? {
        if (!line.startsWith(DATA_PREFIX) || isDone(line)) return null
        return runCatching {
            json
                .parseToJsonElement(line.removePrefix(DATA_PREFIX).trim())
                .jsonObject["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("delta")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    /** `choices[0].message.content` of a non-streaming response body. */
    fun contentFromResponse(body: String): String =
        runCatching {
            json
                .parseToJsonElement(body)
                .jsonObject["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                .orEmpty()
        }.getOrDefault("")

    /** `{base}/chat/completions`; the UI asks for a base ending in `/v1`. */
    fun completionsUrl(baseUrl: String): String = baseUrl.trimEnd('/') + "/chat/completions"

    private const val DATA_PREFIX = "data:"
    private const val DONE_MARKER = "[DONE]"
    const val CHAT_TEMPERATURE = 0.8
    const val MAX_OUTPUT_TOKENS = 512
}
