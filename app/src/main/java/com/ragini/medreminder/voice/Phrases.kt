package com.ragini.medreminder.voice

import com.ragini.medreminder.data.FoodRule
import com.ragini.medreminder.data.Medicine
import com.ragini.medreminder.schedule.RingKind

/**
 * Builds the exact words that are spoken.
 *
 * The core message is always the same plain, unmistakable sentence –
 * "It is time to take your medicine" – followed by every medicine ONE BY ONE:
 * "Number 1: Metformin, 500 mg, with food. Number 2: …". The personality only adds a short
 * greeting or blessing around it, never in place of it.
 */
object Phrases {

    // ------------------------------------------------------------------ English

    private fun opener(m: Mood) = when (m) {
        Mood.CHEERFUL, Mood.GRANDCHILD -> "Hello! "
        Mood.FIRM -> "Attention please. "
        Mood.DEVOTIONAL -> "Namaste. "
        else -> ""
    }

    private fun closer(m: Mood) = when (m) {
        Mood.GENTLE -> " Take care."
        Mood.CHEERFUL -> " You are doing wonderfully!"
        Mood.CALM -> " Slowly, with some water."
        Mood.DEVOTIONAL -> " God bless you."
        Mood.GRANDCHILD -> " I love you!"
        Mood.ELDER -> " Good health is the greatest wealth."
        Mood.MELODY -> " Stay well."
        else -> ""
    }

    private val foodEn = mapOf(
        FoodRule.BEFORE to "before food", FoodRule.AFTER to "after food", FoodRule.WITH to "with food",
        FoodRule.EMPTY to "on an empty stomach", FoodRule.BEDTIME to "at bedtime"
    )
    private val foodHi = mapOf(
        FoodRule.BEFORE to "खाने से पहले", FoodRule.AFTER to "खाने के बाद", FoodRule.WITH to "खाने के साथ",
        FoodRule.EMPTY to "खाली पेट", FoodRule.BEDTIME to "सोने से पहले"
    )
    private val foodMr = mapOf(
        FoodRule.BEFORE to "जेवणाआधी", FoodRule.AFTER to "जेवणानंतर", FoodRule.WITH to "जेवणासोबत",
        FoodRule.EMPTY to "उपाशी पोटी", FoodRule.BEDTIME to "झोपण्यापूर्वी"
    )

    // ------------------------------------------------------------------ Indian languages

    /** dueOne uses {med}; dueMany uses {n}; item uses {i} and {med}. */
    private class L(
        val dueOne: String, val dueMany: String, val item: String, val endMany: String,
        val notYet: String, val pre: String, val greet: String, val love: String
    )

