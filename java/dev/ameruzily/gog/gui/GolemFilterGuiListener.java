package dev.ameruzily.gog.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import dev.ameruzily.gog.CopperGolemFilterPlugin;
import dev.ameruzily.gog.util.ItemIdUtil;

import java.util.*;

public final class GolemFilterGuiListener implements Listener {

    private final CopperGolemFilterPlugin plugin;

    private final NamespacedKey KEY_ENABLED;
    private final NamespacedKey KEY_ALLOW;

    private static final String TITLE = "铜傀儡过滤器";

    // player -> golem
    private final Map<UUID, UUID> editing = new HashMap<>();

    public GolemFilterGuiListener(CopperGolemFilterPlugin plugin) {
        this.plugin = plugin;
        this.KEY_ENABLED = new NamespacedKey(plugin, "filter_enabled");
        this.KEY_ALLOW = new NamespacedKey(plugin, "filter_allow");
    }

    @EventHandler
    public void onRightClickGolem(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked().getType() != EntityType.COPPER_GOLEM) return;

        e.setCancelled(true);

        Player p = e.getPlayer();
        Entity golem = e.getRightClicked();

        Inventory gui = Bukkit.createInventory(p, 27, TITLE);

        gui.setItem(24, button(Material.LIME_WOOL, "保存"));
        gui.setItem(25, button(Material.RED_WOOL, "清空"));
        gui.setItem(26, button(Material.GRAY_WOOL, "关闭"));

        editing.put(p.getUniqueId(), golem.getUniqueId());
        p.openInventory(gui);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!TITLE.equals(e.getView().getTitle())) return;

        UUID golemId = editing.get(p.getUniqueId());
        if (golemId == null) return;

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        // 24/25/26 为按钮
        if (slot == 26) {
            e.setCancelled(true);
            p.closeInventory();
            return;
        }
        if (slot == 25) {
            e.setCancelled(true);
            for (int i = 0; i < 18; i++) e.getInventory().setItem(i, null);
            return;
        }
        if (slot == 24) {
            e.setCancelled(true);
            saveFilterFromGui(p, e.getInventory(), golemId);
            p.closeInventory();
            return;
        }

        // 样本区允许玩家自由放取（0~17）
        // 其余位置不允许操作
        if (slot >= 18) e.setCancelled(true);
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!TITLE.equals(e.getView().getTitle())) return;

        // 关闭时不自动保存，避免误操作
        editing.remove(p.getUniqueId());
    }

    private void saveFilterFromGui(Player p, Inventory inv, UUID golemId) {
        Entity golem = Bukkit.getEntity(golemId);
        if (golem == null || golem.getType() != EntityType.COPPER_GOLEM) {
            p.sendMessage("§c铜傀儡不见了。");
            return;
        }

        Set<String> allow = new HashSet<>();
        for (int i = 0; i < 18; i++) {
            ItemStack it = inv.getItem(i);
            String id = ItemIdUtil.normalize(it);
            if (id != null) allow.add(id);
        }

        var pdc = golem.getPersistentDataContainer();

        if (allow.isEmpty()) {
            // 空白名单 = 恢复原版（不过滤）
            pdc.remove(KEY_ENABLED);
            pdc.remove(KEY_ALLOW);
            p.sendMessage("§7已清除过滤器（恢复原版搬运）。");
            return;
        }

        pdc.set(KEY_ENABLED, PersistentDataType.BYTE, (byte) 1);
        pdc.set(KEY_ALLOW, PersistentDataType.STRING, String.join(";", allow));
        p.sendMessage("§a已保存过滤器：" + allow.size() + " 种物品");
    }

    private ItemStack button(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + name);
            it.setItemMeta(meta);
        }
        return it;
    }
}
