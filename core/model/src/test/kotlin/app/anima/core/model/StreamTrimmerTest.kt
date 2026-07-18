package app.anima.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreamTrimmerTest {
    private val stops = listOf("<|im_end|>", "<|endoftext|>")

    @Test
    fun `passes ordinary chunks straight through`() {
        val trimmer = StreamTrimmer(stops)
        assertThat(trimmer.feed("hello ")).isEqualTo("hello ")
        assertThat(trimmer.feed("world")).isEqualTo("world")
        assertThat(trimmer.flush()).isEmpty()
    }

    @Test
    fun `cuts at a stop token inside one chunk`() {
        val trimmer = StreamTrimmer(stops)
        assertThat(trimmer.feed("done now<|im_end|>garbage")).isEqualTo("done now")
        assertThat(trimmer.feed("more garbage")).isEmpty()
        assertThat(trimmer.flush()).isEmpty()
    }

    @Test
    fun `holds back a partial stop token split across chunks`() {
        val trimmer = StreamTrimmer(stops)
        assertThat(trimmer.feed("bye<|im_")).isEqualTo("bye")
        assertThat(trimmer.feed("end|>tail")).isEmpty()
        assertThat(trimmer.flush()).isEmpty()
    }

    @Test
    fun `releases a held tail that never becomes a stop token`() {
        val trimmer = StreamTrimmer(stops)
        assertThat(trimmer.feed("a<")).isEqualTo("a")
        assertThat(trimmer.feed("3 and b<4")).isEqualTo("<3 and b<4")
        assertThat(trimmer.flush()).isEmpty()
    }

    @Test
    fun `flush returns an innocent held tail at the end`() {
        val trimmer = StreamTrimmer(stops)
        assertThat(trimmer.feed("smile <")).isEqualTo("smile ")
        assertThat(trimmer.flush()).isEqualTo("<")
    }

    @Test
    fun `no stop tokens means no interference`() {
        val trimmer = StreamTrimmer(emptyList())
        assertThat(trimmer.feed("anything <|im_end|> goes")).isEqualTo("anything <|im_end|> goes")
    }

    @Test
    fun `final trim cuts the first stop token and trailing space`() {
        assertThat(StreamTrimmer.trimFinal("ok<|im_end|>junk", stops)).isEqualTo("ok")
        assertThat(StreamTrimmer.trimFinal("plain reply  ", stops)).isEqualTo("plain reply")
    }
}
