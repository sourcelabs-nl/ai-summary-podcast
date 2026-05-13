package com.aisummarypodcast.llm

/**
 * Literal example sentences that previously appeared inside composer prompts. They are listed
 * here so the composer test suite can fail CI if any of them ever reappears verbatim in a built
 * prompt — these are the phrases the LLM was copying word-for-word into generated scripts.
 *
 * This is a blocklist for the *prompt* text, not for the *output*. The LLM may still produce
 * similar sentences organically.
 */
object BannedPrompts {

    val phrases: List<String> = listOf(
        // Briefing / dialogue curiosity-hook samples
        "But here's where it gets really interesting...",
        "So why should we care?",
        "You'd think that's the whole story, but...",

        // Mid-roll callback samples
        "Remember that framework we discussed earlier? Well, this connects directly...",
        "Remember that thing we talked about earlier? Well, this connects directly...",
        "This ties back to what you said about...",

        // Transition / signpost samples
        "Now for something completely different...",
        "Switching gears...",

        // Strategic-cliffhanger samples
        "We'll dig into that bombshell in a moment, but first...",
        "And this actually connects to something wild we'll get to later — I don't want to spoil it yet.",
        "Keep that in mind, because it's about to become very relevant.",

        // Spontaneous-interruption samples
        "Wait, wait — did you say 100x?!",
        "Hold on, I'm not buying that. Isn't that exactly what they said last year?",
        "Okay you lost me — back up. What does that actually mean?",
        "Oh! That reminds me of what we just talked about with...",
        "See, I actually think that's completely wrong, and here's why...",
        "No no, let me finish this part because it changes everything.",
        "Wait — hold on, does that mean...",
        "Okay but that sounds like...",

        // "Coming up:" teaser sample
        "Coming up: AI agents going rogue, a model that halves its own memory, and the security flaw nobody's talking about.",

        // Speaker-transition samples
        "That's a great point. Now I'm curious about...",
        "Interesting — speaking of which...",
        "What do you make of this, Jarno?"
    )
}
