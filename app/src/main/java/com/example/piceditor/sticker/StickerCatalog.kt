package com.example.piceditor.sticker

import android.content.Context

object StickerCatalog {

    private const val FOLDER = "images"

    // Số sticker FREE đầu mỗi category (mirror rule template: ~5 đầu free, còn lại premium).
    private const val FREE_PER_CATEGORY = 5

    // Bỏ qua các pack không phải sticker (thumbnail, tạm thời)
    private val EXCLUDED_PREFIXES = setOf("thumb", "temp")

    // Thứ tự ưu tiên hiển thị (pack nào không có ở đây sẽ xếp sau theo alphabet)
    private val PREFERRED_ORDER = listOf(
        "emoji", "sticker", "icon", "cute", "item",
        "star", "flower", "pink", "blue", "green", "brown", "sea"
    )

    private val ITEM_REGEX = Regex("^([a-zA-Z]+)(\\d+)\\.(webp|png|jpg)$", RegexOption.IGNORE_CASE)

    fun build(context: Context): List<StickerCategory> {
        val names: Array<String> = try {
            context.assets.list(FOLDER) ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        // group theo prefix chữ cái (vd "sticker", "icon"…)
        val groups = mutableMapOf<String, MutableList<Pair<Int, String>>>()
        for (name in names) {
            val m = ITEM_REGEX.matchEntire(name) ?: continue
            val prefix = m.groupValues[1].lowercase()
            if (EXCLUDED_PREFIXES.contains(prefix)) continue
            val index = m.groupValues[2].toIntOrNull() ?: continue
            groups.getOrPut(prefix) { mutableListOf() } += index to name
        }

        val sortedPrefixes = groups.keys.sortedWith(prefixComparator())
        return sortedPrefixes.map { prefix ->
            // Cách A (đồng bộ template): mỗi category để ~5 sticker đầu FREE, từ thứ 6 trở đi PREMIUM.
            val items = groups.getValue(prefix)
                .sortedBy { it.first }
                .mapIndexed { index, (_, fileName) ->
                    StickerItem(
                        assetPath = "file:///android_asset/$FOLDER/$fileName",
                        isPremium = index >= FREE_PER_CATEGORY
                    )
                }
            StickerCategory(id = prefix, preview = items.first(), items = items)
        }
    }

    private fun prefixComparator(): Comparator<String> = Comparator { a, b ->
        val ia = PREFERRED_ORDER.indexOf(a)
        val ib = PREFERRED_ORDER.indexOf(b)
        when {
            ia != -1 && ib != -1 -> ia - ib
            ia != -1 -> -1
            ib != -1 -> 1
            else -> a.compareTo(b)
        }
    }
}
