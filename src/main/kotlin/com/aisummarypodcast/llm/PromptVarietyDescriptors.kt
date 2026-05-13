package com.aisummarypodcast.llm

/**
 * Maps each [PromptVarietyPicker] enum value to a short directive sentence the LLM reads.
 *
 * Keep the descriptors abstract (describe the *kind* of beat, not a literal sample sentence).
 * Concrete sample sentences belong nowhere in the compose prompt — they end up copied verbatim
 * across episodes. See [BannedPrompts].
 */
object PromptVarietyDescriptors {

    fun describe(style: OpeningStyle): String = when (style) {
        OpeningStyle.COLD_OPEN_QUESTION -> "Open with a single pointed question that drops the listener mid-thought."
        OpeningStyle.SHOCKING_STAT -> "Open with a single arresting number or statistic pulled from today's most surprising story."
        OpeningStyle.SCENE_SET -> "Open by sketching a vivid in-the-moment scene from today's most evocative story."
        OpeningStyle.CONTRARIAN_TAKE -> "Open with a contrarian framing of the day's most widely accepted assumption."
        OpeningStyle.FIRST_PERSON_HOOK -> "Open with a brief first-person reaction or admission tied to today's most personal story."
    }

    fun describe(vocab: TransitionVocab): String = when (vocab) {
        TransitionVocab.SIGNPOSTS_A -> "Use transition vocabulary like 'meanwhile', 'in the same vein', 'on a related note', 'speaking of which'."
        TransitionVocab.SIGNPOSTS_B -> "Use transition vocabulary like 'shifting tracks', 'flipping the lens', 'pulling on another thread', 'over to a different beat'."
        TransitionVocab.SIGNPOSTS_C -> "Use transition vocabulary like 'sticking with that thought', 'circling sideways', 'one step over', 'a different angle on the same week'."
    }

    fun describe(shape: SignOffShape): String = when (shape) {
        SignOffShape.RECAP_FIRST -> "End with a recap of the strongest beat or two from the episode, then a brief sign-off."
        SignOffShape.FORWARD_LOOK -> "End by naming what you'll be watching for next, then a brief sign-off."
        SignOffShape.CALL_TO_ACTION -> "End with a single concrete invitation to the listener (try, read, build, share), then a brief sign-off."
        SignOffShape.QUOTE_OF_THE_DAY -> "End with one short quotable line that captures the episode's spirit, then a brief sign-off."
    }

    fun describe(shape: TeaserShape): String = when (shape) {
        TeaserShape.LEAD_WITH_QUESTION -> "Tease the episode by leading with a single intriguing question that the rest of the show answers."
        TeaserShape.CURIOSITY_LIST -> "Tease the episode as a short list of 2-3 punchy intriguing fragments (no full sentences)."
        TeaserShape.COLD_TEASE -> "Tease the episode by dropping the most surprising single fact without setup, then promise context to come."
        TeaserShape.RHETORICAL_HOOK -> "Tease the episode with a rhetorical 'would you believe...' style hook."
    }

    fun describe(pattern: TopicEntryPattern): String = when (pattern) {
        TopicEntryPattern.STRAIGHT_QUESTION -> "Enter each new topic with a direct, neutral question (no name address)."
        TopicEntryPattern.THEME_BRIDGE -> "Enter each new topic by naming an abstract theme that connects it to the previous one."
        TopicEntryPattern.CONTRAST_PIVOT -> "Enter each new topic by contrasting it with the previous one ('on the opposite end of the week...', etc.)."
        TopicEntryPattern.MICRO_RECAP_THEN_PIVOT -> "Enter each new topic by briefly restating what the previous topic concluded, then pivoting."
    }

    fun describe(shape: PenultimateExchangeShape): String = when (shape) {
        PenultimateExchangeShape.MUTUAL_THANKS -> "The penultimate exchange is a brief mutual thank-you between speakers, varied wording each episode."
        PenultimateExchangeShape.FORWARD_LOOK_HANDOFF -> "The penultimate exchange names what the speakers are looking forward to next time before the sign-off."
        PenultimateExchangeShape.SINGLE_SENTENCE_CALLBACK -> "The penultimate exchange callbacks ONE earlier moment from this episode in a single sentence, then hands off."
        PenultimateExchangeShape.COLD_HANDOFF_TO_SIGN_OFF -> "Skip a penultimate exchange entirely — go straight from the last topic into the sign-off."
    }
}
