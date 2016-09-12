package eu.alfred.news.util;

import java.util.Map;

public class StringUtils {
    public static String getReadableString(Map<String, String> map) {
        String readableString = "{";
        if (map != null) {
            boolean first = true;
            for (String key : map.keySet()) {
                if (!first) {
                    readableString += ", " + key;
                } else {
                    first = false;
                    readableString += key;
                }
            }
        }
        readableString += "}";
        return readableString;
    }
}
