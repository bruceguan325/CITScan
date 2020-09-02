package com.intumit.solr.util.chinesetranslate;

import java.util.ArrayList;
import java.util.HashMap;

class Constants {

    public static String HOST_LOCATION = null;
    public static String PROXY_HOST = null;
    public static int PROXY_PORT = 0;
    public static String WIKI_LOOKUP = null;
    public static String FileSystemXmlApplicationContext;
    public static boolean IS_DISPLAY_WIKI_URL = false;
    public static ArrayList<NameValuePair> DICTIONARY_S2T_PHRASE = new ArrayList<NameValuePair>();
    public static ArrayList<NameValuePair> DICTIONARY_S2T_WORD = new ArrayList<NameValuePair>();
    public static ArrayList<NameValuePair> DICTIONARY_T2S_PHRASE = new ArrayList<NameValuePair>();
    public static ArrayList<NameValuePair> DICTIONARY_T2S_WORD = new ArrayList<NameValuePair>();
    public static ArrayList<NameValuePair> DICTIONARY_NO_CHANGE_TW = new ArrayList<NameValuePair>();
    public static ArrayList<NameValuePair> DICTIONARY_NO_CHANGE_CN = new ArrayList<NameValuePair>();
    
    public static HashMap<Character, Character> CHINESE_T2S_S2T_CHAR_MAP = new HashMap<Character, Character>();

    public static String[][] DICTIONARY_PARSER_SETTING = {
        {
            //?��定�?��?��?��?��?�中???
            "dictionaryNoTransferTW.xml",
            "http://zh.wikipedia.org/w/index.php?title=MediaWiki:Conversiontable/zh-tw&variant=zh",
            "window.showTocToggle",
            "<div class=\"printfooter\">",
            "<li>",
            "</li>",
            ";",
            "=>"
        },
        {
            //?��定�?��?��?�簡體中???
            "dictionaryNoTransferCN.xml",
            "http://zh.wikipedia.org/w/index.php?title=MediaWiki:Conversiontable/zh-cn&variant=zh",
            "window.showTocToggle",
            "<div class=\"printfooter\">",
            "<li>",
            "</li>",
            ";",
            "=>"
        },
        {
            //簡�?��?�單�?
            "dictionaryS2TWord.xml",
            "http://zh.wikipedia.org/w/index.php?title=Wikipedia:%E7%B9%81%E7%AE%80%E5%A4%84%E7%90%86/%E7%AE%80%E7%B9%81%E5%8D%95%E5%AD%97&variant=zh",
            "<ul>",
            "<div class=\"printfooter\">",
            "<li>",
            "</li>",
            ";",
            "=>"
        },
        {
            //簡�?��?��?��??
            "dictionaryS2TPhrase.xml",
            "http://zh.wikipedia.org/w/index.php?title=Wikipedia:%E7%B9%81%E7%AE%80%E5%A4%84%E7%90%86/%E7%AE%80%E7%B9%81%E8%AF%8D%E6%B1%87&variant=zh",
            "<ul>",
            "<div class=\"printfooter\">",
            "<li>",
            "</li>",
            ",",
            "=>"
        },
        {
            //繁�?�簡?���?
            "dictionaryT2SWord.xml",
            "http://zh.wikipedia.org/w/index.php?title=Wikipedia:%E7%B9%81%E7%AE%80%E5%A4%84%E7%90%86/%E7%B9%81%E7%AE%80%E5%8D%95%E5%AD%97&variant=zh",
            "<ul>",
            "<div class=\"printfooter\">",
            "<li>",
            "</li>",
            ";",
            "=>"
        },
        {
            //繁�?�簡詞�??
            "dictionaryT2SPhrase.xml",
            "http://zh.wikipedia.org/w/index.php?title=Wikipedia:%E7%B9%81%E7%AE%80%E5%A4%84%E7%90%86/%E7%B9%81%E7%AE%80%E8%AF%8D%E6%B1%87&variant=zh",
            "<ul>",
            "<div class=\"printfooter\">",
            "<li>",
            "</li>",
            ",",
            "=>"
        }
    };

