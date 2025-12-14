package dev.ameruzily.gog.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public final class ItemIdUtil {

    private ItemIdUtil() {}

    public static String normalize(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        // ItemsAdder (soft)
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                // 需要 ItemsAdder API jar 才能编译
                dev.lone.itemsadder.api.CustomStack cs = dev.lone.itemsadder.api.CustomStack.byItemStack(item);
                if (cs != null) return "ia:" + cs.getNamespacedID(); // e.g. ia:myitems:steel_ingot
            } catch (Throwable ignored) {
                // 没有 API / 版本差异 / 运行时异常 -> 回退到 mc
            }
        }

        return "mc:" + item.getType().name();
    }
}