    private val indic = mapOf(
        Lang.HI to L(
            "{p}अब दवा लेने का समय हो गया है। कृपया {med} अभी लीजिए।",
            "{p}अब दवा लेने का समय हो गया है। अभी आपको {n} दवाइयाँ लेनी हैं।",
            "नंबर {i}: {med}।",
            "हर दवा लेने के बाद Taken बटन दबाइए।",
            "आपने अभी तक Taken नहीं दबाया है।",
            "{p}{n} मिनट में दवा लेने का समय होगा: {med}।",
            "नमस्ते।", "अपना ध्यान रखिए।"
        ),
        Lang.MR to L(
            "{p}आता औषध घेण्याची वेळ झाली आहे. कृपया {med} आता घ्या.",
            "{p}आता औषध घेण्याची वेळ झाली आहे. आता तुम्हाला {n} औषधे घ्यायची आहेत.",
            "क्रमांक {i}: {med}.",
            "प्रत्येक औषध घेतल्यावर Taken बटण दाबा.",
            "तुम्ही अजून Taken दाबलेले नाही.",
            "{p}{n} मिनिटांत औषध घेण्याची वेळ होईल: {med}.",
            "नमस्कार.", "काळजी घ्या."
        ),
        Lang.GU to L(
            "{p}હવે દવા લેવાનો સમય થઈ ગયો છે. કૃપા કરીને {med} હમણાં લો.",
            "{p}હવે દવા લેવાનો સમય થઈ ગયો છે. અત્યારે તમારે {n} દવાઓ લેવાની છે.",
            "નંબર {i}: {med}.",
            "દરેક દવા લીધા પછી Taken બટન દબાવો.",
            "તમે હજી Taken દબાવ્યું નથી.",
            "{p}{n} મિનિટમાં દવા લેવાનો સમય થશે: {med}.",
            "નમસ્તે.", "તમારું ધ્યાન રાખજો."
        ),
        Lang.BN to L(
            "{p}এখন ওষুধ খাওয়ার সময় হয়েছে। দয়া করে {med} এখনই খান।",
            "{p}এখন ওষুধ খাওয়ার সময় হয়েছে। এখন আপনাকে {n}টি ওষুধ খেতে হবে।",
            "নম্বর {i}: {med}।",
            "প্রতিটি ওষুধ খাওয়ার পর Taken বোতাম টিপুন।",
            "আপনি এখনও Taken টেপেননি।",
            "{p}{n} মিনিট পরে ওষুধ খাওয়ার সময় হবে: {med}।",
            "নমস্কার।", "নিজের যত্ন নিন।"
        ),
        Lang.TA to L(
            "{p}இப்போது மருந்து எடுக்கும் நேரம் வந்துவிட்டது. தயவுசெய்து {med} இப்போதே எடுத்துக்கொள்ளுங்கள்.",
            "{p}இப்போது மருந்து எடுக்கும் நேரம் வந்துவிட்டது. இப்போது நீங்கள் {n} மருந்துகள் எடுக்க வேண்டும்.",
            "எண் {i}: {med}.",
            "ஒவ்வொரு மருந்தையும் எடுத்த பிறகு Taken பொத்தானை அழுத்துங்கள்.",
            "நீங்கள் இன்னும் Taken அழுத்தவில்லை.",
            "{p}இன்னும் {n} நிமிடங்களில் மருந்து எடுக்கும் நேரம்: {med}.",
            "வணக்கம்.", "உடம்பைப் பார்த்துக்கொள்ளுங்கள்."
        ),
        Lang.TE to L(
            "{p}ఇప్పుడు మందు వేసుకునే సమయం అయింది. దయచేసి {med} ఇప్పుడే వేసుకోండి.",
            "{p}ఇప్పుడు మందు వేసుకునే సమయం అయింది. ఇప్పుడు మీరు {n} మందులు వేసుకోవాలి.",
            "నంబర్ {i}: {med}.",
            "ప్రతి మందు వేసుకున్న తర్వాత Taken బటన్ నొక్కండి.",
            "మీరు ఇంకా Taken నొక్కలేదు.",
            "{p}ఇంకా {n} నిమిషాల్లో మందు వేసుకునే సమయం: {med}.",
            "నమస్కారం.", "జాగ్రత్తగా ఉండండి."
        ),
        Lang.KN to L(
            "{p}ಈಗ ಔಷಧಿ ತೆಗೆದುಕೊಳ್ಳುವ ಸಮಯವಾಗಿದೆ. ದಯವಿಟ್ಟು {med} ಈಗಲೇ ತೆಗೆದುಕೊಳ್ಳಿ.",
            "{p}ಈಗ ಔಷಧಿ ತೆಗೆದುಕೊಳ್ಳುವ ಸಮಯವಾಗಿದೆ. ಈಗ ನೀವು {n} ಔಷಧಿಗಳನ್ನು ತೆಗೆದುಕೊಳ್ಳಬೇಕು.",
            "ಸಂಖ್ಯೆ {i}: {med}.",
            "ಪ್ರತಿ ಔಷಧಿ ತೆಗೆದುಕೊಂಡ ನಂತರ Taken ಗುಂಡಿ ಒತ್ತಿ.",
            "ನೀವು ಇನ್ನೂ Taken ಒತ್ತಿಲ್ಲ.",
            "{p}ಇನ್ನು {n} ನಿಮಿಷಗಳಲ್ಲಿ ಔಷಧಿ ತೆಗೆದುಕೊಳ್ಳುವ ಸಮಯ: {med}.",
            "ನಮಸ್ಕಾರ.", "ಆರೋಗ್ಯ ಕಾಪಾಡಿಕೊಳ್ಳಿ."
        ),
        Lang.ML to L(
            "{p}ഇപ്പോൾ മരുന്ന് കഴിക്കാനുള്ള സമയമായി. ദയവായി {med} ഇപ്പോൾ തന്നെ കഴിക്കൂ.",
            "{p}ഇപ്പോൾ മരുന്ന് കഴിക്കാനുള്ള സമയമായി. ഇപ്പോൾ നിങ്ങൾ {n} മരുന്നുകൾ കഴിക്കണം.",
            "നമ്പർ {i}: {med}.",
            "ഓരോ മരുന്നും കഴിച്ച ശേഷം Taken ബട്ടൺ അമർത്തൂ.",
            "നിങ്ങൾ ഇതുവരെ Taken അമർത്തിയിട്ടില്ല.",
            "{p}ഇനി {n} മിനിറ്റിനുള്ളിൽ മരുന്ന് കഴിക്കാനുള്ള സമയം: {med}.",
            "നമസ്കാരം.", "ആരോഗ്യം ശ്രദ്ധിക്കൂ."
        )
    )

    // ------------------------------------------------------------------ building

    private fun item(m: Medicine, lang: Lang): String {
        val parts = mutableListOf(m.sayName.trim())
        if (m.dosage.isNotBlank()) parts += m.dosage.trim()
        val food = when {
            lang.isEnglish -> foodEn[m.food]
            lang == Lang.HI -> foodHi[m.food]
            lang == Lang.MR -> foodMr[m.food]
            else -> null
        }
        if (food != null) parts += food
        return parts.joinToString(", ")
    }

    private fun cap(s: String) = if (s.isEmpty()) s else s.substring(0, 1).uppercase() + s.substring(1)

