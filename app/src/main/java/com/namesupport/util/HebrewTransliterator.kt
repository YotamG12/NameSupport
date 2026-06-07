package com.namesupport.util

object HebrewTransliterator {

    // Priority 1: dictionary — covers common names and relational words precisely.
    private val dictionary: Map<String, String> = mapOf(
        // ── Male given names ──────────────────────────────────────────────────
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
        "אורי" to "Ori",          "איתמר" to "Itamar",      "בועז" to "Boaz",
        "ינון" to "Yinon",        "עוז" to "Oz",            "שחר" to "Shachar",
        "תומר" to "Tomer",        "חיים" to "Chaim",        "זאב" to "Zeev",
        "ברוך" to "Baruch",       "אהרן" to "Aharon",       "ישראל" to "Israel",
        "נתן" to "Natan",         "מרדכי" to "Mordechai",   "פינחס" to "Pinchas",
        "אליהו" to "Eliyahu",     "אלי" to "Eli",           "נחמן" to "Nachman",
        "נחום" to "Nachum",       "עזריאל" to "Azriel",     "ליאב" to "Liav",
        "עדן" to "Eden",          "ראם" to "Raam",           "עתי" to "Etai",
        "עומר" to "Omer",         "תום" to "Tom",            "עידן" to "Idan",
        "אדם" to "Adam",          "יואב" to "Yoav",          "גלעד" to "Gilad",
        "יניב" to "Yaniv",        "אסף" to "Asaf",           "רן" to "Ran",
        "אוהד" to "Ohad",         "ניסים" to "Nissim",       "רפאל" to "Rafael",
        "אלכסנדר" to "Alexander", "שאול" to "Shaul",         "חנן" to "Hanan",
        "יאיר" to "Yair",         "איל" to "Eil",            "ממן" to "Maman",
        "גדעון" to "Gideon",      "צבי" to "Tzvi",           "נמרוד" to "Nimrod",
        "אביחי" to "Avichai",     "אמיתי" to "Amitai",       "אופיר" to "Ofir",
        "ברק" to "Barak",         "יובל" to "Yuval",         "מתן" to "Matan",
        "אור" to "Or",            "כרמל" to "Carmel",        "יפתח" to "Yiftach",
        "שמוליק" to "Shmulik",    "נחמיה" to "Nehemia",      "יחזקאל" to "Yechezkel",
        "אלדד" to "Eldad",        "שמריה" to "Shmarya",      "רחמים" to "Rachamim",
        "עמנואל" to "Emanuel",    "אוריאל" to "Uriel",       "חגי" to "Hagai",
        "בני" to "Beni",          "דודי" to "Dudi",          "אבנר" to "Avner",
        "אילן" to "Ilan",         "גיורא" to "Giora",        "מוטי" to "Moti",
        "חנוך" to "Hanoch",       "זלמן" to "Zalman",        "מנחם" to "Menachem",
        "אהוד" to "Ehud",         "יהושע" to "Yehoshua",     "אפרים" to "Ephraim",
        "עזרא" to "Ezra",         "מחמד" to "Mohammed",      "אחמד" to "Ahmed",
        "עלי" to "Ali",           "סמיר" to "Samir",         "חאלד" to "Khaled",
        "ולדימיר" to "Vladimir",  "דימה" to "Dima",          "בוריס" to "Boris",
        "אנדריי" to "Andrei",     "אלכסיי" to "Alexei",      "ניקולאי" to "Nikolai",
        "יבגני" to "Evgeny",
        "יונה" to "Yona",         "אמנון" to "Amnon",        "עוזי" to "Uzi",
        "שמשון" to "Shimshon",    "גרשון" to "Gershon",      "יואל" to "Yoel",
        "מנשה" to "Menashe",      "כפיר" to "Kfir",          "ארנון" to "Arnon",
        "מיקי" to "Miki",         "עמי" to "Ami",             "יוחנן" to "Yochanan",
        "אמיחי" to "Amichai",     "אביעד" to "Aviad",        "צביקה" to "Tzvika",
        "כלב" to "Kalev",         "מלאכי" to "Malachi",      "שרגא" to "Shraga",
        "חמי" to "Hami",          "צחי" to "Tzachi",          "גרשום" to "Gershom",
        "ירמיהו" to "Yirmiyahu",  "אמציה" to "Amatzia",      "חנניה" to "Hananya",
        "יגאל" to "Yigal",        "עקיבא" to "Akiva",        "פז" to "Paz",
        "יהורם" to "Yehoram",     "נריה" to "Neria",          "יוחאי" to "Yochai",
        "מאור" to "Maor",         "ניצן" to "Nitzan",         "אופק" to "Ofek",
        "בצלאל" to "Betzalel",    "יקיר" to "Yakir",          "הראל" to "Harel",
        "ויקטור" to "Victor",     "ואדים" to "Vadim",         "לאוניד" to "Leonid",
        "ולרי" to "Valery",       "ניקיטה" to "Nikita",       "פבל" to "Pavel",
        "ויטלי" to "Vitaly",      "סרגיי" to "Sergei",        "טוביה" to "Tuvya",
        "שבתי" to "Shabtai",      "יורם" to "Yoram",          "ניב" to "Niv",
        "רגב" to "Regev",         "קובי" to "Kobi",           "ירחמיאל" to "Yerachmiel",
        "אלחנן" to "Elchanan",    "מורן" to "Moran",          "אוריה" to "Orya",
        "שחף" to "Shachaf",       "יצהר" to "Yitzhar",        "יהב" to "Yahav",
        "מגן" to "Magen",         "ורדי" to "Vardi",          "אלדר" to "Eldar",
        "צמח" to "Tzemach",       "עובדיה" to "Ovadia",
        // ── Female given names ────────────────────────────────────────────────
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
        "יוכבד" to "Yocheved",    "מלכה" to "Malka",        "פנינה" to "Pnina",
        "בתיה" to "Batya",        "אלה" to "Ella",           "ירדן" to "Yarden",
        "טליה" to "Talia",        "קרן" to "Keren",          "עפרה" to "Ofra",
        "אורה" to "Ora",          "דנה" to "Dana",           "רוית" to "Ravit",
        "לירון" to "Liron",       "יעלה" to "Yaela",        "ענבל" to "Inbal",
        "אלינור" to "Elinor",     "נילי" to "Nili",         "שני" to "Shani",
        "צלילה" to "Tzelila",     "גלי" to "Gali",           "אמירה" to "Amira",
        "חמוטל" to "Hamutal",     "אביטל" to "Avital",       "נטע" to "Neta",
        "ליבי" to "Libi",         "שיר" to "Shir",           "שמחה" to "Simcha",
        "רינת" to "Rinat",        "שרית" to "Sarit",         "נורית" to "Nurit",
        "יפעת" to "Yifat",        "שפרה" to "Shifra",        "ברכה" to "Bracha",
        "חנית" to "Hanit",        "ריקי" to "Riki",          "שולמית" to "Shulamit",
        "אביבה" to "Aviva",       "חביבה" to "Haviva",       "זיוה" to "Ziva",
        "גאולה" to "Geula",       "נאוה" to "Nava",          "גילה" to "Gila",
        "עדינה" to "Adina",       "מרגלית" to "Margalit",    "ציפורה" to "Tzipora",
        "גולדה" to "Golda",       "פייגה" to "Faiga",        "כרמלה" to "Carmela",
        "אסנת" to "Osnat",        "נירית" to "Nirit",        "מרב" to "Merav",
        "נוי" to "Noy",           "שקמה" to "Shikma",        "קשת" to "Keshet",
        "חיה" to "Chaya",         "נטליה" to "Natalia",      "אולגה" to "Olga",
        "ילנה" to "Yelena",       "ורדה" to "Varda",
        "אפרת" to "Efrat",        "הדר" to "Hadar",          "זהבית" to "Zahavit",
        "רביד" to "Ravid",        "תרצה" to "Tirtza",        "אלישבע" to "Elisheva",
        "שלהבת" to "Shalhevet",   "טובה" to "Tova",          "מנוחה" to "Menucha",
        "אתי" to "Eti",           "אירית" to "Irit",          "ציפי" to "Tzipi",
        "ריבי" to "Ribi",         "רחלי" to "Racheli",       "תהילה" to "Tehila",
        "מזל" to "Mazal",         "ניצה" to "Nitza",          "ורדית" to "Vardit",
        "עדנה" to "Edna",         "נעמה" to "Naama",          "שרון" to "Sharon",
        "כרמית" to "Carmit",      "אביה" to "Aviya",          "לינוי" to "Linoy",
        "מרינה" to "Marina",      "נינה" to "Nina",           "גלינה" to "Galina",
        "לריסה" to "Larissa",     "אירנה" to "Irina",         "לודמילה" to "Ludmila",
        "סוניה" to "Sonya",       "נאטשה" to "Natasha",       "ויקטוריה" to "Victoria",
        "ולנטינה" to "Valentina", "ריטה" to "Rita",           "ליזה" to "Liza",
        "אנה" to "Anna",          "לינה" to "Lina",           "אלכסנדרה" to "Alexandra",
        "הילה" to "Hila",         "דפנה" to "Dafna",          "עינב" to "Einav",
        "צילה" to "Tzila",        "שרלי" to "Sharli",         "פרידה" to "Frida",
        "שושי" to "Shoshi",       "ליבה" to "Liba",           "ושתי" to "Vashti",
        "נשמה" to "Neshama",
        // ── Family names ──────────────────────────────────────────────────────
        "כהן" to "Cohen",         "מזרחי" to "Mizrachi",    "פרץ" to "Peretz",
        "ביטון" to "Biton",       "כץ" to "Katz",           "גורן" to "Goren",
        "ארז" to "Erez",          "שלום" to "Shalom",       "דהן" to "Dahan",
        "אלמוג" to "Almog",       "גאון" to "Gaon",         "שפירא" to "Shapira",
        "שמיר" to "Shamir",       "בן" to "Ben",            "בר" to "Bar",
        "זיו" to "Ziv",           "טוב" to "Tov",           "אוחיון" to "Ohayion",
        "גרוסמן" to "Grossman",   "הרצוג" to "Herzog",      "גולדברג" to "Goldberg",
        "רוזנברג" to "Rosenberg", "פרידמן" to "Friedman",   "וייס" to "Weiss",
        "שוורץ" to "Schwartz",    "ברקוביץ" to "Berkowitz",  "ברגמן" to "Bergman",
        "נחמיאס" to "Nachmias",   "בוסקילה" to "Buskila",   "חדד" to "Haddad",
        "אזולאי" to "Azulay",     "חיון" to "Hayun",        "סויסה" to "Swissa",
        "אמסלם" to "Amsalem",     "אוזן" to "Ozen",         "פרנקל" to "Frankel",
        "שטרן" to "Stern",        "כספי" to "Caspi",        "שגב" to "Segev",
        "פלד" to "Peled",         "שמש" to "Shemesh",       "כרמי" to "Karmi",
        "ציון" to "Tzion",        "מנור" to "Manor",        "גפן" to "Gefen",
        "חן" to "Chen",           "אטיאס" to "Attias",      "מימון" to "Maimon",
        "אבוטבול" to "Abotbol",   "שלוש" to "Shlosh",       "פרג" to "Faragi",
        "גולן" to "Golan",        "סלע" to "Sela",           "הלוי" to "Halevi",
        "שביט" to "Shavit",       "אריאלי" to "Arieli",      "שניידר" to "Schneider",
        "קורן" to "Koren",        "שוחט" to "Shochat",       "אהרוני" to "Aharoni",
        "גרינברג" to "Greenberg", "לוינסון" to "Levinson",   "ברנשטיין" to "Bernstein",
        "כהנא" to "Kahana",       "לרנר" to "Lerner",        "שטרית" to "Shtrit",
        "עמר" to "Amar",          "ברוש" to "Brosh",          "ינאי" to "Yanai",
        "בנאי" to "Benaya",       "רביבו" to "Revivo",        "אלבז" to "Albaz",
        "אוחנה" to "Ohana",       "דיין" to "Dayan",          "גינזבורג" to "Ginzburg",
        "קפלן" to "Kaplan",       "ברמן" to "Berman",         "לנדאו" to "Landau",
        "ויינשטיין" to "Weinstein","חגג" to "Hagag",          "אדרי" to "Edery",
        "זגורי" to "Zaguri",      "שרביט" to "Sharvit",
        // ── Ethiopian / Eritrean surnames common in Israel ────────────────────
        "מנגיסטו" to "Mangisto",  "מקונן" to "Makonnen",    "בקלה" to "Bekele",
        "תסמה" to "Tesema",       "גרמה" to "Germa",        "אמארה" to "Amare",
        "טקלה" to "Tekle",        "הייله" to "Haile",       "גברה" to "Gebre",
        "וולדה" to "Wolde",       "זריהון" to "Zerihun",
        // ── Common words → translate to English meaning ───────────────────────
        "אבא" to "Dad",           "אמא" to "Mom",
        "סבא" to "Grandpa",       "סבתא" to "Grandma",
        "אח" to "Bro",            "אחות" to "Sis",
        "דודה" to "Auntie",       "גיס" to "Bro-in-law",    "גיסה" to "Sis-in-law",
        "של" to "of",             "מילואים" to "Miluim",
        "בית" to "Home",          "עבודה" to "Work",
        "רופא" to "Doctor",       "חבר" to "Friend",
        "חברה" to "Friend",       "שכן" to "Neighbor",
        "שכנה" to "Neighbor",     "מנהל" to "Manager",
        "מנהלת" to "Manager",     "בוס" to "Boss",
        "גרוש" to "Ex",           "גרושה" to "Ex",
    )

