package searchengine.parsers;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Slf4j
public class Morphology {
    private static LuceneMorphology luceneMorphology;
    private final static String REGEX = "\\p{Punct}|[0-9]|№|©|◄|«|»|—|-|@|…";
    private final static Marker INVALID_SYMBOL_MARKER = MarkerManager.getMarker("INVALID_SYMBOL");
    private final static Logger LOGGER = LogManager.getLogger(LuceneMorphology.class);

    static {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static HashMap<String, Integer> getLemmaListWithCount(String content) {
        content = content.toLowerCase(Locale.ROOT)
                .replaceAll(REGEX, " ");
        HashMap<String, Integer> lemmaListWithCount = new HashMap<>();
        String[] words = content.toLowerCase(Locale.ROOT).split("\\s+");
        for (String word : words) {
            List<String> lemmaList = getLemmaList(word);
            for (String lemma : lemmaList) {
                int count = lemmaListWithCount.getOrDefault(lemma, 0);
                lemmaListWithCount.put(lemma, count + 1);
            }
        }
        return lemmaListWithCount;
    }

    public static List<String> getLemmaList(String word) {
        List<String> lemmaList = new ArrayList<>();
        try {
            List<String> baseRusForm = luceneMorphology.getNormalForms(word);
            if (!isServiceWord(word)) {
                lemmaList.addAll(baseRusForm);
            }
        } catch (Exception e) {
            LOGGER.debug(INVALID_SYMBOL_MARKER, "Символ не найден - " + word);
        }
        return lemmaList;
    }

    public static List<Integer> getLemmaIndexList(String content, String lemmaFromSearchText) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] words = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        int index = 0;
        for (String word : words) {
            List<String> lemmaList = getLemmaList(word);
            for (String lemmaFromContent : lemmaList) {
                if (lemmaFromContent.equals(lemmaFromSearchText)) {
                    lemmaIndexList.add(index);
                }
            }
            index += word.length() + 1;
        }
        return lemmaIndexList;
    }

    private static boolean isServiceWord(String word) {
        List<String> morphInfoList = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : morphInfoList) {
            if (morphInfo.contains("ПРЕДЛ")
                    || morphInfo.contains("СОЮЗ")
                    || morphInfo.contains("МЕЖД")
                    || morphInfo.contains("МС")
                    || morphInfo.contains("ЧАСТ")
                    || morphInfo.length() <= 3) {
                return true;
            }
        }
        return false;
    }
}