    public static final int LOOP_LIMIT = 10000;
    public static final String SPACE = " ";

    public static final String DICTIONARY_FROM_TAG = "from";
    public static final String DICTIONARY_TO_TAG = "to";

    public static final int WIKI_METHOD_SUMMARY = 0;
    public static final int WIKI_METHOD_ALL = 1;
    public static final int WIKI_METHOD_ORIGINAL = 2;

    public static final String WIKI_END_TAG = "]]";
    public static final String WIKI_WORD_START = "[[";
    public static final String WIKI_WORD_END = "]]";
    public static final String WIKI_EXTENTION_START = "{{";
    public static final String WIKI_EXTENTION_END = "}}";
    public static final String WIKI_SECTION_START = "==";
    public static final String WIKI_SECTION_END = "==";
    public static final String WIKI_TABLE_START1 = "{|";
    public static final String WIKI_TABLE_END1 = "|}";
    public static final String WIKI_TABLE_START2 = "<table";
    public static final String WIKI_TABLE_END2 = "</table>";

    public static final String WIKI_BOLD_AND_ITALIC_START = "'''''";
    public static final String WIKI_BOLD_AND_ITALIC_END = "'''''";
    public static final String HTML_BOLD_AND_ITALIC_START = "<b><i>";
    public static final String HTML_BOLD_AND_ITALIC_END = "</i></b>";
    public static final String WIKI_BOLD_START = "'''";
    public static final String WIKI_BOLD_END = "'''";
    public static final String HTML_BOLD_START = "<b>";
    public static final String HTML_BOLD_END = "</b>";
    public static final String WIKI_ITALIC_START = "''";
    public static final String WIKI_ITALIC_END = "''";
    public static final String HTML_ITALIC_START = "<i>";
    public static final String HTML_ITALIC_END = "</i>";
    public static final String WIKI_HORIZENTAL_LINE = "----";
    public static final String HTML_HORIZENTAL_LINE = "<hr />";
    public static final String WIKI_NEW_LINE1 = "\r\n";
    public static final String WIKI_NEW_LINE2 = "\n";
    public static final String HTML_NEWLINE_LINE = "<br />";
    public static final String WIKI_HTML_START = "[http://";
    public static final String WIKI_HTML_END = "]";
    public static final String WIKI_UL_TAG = "*";
    public static final String WIKI_OL_TAG = "#";
    public static final String HTML_UL_START_TAG = "<ul>";
    public static final String HTML_UL_END_TAG = "</ul>";
    public static final String HTML_OL_START_TAG = "<ol>";
    public static final String HTML_OL_END_TAG = "</ol>";
    public static final String HTML_LI_START_TAG = "<li>";
    public static final String HTML_LI_END_TAG = "</li>";
    public static final String WIKI_ZH_TAG_START = "-{";
    public static final String WIKI_ZH_TAG_END = "}-";
    public static final String WIKI_EXT_REDIRECT_TAG_START = "{{redirect|";
    public static final String WIKI_EXT_REDIRECT_TAG_END = "}}";
    public static final String WIKI_EXT_DISAMBIG_TAG_START = "{{disambig";
    public static final String WIKI_EXT_DISAMBIG_TAG_END = "}}";
    public static final String WIKI_EXT_OTHERUSES_TAG_START = "{{otheruses|";
    public static final String WIKI_EXT_OTHERUSES_TAG_END = "}}";
    public static final String WIKI_EXT_DISAMBIG_PAGE_TAG_START = "{{消歧义页|";
    public static final String WIKI_EXT_DISAMBIG_PAGE_TAG_END = "}}";

//    public static final String LANGUAGE_EN_NAME = "?��???";
//    public static final String LANGUAGE_ZHTW_NAME = "繁�?�中???";
//    public static final String LANGUAGE_ZHCN_NAME = "簡�?�中???";
//    public static final String LANGUAGE_JA_NAME = "?��???";
    public static final String LANGUAGE_EN_VALUE = "en";
    public static final String LANGUAGE_ZHTW_VALUE = "zh-tw";
    public static final String LANGUAGE_ZHCN_VALUE = "zh-cn";
    public static final String LANGUAGE_JA_VALUE = "ja";

