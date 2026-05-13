package com.aisummarypodcast.llm

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.LocalDate

enum class OpeningStyle { COLD_OPEN_QUESTION, SHOCKING_STAT, SCENE_SET, CONTRARIAN_TAKE, FIRST_PERSON_HOOK }

enum class TransitionVocab { SIGNPOSTS_A, SIGNPOSTS_B, SIGNPOSTS_C }

enum class SignOffShape { RECAP_FIRST, FORWARD_LOOK, CALL_TO_ACTION, QUOTE_OF_THE_DAY }

enum class TeaserShape { LEAD_WITH_QUESTION, CURIOSITY_LIST, COLD_TEASE, RHETORICAL_HOOK }

enum class TopicEntryPattern { STRAIGHT_QUESTION, THEME_BRIDGE, CONTRAST_PIVOT, MICRO_RECAP_THEN_PIVOT }

enum class PenultimateExchangeShape { MUTUAL_THANKS, FORWARD_LOOK_HANDOFF, SINGLE_SENTENCE_CALLBACK, COLD_HANDOFF_TO_SIGN_OFF }

data class PromptVarietySelection(
    val openingStyle: OpeningStyle,
    val transitionVocab: TransitionVocab,
    val signOffShape: SignOffShape,
    val teaserShape: TeaserShape,
    val topicEntryPattern: TopicEntryPattern,
    val penultimateExchangeShape: PenultimateExchangeShape
)

/**
 * Deterministically rotates prompt-shape selections per (podcastId, episodeDate).
 *
 * Selection is a pure function of the key: regenerating the same episode yields the same
 * scaffolding choices. Different bytes of a SHA-256 digest are used per axis so the axes
 * rotate semi-independently.
 */
@Component
class PromptVarietyPicker {

    fun pick(podcastId: String, episodeDate: LocalDate): PromptVarietySelection {
        val digest = sha256("$podcastId:$episodeDate")
        return PromptVarietySelection(
            openingStyle = pickFromBytes(digest, 0, OpeningStyle.entries),
            transitionVocab = pickFromBytes(digest, 4, TransitionVocab.entries),
            signOffShape = pickFromBytes(digest, 8, SignOffShape.entries),
            teaserShape = pickFromBytes(digest, 12, TeaserShape.entries),
            topicEntryPattern = pickFromBytes(digest, 16, TopicEntryPattern.entries),
            penultimateExchangeShape = pickFromBytes(digest, 20, PenultimateExchangeShape.entries)
        )
    }

    private fun sha256(input: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))

    private fun <T> pickFromBytes(digest: ByteArray, offset: Int, values: List<T>): T {
        // Take 4 bytes as an unsigned 32-bit int, mod the enum size.
        val b0 = digest[offset].toInt() and 0xff
        val b1 = digest[offset + 1].toInt() and 0xff
        val b2 = digest[offset + 2].toInt() and 0xff
        val b3 = digest[offset + 3].toInt() and 0xff
        val composite = (b0.toLong() shl 24) or (b1.toLong() shl 16) or (b2.toLong() shl 8) or b3.toLong()
        return values[(composite % values.size).toInt()]
    }
}
