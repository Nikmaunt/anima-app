package app.anima.core.mind

import javax.inject.Qualifier

/**
 * ADR-020: the OPTIONAL alternative local-runtime engine (LiteRT-LM). The
 * binding exists only in debug builds (src/debug LiteRtLmDebugModule); in
 * release the Optional is empty, so the flag can never route anywhere and the
 * release APK carries no LiteRT-LM runtime at all.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AltLocalEngine