    public static final String NO_FOUND = "Not Found!";

    public static final String RECOMMEND = "ok";

//    public static final NameValuePair[] LANGUAGES = {
//        new NameValuePair(LANGUAGE_EN_NAME, LANGUAGE_EN_VALUE),
//        new NameValuePair(LANGUAGE_ZHTW_NAME, LANGUAGE_ZHTW_VALUE),
//        new NameValuePair(LANGUAGE_ZHCN_NAME, LANGUAGE_ZHCN_VALUE),
//        new NameValuePair(LANGUAGE_JA_NAME, LANGUAGE_JA_VALUE)
//    };

    public static final String[] WIKI_LANGUAGE_PRIORITY = {
        LANGUAGE_EN_VALUE, LANGUAGE_ZHTW_VALUE, LANGUAGE_ZHCN_VALUE, LANGUAGE_JA_VALUE
    };

//    public static final String ZH_TW_NAME = "繁�?�中???";
//    public static final String ZH_CN_NAME = "簡�?�中???";
//    public static final String ZH_NO_NAME = "不�?��??";
    public static final String ZH_TW_VALUE = "zh-tw";
    public static final String ZH_CN_VALUE = "zh-cn";
    public static final String ZH_HK_VALUE = "zh-hk";
    public static final String ZH_SG_VALUE = "zh-sg";
    public static final String ZH_NO_VALUE = "zh";

//    public static final NameValuePair[] CHINESE_DISPLAY_TYPES = {
//        new NameValuePair(ZH_TW_NAME, ZH_TW_VALUE),
//        new NameValuePair(ZH_CN_NAME, ZH_CN_VALUE),
//        new NameValuePair(ZH_NO_NAME, ZH_NO_VALUE)
//    };

//    public static final String[][] NON_NECESSARY_TAGS = {
//        {
//            "{|", "|}"
//        },
//           {
//            "[[media:", "]]"
//        }, {
//            "[[:media:", "]]"
//        }, {
//            "[[Image:", "]]"
//        }, {
//            "[[:Image:", "]]"
//        }, {
//            "[[image:", "]]"
//        }, {
//            "[[?��???:", "]]"
//        }, {
//            "[[Category:", "]]"
//        }, {
//            "[[category:", "]]"
//        }, {
//            "[[:Category:", "]]"
//        }, {
//            "[[:category:", "]]"
//        }, {
//            "<!--", "-->"
//        }, {
//            "<table", "</table>"
//        }, {
//            "{{Taxobox_begin", "{{Taxobox_end}}"
//        }, {
//            "[[Special:Whatlinkshere", "]]"
//        }, {
//            "[[Special:Recentchangeslinked", "]]"
//        }, {
//            "[[Special:Contributions", "]]"
//        }, {
//            "[[Special:Recentchangeslinked", "]]"
//        }, {
//            "-{T", "}-"
//        }, {
//            "__NOCC", "__"
//        }, {
//            "__NOCONTENTCONVERT", "__"
//        }, {
//            "__NOTC", "__"
//        }, {
//            "__NOTITLECONVERT", "__"
//        }, {
//            "<gallery>", "</gallery>"
//        }
//    };

    public static final String[][] NON_NECESSARY_INNER_TAGS = {
        {
            "[[", "]]"
        }, {
            "{{", "}}"
        }, {
            "{|", "|}"
        }, {
            "<table", "</table>"
        }
    };