    /**
     * @param minutes minutes before the dose (for the early reminders)
     * @param meds every medicine due at this moment – there is no limit on how many
     */
    fun build(persona: VoicePersona, kind: RingKind, minutes: Int, userName: String, meds: List<Medicine>): String {
        if (meds.isEmpty()) return ""
        val name = userName.trim()
        val lang = persona.lang
        val mood = persona.mood
        return if (lang.isEnglish) english(kind, minutes, name, meds, mood) else indian(kind, minutes, name, meds, lang, mood)
    }

    private fun english(kind: RingKind, minutes: Int, name: String, meds: List<Medicine>, mood: Mood): String {
        val p = if (name.isBlank()) "" else "$name, "
        if (kind == RingKind.PRE) {
            val n = if (minutes == 1) "1 minute" else "$minutes minutes"
            val list = meds.map { it.sayName.trim() }
            val what = if (list.size == 1) list[0]
            else "your ${list.size} medicines: " + list.dropLast(1).joinToString(", ") + " and " + list.last()
            return cap("${p}a reminder. In $n, it will be time to take $what.")
        }
        val sb = StringBuilder()
        if (kind == RingKind.FOLLOW) sb.append(cap("${p}you have not tapped Taken yet. ")) else sb.append(opener(mood))
        val who = if (kind == RingKind.FOLLOW) "" else p
        if (meds.size == 1) {
            sb.append(cap("${who}it is time to take your medicine. "))
            sb.append("Please take ${item(meds[0], Lang.EN_IN)}, now. Then tap Taken.")
        } else {
            sb.append(cap("${who}it is time to take your medicines. "))
            sb.append("You have ${meds.size} medicines to take now. ")
            meds.forEachIndexed { i, m -> sb.append("Number ${i + 1}: ${item(m, Lang.EN_IN)}. ") }
            sb.append("Please take each one, and tap Taken for each.")
        }
        if (kind == RingKind.DUE) sb.append(closer(mood))
        return sb.toString().trim()
    }

    private fun indian(kind: RingKind, minutes: Int, name: String, meds: List<Medicine>, lang: Lang, mood: Mood): String {
        val l = indic.getValue(lang)
        val p = if (name.isBlank()) "" else "$name, "
        if (kind == RingKind.PRE) {
            return l.pre.replace("{p}", p).replace("{n}", "$minutes").replace("{med}", meds.joinToString(", ") { it.sayName.trim() })
        }
        val sb = StringBuilder()
        if (kind == RingKind.FOLLOW) sb.append(l.notYet).append(' ')
        else if (mood == Mood.DEVOTIONAL) sb.append(l.greet).append(' ')
        if (meds.size == 1) {
            sb.append(l.dueOne.replace("{p}", p).replace("{med}", item(meds[0], lang)))
        } else {
            sb.append(l.dueMany.replace("{p}", p).replace("{n}", "${meds.size}")).append(' ')
            meds.forEachIndexed { i, m -> sb.append(l.item.replace("{i}", "${i + 1}").replace("{med}", item(m, lang))).append(' ') }
            sb.append(l.endMany)
        }
        if (kind == RingKind.DUE && (mood == Mood.GENTLE || mood == Mood.GRANDCHILD)) sb.append(' ').append(l.love)
        return sb.toString().trim()
    }

    /** What a voice says when it is tapped in the Voices list. */
    fun sample(persona: VoicePersona, userName: String): String {
        val demo = Medicine(name = "Vitamin D", dosage = "1 tablet", times = listOf(480), startDate = java.time.LocalDate.now())
        return build(persona, RingKind.DUE, 0, userName, listOf(demo))
    }

    fun test(persona: VoicePersona, userName: String): String {
        val p = if (userName.isBlank()) "" else "${userName.trim()}, "
        return when (persona.lang) {
            Lang.HI -> "${p}नमस्ते। यह रागिनी की जाँच है। अगर आप मुझे सुन पा रहे हैं, तो सब ठीक है।"
            Lang.MR -> "${p}नमस्कार. ही रागिनीची चाचणी आहे. तुम्हाला मी ऐकू येत असेल, तर सर्व ठीक आहे."
            else -> "Hello ${p}this is a test reminder from Ragini. If you can hear me, your reminders are working perfectly."
        }
    }

    fun lowStock(meds: List<Medicine>, userName: String): String {
        val p = if (userName.isBlank()) "" else "${userName.trim()}, "
        return "${p}your ${meds.joinToString(" and ") { it.sayName }} is running low. Please ask for a refill."
    }

    /** Spoken when the phone battery is low, so the reminders can keep working. */
    fun lowBattery(persona: VoicePersona, percent: Int): String = when (persona.lang) {
        Lang.HI -> "फ़ोन की बैटरी $percent प्रतिशत है। कृपया फ़ोन को चार्ज पर लगाइए, ताकि मैं आपको याद दिलाती रहूँ।"
        Lang.MR -> "फोनची बॅटरी $percent टक्के आहे. कृपया फोन चार्जिंगला लावा, म्हणजे मी तुम्हाला आठवण करून देत राहीन."
        else -> "The phone battery is at $percent percent. Please put the phone on charge, so I can keep reminding you."
    }
}
