package com.aisummarypodcast.podcast

import java.util.Locale

enum class SupportedLanguage(val code: String, val displayName: String) {
    AFRIKAANS("af", "Afrikaans"),
    ARABIC("ar", "Arabic"),
    ARMENIAN("hy", "Armenian"),
    AZERBAIJANI("az", "Azerbaijani"),
    BELARUSIAN("be", "Belarusian"),
    BOSNIAN("bs", "Bosnian"),
    BULGARIAN("bg", "Bulgarian"),
    CATALAN("ca", "Catalan"),
    CHINESE("zh", "Chinese"),
    CROATIAN("hr", "Croatian"),
    CZECH("cs", "Czech"),
    DANISH("da", "Danish"),
    DUTCH("nl", "Dutch"),
    ENGLISH("en", "English"),
    ESTONIAN("et", "Estonian"),
    FINNISH("fi", "Finnish"),
    FRENCH("fr", "French"),
    GALICIAN("gl", "Galician"),
    GERMAN("de", "German"),
    GREEK("el", "Greek"),
    HEBREW("he", "Hebrew"),
    HINDI("hi", "Hindi"),
    HUNGARIAN("hu", "Hungarian"),
    ICELANDIC("is", "Icelandic"),
    INDONESIAN("id", "Indonesian"),
    ITALIAN("it", "Italian"),
    JAPANESE("ja", "Japanese"),
    KANNADA("kn", "Kannada"),
    KAZAKH("kk", "Kazakh"),
    KOREAN("ko", "Korean"),
    LATVIAN("lv", "Latvian"),
    LITHUANIAN("lt", "Lithuanian"),
    MACEDONIAN("mk", "Macedonian"),
    MALAY("ms", "Malay"),
    MARATHI("mr", "Marathi"),
    MAORI("mi", "Maori"),
    NEPALI("ne", "Nepali"),
    NORWEGIAN("no", "Norwegian"),
    PERSIAN("fa", "Persian"),
    POLISH("pl", "Polish"),
    PORTUGUESE("pt", "Portuguese"),
    ROMANIAN("ro", "Romanian"),
    RUSSIAN("ru", "Russian"),
    SERBIAN("sr", "Serbian"),
    SLOVAK("sk", "Slovak"),
    SLOVENIAN("sl", "Slovenian"),
    SPANISH("es", "Spanish"),
    SWAHILI("sw", "Swahili"),
    SWEDISH("sv", "Swedish"),
    TAGALOG("tl", "Tagalog"),
    TAMIL("ta", "Tamil"),
    THAI("th", "Thai"),
    TURKISH("tr", "Turkish"),
    UKRAINIAN("uk", "Ukrainian"),
    URDU("ur", "Urdu"),
    VIETNAMESE("vi", "Vietnamese"),
    WELSH("cy", "Welsh");

    fun toLocale(): Locale {
        val locale = Locale.of(code)
        val formatted = locale.getDisplayLanguage(Locale.ENGLISH)
        return if (formatted.isNotEmpty() && formatted != code) locale else Locale.ENGLISH
    }

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: String): SupportedLanguage? = byCode[code]

        fun isSupported(code: String): Boolean = code in byCode
    }
}
