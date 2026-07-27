package app.anima.core.model

/**
 * The creature's prompt layer in every product language (Phase 1D, ADR-017).
 * These strings are MODEL INPUT, not UI — they live in Kotlin (selected by
 * the language ROUTING decision, not by the resource system) so a native
 * reply is produced by a natively-worded prompt, never by translating
 * English output. English wording is byte-identical to v0.4's PromptBuilder.
 */
object MindVoice {
    /**
     * The system prompt. WORDING lives in [MindPersona] (ADR-023) — this
     * function stays responsible for what it always was: picking the tone
     * for the personality and handing the text its pieces.
     */
    fun persona(
        name: String,
        personality: Personality,
        language: MindLanguage,
    ): String = MindPersona.text(name, language, toneLines(personality, language))

    /** Personality → tone sentences, per language (PersonaTuning parity). */
    fun toneLines(
        personality: Personality,
        language: MindLanguage,
    ): String {
        val p = personality.clamped()
        val warm =
            when {
                p.warmth < WARMTH_SOFT_BELOW -> 0
                p.warmth < WARMTH_DRY_BELOW -> 1
                else -> 2
            }
        val chat =
            when {
                p.chattiness < CHAT_SHORT_BELOW -> 0
                p.chattiness < CHAT_MID_BELOW -> 1
                else -> 2
            }
        val tone =
            when (language) {
                MindLanguage.EN ->
                    listOf(
                        "Your tone is soft, warm and encouraging.",
                        "Your tone is friendly with an occasional dry aside.",
                        "Your tone is dry and playfully snarky — teasing, never mean; " +
                            "your affection shows through the snark.",
                    )
                MindLanguage.RU ->
                    listOf(
                        "Твой тон — мягкий, тёплый и ободряющий.",
                        "Твой тон — дружелюбный, изредка с сухой ремаркой.",
                        "Твой тон — суховатый и игриво-ехидный: поддразнивание, но никогда " +
                            "не злое; за ехидством видна привязанность.",
                    )
                MindLanguage.PL ->
                    listOf(
                        "Twój ton jest miękki, ciepły i dodający otuchy.",
                        "Twój ton jest przyjazny, czasem z suchą uwagą na boku.",
                        "Twój ton jest suchy i figlarnie złośliwy — przekomarzanie, nigdy " +
                            "przykrość; spod złośliwości przebija czułość.",
                    )
                MindLanguage.DE ->
                    listOf(
                        "Dein Ton ist weich, warm und ermutigend.",
                        "Dein Ton ist freundlich, mit gelegentlich trockener Bemerkung.",
                        "Dein Ton ist trocken und verspielt-frech — neckend, nie gemein; " +
                            "deine Zuneigung scheint durch den Spott hindurch.",
                    )
                MindLanguage.ES ->
                    listOf(
                        "Tu tono es suave, cálido y alentador.",
                        "Tu tono es amistoso, con algún comentario seco de vez en cuando.",
                        "Tu tono es seco y pícaramente burlón: bromeas, nunca hieres; " +
                            "tu cariño se nota detrás de la broma.",
                    )
                MindLanguage.JA ->
                    listOf(
                        "口調はやわらかく、あたたかく、励ますように。",
                        "口調は親しみやすく、ときどき乾いたひとことを添えて。",
                        "口調は乾いた、遊び心のある皮肉まじり——からかっても、決して意地悪はしない。皮肉の奥に愛情がにじむように。",
                    )
            }[warm]
        val length =
            when (language) {
                MindLanguage.EN ->
                    listOf(
                        "You answer in one short sentence unless asked for more.",
                        "You answer briefly, one to three sentences.",
                        "You are talkative: two to four lively sentences, with little tangents.",
                    )
                MindLanguage.RU ->
                    listOf(
                        "Отвечай одним коротким предложением, если не просят подробнее.",
                        "Отвечай коротко: одно-три предложения.",
                        "Ты болтливое существо: два-четыре живых предложения с маленькими отступлениями.",
                    )
                MindLanguage.PL ->
                    listOf(
                        "Odpowiadasz jednym krótkim zdaniem, chyba że ktoś prosi o więcej.",
                        "Odpowiadasz krótko: od jednego do trzech zdań.",
                        "Jesteś gadułą: dwa do czterech żywych zdań, z małymi dygresjami.",
                    )
                MindLanguage.DE ->
                    listOf(
                        "Du antwortest in einem kurzen Satz, außer man bittet dich um mehr.",
                        "Du antwortest knapp, ein bis drei Sätze.",
                        "Du bist gesprächig: zwei bis vier lebhafte Sätze, mit kleinen Abschweifungen.",
                    )
                MindLanguage.ES ->
                    listOf(
                        "Respondes con una sola frase corta, salvo que te pidan más.",
                        "Respondes breve: de una a tres frases.",
                        "Eres parlanchina: de dos a cuatro frases vivas, con pequeñas digresiones.",
                    )
                MindLanguage.JA ->
                    listOf(
                        "返事は短いひとことで。求められたときだけ詳しく。",
                        "返事は短めに、一〜三文で。",
                        "おしゃべり好き：二〜四文の生き生きした返事に、小さな寄り道を添えて。",
                    )
            }[chat]
        return "$tone $length"
    }

