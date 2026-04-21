package com.namesupport.util

object HebrewTransliterator {

    // Full-word dictionary: common Hebrew names → preferred English spelling.
    // Checked before character-by-character fallback.
    private val dictionary: Map<String, String> = mapOf(
        // Male given names
        "יוסי" to "Yossi",       "יוסף" to "Yosef",       "משה" to "Moshe",
        "דוד" to "David",         "יצחק" to "Yitzhak",      "שלמה" to "Shlomo",
        "אברהם" to "Avraham",     "יעקב" to "Yaakov",       "שמואל" to "Shmuel",
        "בנימין" to "Binyamin",   "ראובן" to "Reuven",      "שמעון" to "Shimon",
        "לוי" to "Levi",          "יהודה" to "Yehuda",      "דן" to "Dan",
        "אשר" to "Asher",         "אריאל" to "Ariel",       "עמית" to "Amit",
        "ליאור" to "Lior",        "גיל" to "Gil",           "רון" to "Ron",
        "עידו" to "Ido",          "נדב" to "Nadav",         "ירון" to "Yaron",
        "אלון" to "Alon",         "ניר" to "Nir",           "עמיר" to "Amir",
        "אייל" to "Eyal",         "טל" to "Tal",            "שי" to "Shai",
        "רועי" to "Roi",          "אביב" to "Aviv",         "אורן" to "Oren",
        "גבריאל" to "Gavriel",   "מיכאל" to "Michael",     "דניאל" to "Daniel",
        "יונתן" to "Yonatan",     "יהונתן" to "Yehonatan",  "אבי" to "Avi",
        "ישי" to "Yishai",        "נועם" to "Noam",         "גל" to "Gal",
        "אורי" to "Uri",          "איתמר" to "Itamar",      "בועז" to "Boaz",
        "ינון" to "Yinon",        "עוז" to "Oz",            "שחר" to "Shachar",
        "תומר" to "Tomer",        "חיים" to "Chaim",        "זאב" to "Zeev",
        "ברוך" to "Baruch",       "אהרן" to "Aharon",       "ישראל" to "Israel",
        "נתן" to "Natan",         "מרדכי" to "Mordechai",   "פינחס" to "Pinchas",
        "אליהו" to "Eliyahu",     "אלי" to "Eli",           "נחמן" to "Nachman",
        "נחום" to "Nachum",       "עזריאל" to "Azriel",     "ליאב" to "Liav",
        "עדן" to "Eden",          "ראם" to "Raam",           "עתי" to "Etai",
        // Female given names
        "שרה" to "Sarah",         "רחל" to "Rachel",        "רבקה" to "Rivka",
        "לאה" to "Leah",          "מרים" to "Miriam",       "דינה" to "Dina",
        "נועה" to "Noa",          "תמר" to "Tamar",         "נעמי" to "Naomi",
        "חנה" to "Hana",          "שושנה" to "Shoshana",    "אסתר" to "Esther",
        "זהבה" to "Zahava",       "יפה" to "Yafa",          "רות" to "Ruth",
        "דבורה" to "Devora",      "יהודית" to "Yehudit",    "אביגיל" to "Avigail",
        "אורית" to "Orit",        "חגית" to "Hagit",        "דלית" to "Dalit",
        "שירה" to "Shira",        "ענת" to "Anat",          "גלית" to "Galit",
        "יעל" to "Yael",          "מיכל" to "Michal",       "רינה" to "Rina",
        "לילך" to "Lilach",       "אילנה" to "Ilana",       "עדי" to "Adi",
        "מאיה" to "Maya",         "רוני" to "Roni",         "ליהי" to "Lihi",
        "שקד" to "Shaked",        "נגה" to "Noga",          "ורד" to "Vered",
        "הדס" to "Hadas",         "טלי" to "Tali",          "יפית" to "Yafit",
        "מיטל" to "Mital",        "ליאת" to "Liat",         "ספיר" to "Sapir",
        "אגם" to "Agam",          "שלי" to "Sheli",         "רז" to "Raz",
        "יוכבד" to "Yocheved",
        // Family names
        "כהן" to "Cohen",         "מזרחי" to "Mizrachi",    "פרץ" to "Peretz",
        "ביטון" to "Biton",       "כץ" to "Katz",           "גורן" to "Goren",
        "ארז" to "Erez",          "שלום" to "Shalom",       "דהן" to "Dahan",
        "אלמוג" to "Almog",       "גאון" to "Gaon",         "שפירא" to "Shapira",
        "שמיר" to "Shamir",       "בן" to "Ben",            "בר" to "Bar",
        "זיו" to "Ziv",           "טוב" to "Tov",           "אוחיון" to "Ohayion",
    )

    // Default transliteration per Hebrew letter (fricative/spirant forms)
    private val charMap: Map<Char, String> = mapOf(
        'א' to "a",  'ב' to "v",  'ג' to "g",  'ד' to "d",
        'ה' to "h",  'ו' to "v",  'ז' to "z",  'ח' to "ch",
        'ט' to "t",  'י' to "y",  'כ' to "ch", 'ך' to "ch",
        'ל' to "l",  'מ' to "m",  'ם' to "m",  'נ' to "n",
        'ן' to "n",  'ס' to "s",  'ע' to "",   'פ' to "f",
        'ף' to "f",  'צ' to "tz", 'ץ' to "tz", 'ק' to "k",
        'ר' to "r",  'ש' to "sh", 'ת' to "t",
    )

    // Stop forms used when the letter is word-initial (has implied dagesh)
    private val initialCharMap: Map<Char, String> = mapOf(
        'ב' to "b",
        'כ' to "k",
        'פ' to "p",
    )

    fun containsHebrew(text: String): Boolean =
        text.any { it in 'א'..'ת' }

    fun transliterate(hebrewName: String): String =
        hebrewName
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { transliterateWord(it.trim()) }
            .trim()

    private fun transliterateWord(word: String): String {
        val stripped = stripNikud(word)

        // Return non-Hebrew words unchanged (e.g. "Cohen" in a mixed name)
        if (!containsHebrew(stripped)) return word

        // Full-word dictionary lookup first
        dictionary[stripped]?.let { return it }

        // Character-by-character fallback
        return buildString {
            stripped.forEachIndexed { i, ch ->
                val mapped = when {
                    i == 0 && ch in initialCharMap -> initialCharMap[ch]!!
                    ch in charMap                  -> charMap[ch]!!
                    ch.isLetter()                  -> ch.toString() // keep Latin letters
                    else                           -> ""            // skip nikud remnants
                }
                append(mapped)
            }
        }
            .replaceFirstChar { it.uppercase() }
            .ifEmpty { word }
    }

    // Strip Hebrew vowel points (nikud) — U+0591..U+05C7
    private fun stripNikud(text: String): String =
        text.filter { it.code !in 0x0591..0x05C7 }
}