    // Char-by-char fallback (used when word is not in dictionary).
    private val charMap: Map<Char, String> = mapOf(
        'א' to "a",  'ב' to "v",  'ג' to "g",  'ד' to "d",
        'ה' to "h",  'ו' to "v",  'ז' to "z",  'ח' to "ch",
        'ט' to "t",  'י' to "y",  'כ' to "ch", 'ך' to "ch",
        'ל' to "l",  'מ' to "m",  'ם' to "m",  'נ' to "n",
        'ן' to "n",  'ס' to "s",  'ע' to "",   'פ' to "f",
        'ף' to "f",  'צ' to "tz", 'ץ' to "tz", 'ק' to "k",
        'ר' to "r",  'ש' to "sh", 'ת' to "t",
    )

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
        if (!containsHebrew(stripped)) return word

        // Priority 1: dictionary
        dictionary[stripped]?.let { return it }

        // Priority 2: context-aware char-by-char fallback
        val len = stripped.length
        return buildString {
            stripped.forEachIndexed { i, ch ->
                val isFirst = i == 0
                val isLast = i == len - 1
                val prevIsHebrew = i > 0 && stripped[i - 1] in 'א'..'ת'
                val nextIsHebrew = !isLast && stripped[i + 1] in 'א'..'ת'
                append(when {
                    // Final ה is the feminine suffix — sounds like "a" in Israeli Hebrew
                    ch == 'ה' && isLast -> "a"
                    // ו between two Hebrew letters acts as the "o" vowel
                    ch == 'ו' && prevIsHebrew && nextIsHebrew -> "o"
                    // י after a Hebrew letter acts as the "i" vowel
                    ch == 'י' && prevIsHebrew && (isLast || nextIsHebrew) -> "i"
                    // Initial-position overrides for ב/כ/פ
                    isFirst && ch in initialCharMap -> initialCharMap[ch]!!
                    ch in charMap -> charMap[ch]!!
                    ch.isLetter() -> ch.toString()
                    else -> ""
                })
            }
        }.replaceFirstChar { it.uppercase() }.ifEmpty { word }
    }

    private fun stripNikud(text: String): String =
        text.filter { it.code !in 0x0591..0x05C7 }
}