    public static final String[][] WIKI_WORD_REDIRECT_TAGS = {
        {
            "#REDIRECT [[", "]]"
        }, {
            "#redirect [[", "]]"
        }, {
            "#REDIRECT[[", "]]"
        }, {
            "#redirect[[", "]]"
        }, {
            "#REDIRECTION[[", "]]"
        }, {
            "#Redirect [[","]]"
        }
        /*
         * #REDiRECT [[Washington, Baltimore and Annapolis Electric Railway]]
            #redirecT [[guarana]]
            #REDirect [[Kara-Suu]]

         */
    };

    public static final String[] LANGUAGE_TAGS = {
        "aa",
        "ab",
        "af",
        "ak",
        "als",
        "am",
        "an",
        "ang",
        "ar",
        "arc",
        "as",
        "ast",
        "av",
        "ay",
        "az",
        "ba",
        "bar",
        "bat-smg",
        "be",
        "be-x-old",
        "bg",
        "bh",
        "bi",
        "bm",
        "bn",
        "bo",
        "bpy",
        "br",
        "bs",
        "bug",
        "bxr",
        "ca",
        "cbk-zam",
        "cdo",
        "ce",
        "ceb",
        "ch",
        "cho",
        "chr",
        "chy",
        "co",
        "cr",
        "cs",
        "csb",
        "cu",
        "cv",
        "cy",
        "da",
        "de",
        "diq",
        "dv",
        "dz",
        "ee",
        "el",
        "eml",
        "eo",
        "es",
        "et",
        "eu",
        "fa",
        "ff",
        "fi",
        "fiu-vro",
        "fj",
        "fo",
        "fr",
        "frp",
        "fur",
        "fy",
        "ga",
        "gd",
        "gl",
        "glk",
        "gn",
        "got",
        "gu",
        "gv",
        "ha",
        "hak",
        "haw",
        "he",
        "hi",
        "ho",
        "hr",
        "hsb",
        "ht",
        "hu",
        "hy",
        "hz",
        "ia",
        "id",
        "ie",
        "ig",
        "ii",
        "ik",
        "ilo",
        "io",
        "is",
        "it",
        "iu",
        "ja",
        "jbo",
        "jv",
        "ka",
        "kab",
        "kg",
        "ki",
        "kj",
        "kk",
        "kl",
        "km",
        "kn",
        "ko",
        "kr",
        "ks",
        "ksh",
        "ku",
        "kv",
        "kw",
        "ky",
        "la",
        "lad",
        "lb",
        "lbe",
        "lg",
        "li",
        "lij",
        "lmo",
        "ln",
        "lo",
        "lt",
        "lv",
        "map-bms",
        "mg",
        "mh",
        "mi",
        "mk",
        "ml",
        "mn",
        "mo",
        "mr",
        "ms",
        "mt",
        "mus",
        "my",
        "mzn",
        "na",
        "nah",
        "nap",
        "nd",
        "nds",
        "nds-nl",
        "ne",
        "new",
        "ng",
        "nl",
        "nn",
        "no",
        "nov",
        "nrm",
        "nv",
        "ny",
        "oc",
        "om",
        "or",
        "os",
        "pa",
        "pag",
        "pam",
        "pap",
        "pdc",
        "pi",
        "pih",
        "pl",
        "pms",
        "ps",
        "pt",
        "qu",
        "rm",
        "rmy",
        "rn",
        "ro",
        "roa-rup",
        "roa-tara",
        "ru",
        "ru-sib",
        "rw",
        "sa",
        "sc",
        "scn",
        "sco",
        "sd",
        "se",
        "sg",
        "sh",
        "si",
        "simple",
        "sk",
        "sl",
        "sm",
        "sn",
        "so",
        "sq",
        "sr",
        "ss",
        "st",
        "su",
        "sv",
        "sw",
        "ta",
        "te",
        "tet",
        "tg",
        "th",
        "ti",
        "tk",
        "tl",
        "tlh",
        "tn",
        "to",
        "tokipona",
        "tpi",
        "tr",
        "ts",
        "tt",
        "tum",
        "tw",
        "ty",
        "udm",
        "ug",
        "uk",
        "ur",
        "uz",
        "ve",
        "vec",
        "vi",
        "vls",
        "vo",
        "wa",
        "war",
        "wo",
        "wuu",
        "xal",
        "xh",
        "yi",
        "yo",
        "za",
        "zea",
        "zh",
        "zh-classical",
        "zh-min-nan",
        "zh-yue",
        "zu"
    };

