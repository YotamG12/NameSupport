package com.namesupport.util

object HebrewTransliterator {

    // Full-word dictionary: common Hebrew names/words → preferred English spelling.
    // SmartTransliterator checks this first before hitting the Claude API.
    private val dictionary: Map<String, String> = mapOf(
        // ── Relationship words (common in Israeli contact lists) ───────────────
        "אבא"    to "Abba",        "אמא"    to "Ima",
        "סבא"    to "Saba",        "סבתא"   to "Savta",
        "דודה"   to "Doda",        "אחות"   to "Achot",
        "אח"     to "Ach",         "אחי"    to "Achi",
        "חבר"    to "Chaver",      "חברה"   to "Chavera",
        "נכד"    to "Neched",      "נכדה"   to "Nechda",
        "גיסה"   to "Gisa",        "גיס"    to "Gis",

        // ── Male given names ──────────────────────────────────────────────────
        "יוסי"   to "Yossi",       "יוסף"   to "Yosef",       "משה"    to "Moshe",
        "דוד"    to "David",       "יצחק"   to "Yitzhak",     "שלמה"   to "Shlomo",
        "אברהם"  to "Avraham",     "יעקב"   to "Yaakov",      "שמואל"  to "Shmuel",
        "בנימין" to "Binyamin",    "ראובן"  to "Reuven",      "שמעון"  to "Shimon",
        "לוי"    to "Levi",        "יהודה"  to "Yehuda",      "דן"     to "Dan",
        "אשר"    to "Asher",       "אריאל"  to "Ariel",       "עמית"   to "Amit",
        "ליאור"  to "Lior",        "גיל"    to "Gil",         "רון"    to "Ron",
        "עידו"   to "Ido",         "נדב"    to "Nadav",       "ירון"   to "Yaron",
        "אלון"   to "Alon",        "ניר"    to "Nir",         "עמיר"   to "Amir",
        "אייל"   to "Eyal",        "טל"     to "Tal",         "שי"     to "Shai",
        "רועי"   to "Roi",         "אביב"   to "Aviv",        "אורן"   to "Oren",
        "גבריאל" to "Gavriel",     "מיכאל"  to "Michael",     "דניאל"  to "Daniel",
        "יונתן"  to "Yonatan",     "יהונתן" to "Yehonatan",  "אבי"    to "Avi",
        "ישי"    to "Yishai",      "נועם"   to "Noam",        "גל"     to "Gal",
        "אורי"   to "Uri",         "איתמר"  to "Itamar",      "בועז"   to "Boaz",
        "ינון"   to "Yinon",       "עוז"    to "Oz",          "שחר"    to "Shachar",
        "תומר"   to "Tomer",       "חיים"   to "Chaim",       "זאב"    to "Zeev",
        "ברוך"   to "Baruch",      "אהרן"   to "Aharon",      "ישראל"  to "Israel",
        "נתן"    to "Natan",       "מרדכי"  to "Mordechai",   "פינחס"  to "Pinchas",
        "אליהו"  to "Eliyahu",     "אלי"    to "Eli",         "נחמן"   to "Nachman",
        "נחום"   to "Nachum",      "עזריאל" to "Azriel",      "ליאב"   to "Liav",
        "עדן"    to "Eden",        "ראם"    to "Raam",        "עתי"    to "Etai",
        "יובל"   to "Yuval",       "אביחי"  to "Avichai",     "עמנואל" to "Emanuel",
        "שלום"   to "Shalom",      "בן"     to "Ben",         "ניסים"  to "Nissim",
        "שגיא"   to "Sagi",        "איתי"   to "Itay",        "יאיר"   to "Yair",
        "אמיר"   to "Amir",        "גדעון"  to "Gideon",      "פנחס"   to "Pinchas",

        // ── Female given names ────────────────────────────────────────────────
        "שרה"    to "Sarah",       "רחל"    to "Rachel",      "רבקה"   to "Rivka",
        "לאה"    to "Leah",        "מרים"   to "Miriam",      "דינה"   to "Dina",
        "נועה"   to "Noa",         "תמר"    to "Tamar",       "נעמי"   to "Naomi",
        "חנה"    to "Hana",        "שושנה"  to "Shoshana",    "אסתר"   to "Esther",
        "זהבה"   to "Zahava",      "יפה"    to "Yafa",        "רות"    to "Ruth",
        "דבורה"  to "Devora",      "יהודית" to "Yehudit",     "אביגיל" to "Avigail",
        "אורית"  to "Orit",        "חגית"   to "Hagit",       "דלית"   to "Dalit",
        "שירה"   to "Shira",       "ענת"    to "Anat",        "גלית"   to "Galit",
        "יעל"    to "Yael",        "מיכל"   to "Michal",      "רינה"   to "Rina",
        "לילך"   to "Lilach",      "אילנה"  to "Ilana",       "עדי"    to "Adi",
        "מאיה"   to "Maya",        "רוני"   to "Roni",        "ליהי"   to "Lihi",
        "שקד"    to "Shaked",      "נגה"    to "Noga",        "ורד"    to "Vered",
        "הדס"    to "Hadas",       "טלי"    to "Tali",        "יפית"   to "Yafit",
        "מיטל"   to "Mital",       "ליאת"   to "Liat",        "ספיר"   to "Sapir",
        "אגם"    to "Agam",        "שלי"    to "Sheli",       "רז"     to "Raz",
        "יוכבד"  to "Yocheved",    "גאולה"  to "Geula",       "פנינה"  to "Pnina",
        "רוחמה"  to "Ruchama",     "חיה"    to "Chaya",       "בת"     to "Bat",
        "יסמין"  to "Yasmin",      "שני"    to "Shani",       "דנית"   to "Danit",

        // ── Family names ──────────────────────────────────────────────────────
        "כהן"    to "Cohen",       "מזרחי"  to "Mizrachi",    "פרץ"    to "Peretz",
        "ביטון"  to "Biton",       "כץ"     to "Katz",        "גורן"   to "Goren",
        "ארז"    to "Erez",        "דהן"    to "Dahan",       "אלמוג"  to "Almog",
        "גאון"   to "Gaon",        "שפירא"  to "Shapira",     "שמיר"   to "Shamir",
        "זיו"    to "Ziv",         "טוב"    to "Tov",         "אוחיון" to "Ohayion",
        "לוי"    to "Levi",        "אברג'יל" to "Aberjil",    "בן"     to "Ben",
        "בר"     to "Bar",         "פלד"    to "Plad",        "אמסלם"  to "Amsalem",
        "חדד"    to "Hadad",       "נחמיאס" to "Nachmias",    "ביבי"   to "Bibi",
    )

