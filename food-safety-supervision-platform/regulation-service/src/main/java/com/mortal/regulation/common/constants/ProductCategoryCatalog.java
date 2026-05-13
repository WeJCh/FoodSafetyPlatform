package com.mortal.regulation.common.constants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class ProductCategoryCatalog {

    public static final String CATERING_PREPARED = "餐饮自制食品";
    public static final String PREPACKAGED_FOOD = "预包装食品";
    public static final String BULK_FOOD = "散装食品";
    public static final String FRESH_AGRICULTURAL = "生鲜农产品";
    public static final String DAIRY = "乳制品";
    public static final String MEAT_AND_AQUATIC = "肉及水产制品";
    public static final String GRAIN_AND_CONDIMENT = "粮油调味品";
    public static final String OTHER = "其他食品";

    public static final List<String> STANDARD_CATEGORIES = List.of(
        CATERING_PREPARED,
        PREPACKAGED_FOOD,
        BULK_FOOD,
        FRESH_AGRICULTURAL,
        DAIRY,
        MEAT_AND_AQUATIC,
        GRAIN_AND_CONDIMENT,
        OTHER
    );

    private static final Set<String> STANDARD_CATEGORY_SET = Set.copyOf(STANDARD_CATEGORIES);
    private static final Map<String, String> LEGACY_ALIASES = createLegacyAliases();

    private ProductCategoryCatalog() {
    }

    public static boolean isSupported(String value) {
        return normalize(value) != null;
    }

    public static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (STANDARD_CATEGORY_SET.contains(normalized)) {
            return normalized;
        }
        return LEGACY_ALIASES.get(normalized);
    }

    private static Map<String, String> createLegacyAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("餐饮服务", CATERING_PREPARED);
        aliases.put("快餐", CATERING_PREPARED);
        aliases.put("预包装食品", PREPACKAGED_FOOD);
        aliases.put("散装食品", BULK_FOOD);
        aliases.put("水果", FRESH_AGRICULTURAL);
        aliases.put("蔬菜", FRESH_AGRICULTURAL);
        aliases.put("乳制品", DAIRY);
        aliases.put("肉制品", MEAT_AND_AQUATIC);
        aliases.put("水产制品", MEAT_AND_AQUATIC);
        aliases.put("烘焙食品", PREPACKAGED_FOOD);
        aliases.put("冷冻食品", PREPACKAGED_FOOD);
        aliases.put("饮料", PREPACKAGED_FOOD);
        aliases.put("酒类", PREPACKAGED_FOOD);
        aliases.put("调味品", GRAIN_AND_CONDIMENT);
        aliases.put("粮油及谷物制品", GRAIN_AND_CONDIMENT);
        aliases.put("谷物制品", GRAIN_AND_CONDIMENT);
        aliases.put("其他食品", OTHER);
        return Map.copyOf(aliases);
    }
}