    /**
     * Lucene Stop Word
     * ?��來�?�濾??�鍵�?
     * @param
     * @return
     */
    public static final String[] EnglishStopWords = { "a", "about", "above",
            "across", "after", "afterwards", "again", "against", "all",
            "almost", "alone", "along", "already", "also", "although",
            "always", "am", "among", "amongst", "amoungst", "amount", "an",
            "and", "another", "any", "anyhow", "anyone", "anything", "anyway",
            "anywhere", "are", "around", "as", "at", "back", "be", "became",
            "because", "become", "becomes", "becoming", "been", "before",
            "beforehand", "behind", "being", "below", "beside", "besides",
            "between", "beyond", "bill", "both", "bottom", "but", "by", "call",
            "can", "cannot", "cant", "co", "computer", "con", "could",
            "couldnt", "cry", "de", "describe", "detail", "do", "done", "down",
            "due", "during", "each", "eg", "eight", "either", "eleven", "else",
            "elsewhere", "empty", "enough", "etc", "even", "ever", "every",
            "everyone", "everything", "everywhere", "except", "few", "fifteen",
            "fify", "fill", "find", "fire", "first", "five", "for", "former",
            "formerly", "forty", "found", "four", "from", "front", "full",
            "further", "get", "give", "go", "had", "has", "hasnt", "have",
            "he", "hence", "her", "here", "hereafter", "hereby", "herein",
            "hereupon", "hers", "herself", "him", "himself", "his", "how",
            "however", "hundred", "i", "ie", "if", "in", "inc", "indeed",
            "interest", "into", "is", "it", "its", "itself", "keep", "last",
            "latter", "latterly", "least", "less", "ltd", "made", "many",
            "may", "me", "meanwhile", "might", "mill", "mine", "more",
            "moreover", "most", "mostly", "move", "much", "must", "my",
            "myself", "name", "namely", "neither", "never", "nevertheless",
            "next", "nine", "no", "nobody", "none", "noone", "nor", "not",
            "nothing", "now", "nowhere", "of", "off", "often", "on", "once",
            "one", "only", "onto", "or", "other", "others", "otherwise", "our",
            "ours", "ourselves", "out", "over", "own", "part", "per",
            "perhaps", "please", "put", "rather", "re", "same", "say", "says",
            "said", "see", "seem", "seemed", "seeming", "seems", "serious",
            "several", "she", "should", "show", "side", "since", "sincere",
            "six", "sixty", "so", "some", "somehow", "someone", "something",
            "sometime", "sometimes", "somewhere", "still", "such", "system",
            "take", "ten", "than", "that", "the", "their", "them",
            "themselves", "then", "thence", "there", "thereafter", "thereby",
            "therefore", "therein", "thereupon", "these", "they", "thick",
            "thin", "third", "this", "those", "though", "three", "through",
            "throughout", "thru", "thus", "to", "together", "too", "top",
            "toward", "towards", "twelve", "twenty", "two", "un", "under",
            "until", "up", "upon", "us", "very", "via", "was", "we", "well",
            "were", "what", "whatever", "when", "whence", "whenever", "where",
            "whereafter", "whereas", "whereby", "wherein", "whereupon",
            "wherever", "whether", "which", "while", "whither", "who",
            "whoever", "whole", "whom", "whose", "why", "will", "with",
            "within", "without", "would", "yet", "you", "your", "yours",
            "yourself", "yourselves",
    };
}
