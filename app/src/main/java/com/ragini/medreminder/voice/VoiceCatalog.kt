package com.ragini.medreminder.voice

/** Languages the reminder can speak. The phone's own text-to-speech engine provides the voices. */
enum class Lang(val tag: String, val title: String, val native: String) {
    EN_IN("en-IN", "English · India", "English"),
    EN_US("en-US", "English · US", "English"),
    EN_GB("en-GB", "English · UK", "English"),
    EN_AU("en-AU", "English · Australia", "English"),
    HI("hi-IN", "Hindi", "हिन्दी"),
    MR("mr-IN", "Marathi", "मराठी"),
    GU("gu-IN", "Gujarati", "ગુજરાતી"),
    BN("bn-IN", "Bengali", "বাংলা"),
    TA("ta-IN", "Tamil", "தமிழ்"),
    TE("te-IN", "Telugu", "తెలుగు"),
    KN("kn-IN", "Kannada", "ಕನ್ನಡ"),
    ML("ml-IN", "Malayalam", "മലയാളം");

    val isEnglish: Boolean get() = tag.startsWith("en")
}

/** The personality of a voice: how high, how fast, which words, which chime. */
enum class Mood(val title: String, val blurb: String, val pitch: Float, val rate: Float) {
    GENTLE("Gentle Dawn", "Soft and unhurried, like a morning hello", 1.05f, 0.85f),
    CHEERFUL("Sunny Cheer", "Bright, warm and encouraging", 1.15f, 1.0f),
    CALM("Monsoon Calm", "Slow, deep and soothing", 0.92f, 0.78f),
    FIRM("Doctor's Desk", "Clear, precise and to the point", 1.0f, 0.92f),
    DEVOTIONAL("Temple Bell", "A blessing with every reminder", 1.0f, 0.82f),
    MELODY("Raga Melody", "Lilting and musical", 1.1f, 0.88f),
    GRANDCHILD("Little Grandchild", "Playful, high and full of love", 1.45f, 1.02f),
    ELDER("Wise Elder", "Steady, respectful and reassuring", 0.82f, 0.8f),
    BRISK("Quick Nudge", "Short and quick – just the essentials", 1.0f, 1.12f)
}

data class VoicePersona(
    val id: String,
    val name: String,
    val lang: Lang,
    val mood: Mood,
    val female: Boolean,
    val variant: Int
) {
    /** Kept inside a range where every word stays crystal clear. */
    val pitch: Float
        get() = when {
            mood == Mood.GRANDCHILD -> mood.pitch
            female -> mood.pitch * 1.04f
            else -> mood.pitch * 0.9f
        }.coerceIn(0.8f, 1.3f)
    val rate: Float get() = mood.rate.coerceIn(0.75f, 0.92f)
    val title: String get() = "$name · ${mood.title}"
}

/**
 * 108 voices – 12 languages × 9 personalities. (108 is the number of beads on a mala.)
 * Each persona picks a different installed voice where the phone has several for that language,
 * and shapes it with its own pitch, speed and words.
 */
object VoiceCatalog {
    private val names: Map<Lang, List<String>> = mapOf(
        Lang.EN_IN to listOf("Asha", "Rohan", "Kavya", "Arjun", "Diya", "Vikram", "Anaya", "Nikhil", "Isha"),
        Lang.EN_US to listOf("Grace", "Daniel", "Emma", "Michael", "Lily", "James", "Chloe", "Ethan", "Ruby"),
        Lang.EN_GB to listOf("Charlotte", "Oliver", "Amelia", "Harry", "Sophie", "George", "Poppy", "Henry", "Alice"),
        Lang.EN_AU to listOf("Matilda", "Jack", "Zoe", "Lachlan", "Ivy", "Cooper", "Willow", "Noah", "Ella"),
        Lang.HI to listOf("Meera", "Aarav", "Radha", "Kabir", "Gauri", "Dev", "Anjali", "Raghav", "Pooja"),
        Lang.MR to listOf("Sai", "Shreyas", "Ketaki", "Omkar", "Mrunal", "Siddharth", "Janhavi", "Ninad", "Gargi"),
        Lang.GU to listOf("Hetal", "Parth", "Krupa", "Jignesh", "Dhara", "Chirag", "Riddhi", "Harsh", "Nidhi"),
        Lang.BN to listOf("Tanushree", "Arnab", "Mou", "Sourav", "Riya", "Abir", "Paromita", "Indranil", "Ishani"),
        Lang.TA to listOf("Kavitha", "Karthik", "Lakshmi", "Surya", "Anitha", "Murugan", "Divya", "Senthil", "Nila"),
        Lang.TE to listOf("Sravani", "Venkat", "Lasya", "Ravi", "Keerthi", "Sai Kiran", "Harika", "Pavan", "Sirisha"),
        Lang.KN to listOf("Shwetha", "Prakash", "Bhoomika", "Darshan", "Spoorthi", "Manju", "Ananya", "Kiran", "Rashmi"),
        Lang.ML to listOf("Anju", "Arun", "Gayathri", "Vishnu", "Nimisha", "Midhun", "Parvathy", "Sreejith", "Aswathy")
    )

    val all: List<VoicePersona> = buildList {
        for (lang in Lang.entries) {
            Mood.entries.forEachIndexed { i, mood ->
                add(
                    VoicePersona(
                        id = "${lang.tag}-${mood.name.lowercase()}",
                        name = names.getValue(lang)[i],
                        lang = lang,
                        mood = mood,
                        female = i % 2 == 0,
                        variant = i
                    )
                )
            }
        }
    }

    fun byId(id: String?): VoicePersona? = all.firstOrNull { it.id == id }

    val fallback: VoicePersona get() = all.first()
}
