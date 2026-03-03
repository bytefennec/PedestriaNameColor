package com.pedestriamc.namecolor.manager;

import com.pedestriamc.namecolor.NameColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BlacklistManager {

    private final boolean normalize;
    private final boolean fuzzy;
    private final double fuzzyPercentage;

    private final List<String> blacklist;
    private final List<Pattern> blacklistPatterns;

    public BlacklistManager(@NotNull NameColor nameColor) {
        FileConfiguration config = nameColor.files().getBlacklistConfig();

        normalize = config.getBoolean("normalize");
        fuzzy = config.getBoolean("fuzzy", true);
        fuzzyPercentage = config.getDouble("max-percentage", 0.2);

        blacklist = loadBlacklist(config);
        blacklistPatterns = loadBlacklistPatterns(config);
    }

    public boolean isBlacklisted(@NotNull String name) {
        if (normalize) {
            name = normalize(name);
        }

        if (blacklist.contains(name)) {
            return true;
        }

        for (Pattern pattern : blacklistPatterns) {
            if (pattern.matcher(name).matches()) {
                return true;
            }
        }

        if (fuzzy) {
            return fuzzyCheck(name);
        } else {
            return false;
        }
    }

    private @NotNull List<String> loadBlacklist(@NotNull FileConfiguration config) {
        List<String> list = new ArrayList<>();
        for (String str : config.getStringList("blacklist")) {
            if (normalize) {
                list.add(normalize(str));
            } else {
                list.add(str);
            }
        }

        return list;
    }

    private @NotNull List<Pattern> loadBlacklistPatterns(@NotNull FileConfiguration config) {
        List<Pattern> patterns = new ArrayList<>();

        List<String> strings = config.getStringList("blacklist");
        for (String string : strings) {
            patterns.add(Pattern.compile(string, Pattern.CASE_INSENSITIVE));
        }

        return patterns;
    }

    // Returns true if too close
    // Does not normalize, expected to already be done.
    private boolean fuzzyCheck(@NotNull String name) {
        int length = name.length();
        for (String string : blacklist) {
            int distance = StringUtils.getLevenshteinDistance(name, string);

            int maxLen = Math.max(length, string.length());
            int threshold = Math.max(1, (int)(fuzzyPercentage * maxLen));

            if (distance <= threshold) {
                return true;
            }
        }

        return false;
    }

    private @NotNull String normalize(@NotNull String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