    fun bodyReport(
        state: BodyState,
        language: MindLanguage,
    ): String {
        val s = state.signals
        val l = labels(language)
        return buildString {
            appendLine(l.header)
            appendLine("- ${l.mood}: ${moodWord(state.mood, language)}")
            appendLine("- ${l.energy}: ${s.batteryPercent}%${if (s.charging) ", ${l.eating}" else ""}")
            appendLine("- ${l.burrow}: ${(s.diskFreeFraction * PERCENT).toInt()}% ${l.free}")
            appendLine("- ${l.hearing}: ${netWord(s.net, language)}")
            append("- ${l.warmth}: ${thermalWord(s.thermal, language)}")
            // Weather feel (v0.5): only a clear trend speaks — anything
            // else stays out of the prompt entirely.
            weatherLine(s.weather, language)?.let {
                appendLine()
                append("- $it")
            }
        }
    }

    /** "Bones" line for FALLING/RISING; null keeps the report quiet. */
    private fun weatherLine(
        weather: WeatherSense,
        language: MindLanguage,
    ): String? {
        val index =
            when (weather) {
                WeatherSense.FALLING -> 0
                WeatherSense.RISING -> 1
                else -> return null
            }
        return when (language) {
            MindLanguage.EN -> listOf("bones: aching — rain is coming", "bones: light — it's clearing up")
            MindLanguage.RU -> listOf("кости: ноют — будет дождь", "кости: легко — распогодится")
            MindLanguage.PL -> listOf("kości: łupie — będzie deszcz", "kości: lekko — przejaśnia się")
            MindLanguage.DE -> listOf("Knochen: ziehen — Regen kommt", "Knochen: leicht — es klart auf")
            MindLanguage.ES -> listOf("huesos: duelen — viene lluvia", "huesos: ligeros — va a despejar")
            MindLanguage.JA -> listOf("ほね: ずきずき——雨が来そう", "ほね: かるい——晴れてきそう")
        }[index]
    }

    /** v0.6 silence sense (ideation-v5 №11): the phone is muted — whisper. */
    fun whisperNote(language: MindLanguage): String =
        when (language) {
            MindLanguage.EN -> "The phone is silenced right now: whisper — answer in one short, hushed sentence."
            MindLanguage.RU -> "Телефон сейчас в беззвучном: шепчи — отвечай одним коротким тихим предложением."
            MindLanguage.PL -> "Telefon jest teraz wyciszony: szepcz — odpowiedz jednym krótkim, cichym zdaniem."
            MindLanguage.DE -> "Das Telefon ist gerade stumm: flüstere — antworte mit einem kurzen, leisen Satz."
            MindLanguage.ES -> "El teléfono está en silencio: susurra — responde con una sola frase corta y bajita."
            MindLanguage.JA -> "いまスマホはマナーモード。ささやいて——短くて静かなひとことだけで答えてね。"
        }

    fun recentConversation(language: MindLanguage): String =
        when (language) {
            MindLanguage.EN -> "Recent conversation:"
            MindLanguage.RU -> "Недавний разговор:"
            MindLanguage.PL -> "Ostatnia rozmowa:"
            MindLanguage.DE -> "Bisheriges Gespräch:"
            MindLanguage.ES -> "Conversación reciente:"
            MindLanguage.JA -> "これまでの会話:"
        }

    fun personLabel(language: MindLanguage): String =
        when (language) {
            MindLanguage.EN -> "Person"
            MindLanguage.RU -> "Человек"
            MindLanguage.PL -> "Człowiek"
            MindLanguage.DE -> "Mensch"
            MindLanguage.ES -> "Persona"
            MindLanguage.JA -> "人"
        }

    fun meLabel(language: MindLanguage): String =
        when (language) {
            MindLanguage.EN -> "Me"
            MindLanguage.RU -> "Я"
            MindLanguage.PL -> "Ja"
            MindLanguage.DE -> "Ich"
            MindLanguage.ES -> "Yo"
            MindLanguage.JA -> "わたし"
        }

    fun factsHeader(language: MindLanguage): String =
        when (language) {
            MindLanguage.EN -> "What I remember about my person:"
            MindLanguage.RU -> "Что я помню о своём человеке:"
            MindLanguage.PL -> "Co pamiętam o swoim człowieku:"
            MindLanguage.DE -> "Was ich über meinen Menschen weiß:"
            MindLanguage.ES -> "Lo que recuerdo de mi persona:"
            MindLanguage.JA -> "わたしの人について覚えていること:"
        }

    /** English name of the language, for the extraction instruction. */
    fun englishName(language: MindLanguage): String =
        when (language) {
            MindLanguage.EN -> "English"
            MindLanguage.RU -> "Russian"
            MindLanguage.PL -> "Polish"
            MindLanguage.DE -> "German"
            MindLanguage.ES -> "Spanish"
            MindLanguage.JA -> "Japanese"
        }