    // Default transliteration (spirant/fricative forms used mid-word)
    private val charMap: Map<Char, String> = mapOf(
        'א' to "",   'ב' to "v",  'ג' to "g",  'ד' to "d",
        'ה' to "h",  'ו' to "o",  'ז' to "z",  'ח' to "ch",
        'ט' to "t",  'י' to "i",  'כ' to "ch", 'ך' to "ch",
        'ל' to "l",  'מ' to "m",  'ם' to "m",  'נ' to "n",
        'ן' to "n",  'ס' to "s",  'ע' to "",   'פ' to "f",
        'ף' to "f",  'צ' to "tz", 'ץ' to "tz", 'ק' to "k",
        'ר' to "r",  'ש' to "sh", 'ת' to "t",
    )

    // Word-initial overrides (implied dagesh / hard pronunciation)
    private val initialCharMap: Map<Char, String> = mapOf(
        'ב' to "b",
        'כ' to "k",
        'פ' to "p",
        'ו' to "v",
        'א' to "a",
        'ע' to "a",
    )

    fun containsHebrew(text: String): Boolean =
        text.any { it in 'א'..'ת' }

    /** Looks up [word] (after stripping nikud) in the local dictionary. Null if not found. */
    fun dictionaryLookup(word: String): String? = dictionary[stripNikud(word)]

    /** Character-by-character fallback. [word] should already have nikud stripped. */
    fun transliterateCharByChar(word: String): String {
        if (!containsHebrew(word)) return word
        return buildString {
            word.forEachIndexed { i, ch ->
                append(
                    when {
                        i == 0 && ch in initialCharMap -> initialCharMap[ch]!!
                        ch in charMap                  -> charMap[ch]!!
                        ch.isLetter()                  -> ch.toString()
                        else                           -> ""
                    }
                )
            }
        }
            .replaceFirstChar { it.uppercase() }
            .ifEmpty { word }
    }

    /** Full local transliteration pipeline (used as fallback when SmartTransliterator is unavailable). */
    fun transliterate(hebrewName: String): String =
        hebrewName
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { transliterateWord(it.trim()) }
            .trim()

    private fun transliterateWord(word: String): String {
        val stripped = stripNikud(word)
        if (!containsHebrew(stripped)) return word
        return dictionaryLookup(stripped) ?: transliterateCharByChar(stripped)
    }

    fun stripNikud(text: String): String =
        text.filter { it.code !in 0x0591..0x05C7 }
}
