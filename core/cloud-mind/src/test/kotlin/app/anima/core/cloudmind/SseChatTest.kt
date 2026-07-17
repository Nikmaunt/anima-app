package app.anima.core.cloudmind

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class SseChatTest {
    @Test
    fun `request body carries model, stream flag and both messages`() {
        val body = SseChat.requestBody("gpt-x", "sys prompt", "user prompt", stream = true)
        val obj = Json.parseToJsonElement(body).jsonObject
        assertThat(obj["model"]?.jsonPrimitive?.content).isEqualTo("gpt-x")
        assertThat(obj["stream"]?.jsonPrimitive?.content).isEqualTo("true")
        val messages = obj["messages"]!!.jsonArray
        assertThat(messages).hasSize(2)
        assertThat(
            messages[0]
                .jsonObject["role"]
                ?.jsonPrimitive
                ?.content,
        ).isEqualTo("system")
        assertThat(
            messages[1]
                .jsonObject["content"]
                ?.jsonPrimitive
                ?.content,
        ).isEqualTo("user prompt")
    }

    @Test
    fun `chunk parsing reads delta content and tolerates garbage`() {
        val line = """data: {"choices":[{"delta":{"content":"Hel"}}]}"""
        assertThat(SseChat.chunkFromLine(line)).isEqualTo("Hel")
        assertThat(SseChat.chunkFromLine("data: {\"choices\":[{\"delta\":{}}]}")).isNull()
        assertThat(SseChat.chunkFromLine(": keep-alive comment")).isNull()
        assertThat(SseChat.chunkFromLine("data: not json at all")).isNull()
        assertThat(SseChat.chunkFromLine("")).isNull()
    }

    @Test
    fun `done marker terminates and is not a chunk`() {
        assertThat(SseChat.isDone("data: [DONE]")).isTrue()
        assertThat(SseChat.isDone("data: {\"choices\":[]}")).isFalse()
        assertThat(SseChat.chunkFromLine("data: [DONE]")).isNull()
    }

    @Test
    fun `non-streaming response content is extracted`() {
        val body =
            """{"choices":[{"message":{"content":"[{\"category\":\"other\",\"text\":\"x\"}]"}}]}"""
        assertThat(SseChat.contentFromResponse(body)).contains("category")
        assertThat(SseChat.contentFromResponse("broken")).isEmpty()
    }

    @Test
    fun `completions url appends the standard path once`() {
        assertThat(SseChat.completionsUrl("https://api.openai.com/v1"))
            .isEqualTo("https://api.openai.com/v1/chat/completions")
        assertThat(SseChat.completionsUrl("https://host/v1/"))
            .isEqualTo("https://host/v1/chat/completions")
    }
}