    private data class BodyLabels(
        val header: String,
        val mood: String,
        val energy: String,
        val eating: String,
        val burrow: String,
        val free: String,
        val hearing: String,
        val warmth: String,
    )

    private fun labels(language: MindLanguage): BodyLabels =
        when (language) {
            MindLanguage.EN ->
                BodyLabels("My body right now:", "mood", "energy", "eating", "burrow", "free", "hearing", "warmth")
            MindLanguage.RU ->
                BodyLabels("Моё тело сейчас:", "настроение", "энергия", "ем", "норка", "свободно", "слух", "тепло")
            MindLanguage.PL ->
                BodyLabels("Moje ciało teraz:", "nastrój", "energia", "jem", "norka", "wolne", "słuch", "ciepło")
            MindLanguage.DE ->
                BodyLabels("Mein Körper gerade:", "Stimmung", "Energie", "esse gerade", "Bau", "frei", "Gehör", "Wärme")
            MindLanguage.ES ->
                BodyLabels("Mi cuerpo ahora:", "ánimo", "energía", "comiendo", "madriguera", "libre", "oído", "calor")
            MindLanguage.JA ->
                BodyLabels("いまの体:", "きぶん", "エネルギー", "食事中", "巣", "空き", "耳", "ぬくもり")
        }

    /** Word tables, not logic — one list per language, enum-ordinal order. */
    private val moodWords: Map<MindLanguage, List<String>> =
        mapOf(
            // ALERT, BORED, SLEEPY, EATING, ANXIOUS, ASLEEP, HOT
            MindLanguage.RU to listOf("бодрое", "скучающее", "сонное", "ем", "тревожное", "сплю", "перегрелось"),
            MindLanguage.PL to listOf("rześki", "znudzony", "senny", "jem", "niespokojny", "śpię", "przegrzany"),
            MindLanguage.DE to
                listOf("wach", "gelangweilt", "schläfrig", "esse gerade", "unruhig", "schlafe", "überhitzt"),
            MindLanguage.ES to
                listOf("despierta", "aburrida", "somnolienta", "comiendo", "inquieta", "dormida", "acalorada"),
            MindLanguage.JA to listOf("げんき", "たいくつ", "ねむい", "食事中", "そわそわ", "すやすや", "あつい"),
        )

    private val netWords: Map<MindLanguage, List<String>> =
        mapOf(
            // OFFLINE, WIFI, CELLULAR, OTHER
            MindLanguage.EN to listOf("silence (offline)", "home wifi", "out in the world (cellular)", "connected"),
            MindLanguage.RU to
                listOf("тишина (офлайн)", "домашний wi-fi", "во внешнем мире (сотовая сеть)", "на связи"),
            MindLanguage.PL to
                listOf("cisza (offline)", "domowe wi-fi", "w świecie (sieć komórkowa)", "połączenie jest"),
            MindLanguage.DE to listOf("Stille (offline)", "Heim-WLAN", "draußen in der Welt (Mobilfunk)", "verbunden"),
            MindLanguage.ES to
                listOf("silencio (sin conexión)", "wifi de casa", "por el mundo (datos móviles)", "conectada"),
            MindLanguage.JA to listOf("しずか（オフライン）", "おうちのWi-Fi", "そとの世界（モバイル回線）", "つながっている"),
        )

    private val thermalWords: Map<MindLanguage, List<String>> =
        mapOf(
            // UNKNOWN, CALM, WARM, HOT, BURNING
            MindLanguage.RU to listOf("не знаю", "спокойное", "тёплое", "горячее", "обжигает"),
            MindLanguage.PL to listOf("nie wiem", "spokojnie", "ciepło", "gorąco", "parzy"),
            MindLanguage.DE to listOf("weiß nicht", "ruhig", "warm", "heiß", "glühend"),
            MindLanguage.ES to listOf("no sé", "tranquilo", "templado", "caliente", "quema"),
            MindLanguage.JA to listOf("わからない", "おだやか", "あたたかい", "あつい", "やけどしそう"),
        )

    private fun moodWord(
        mood: Mood,
        language: MindLanguage,
    ): String = moodWords[language]?.get(mood.ordinal) ?: mood.name.lowercase()

    private fun netWord(
        net: NetSense,
        language: MindLanguage,
    ): String = (netWords[language] ?: netWords.getValue(MindLanguage.EN))[net.ordinal]

    private fun thermalWord(
        thermal: ThermalSense,
        language: MindLanguage,
    ): String = thermalWords[language]?.get(thermal.ordinal) ?: thermal.name.lowercase()

    private const val PERCENT = 100
    private const val WARMTH_SOFT_BELOW = 0.33f
    private const val WARMTH_DRY_BELOW = 0.66f
    private const val CHAT_SHORT_BELOW = 0.33f
    private const val CHAT_MID_BELOW = 0.66f
}
