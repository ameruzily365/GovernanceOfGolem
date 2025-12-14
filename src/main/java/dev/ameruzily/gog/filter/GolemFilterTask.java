package dev.ameruzily.gog.filter;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import dev.ameruzily.gog.CopperGolemFilterPlugin;
import dev.ameruzily.gog.util.ItemIdUtil;

import java.util.*;

public final class GolemFilterTask {

    private final CopperGolemFilterPlugin plugin;

    private final NamespacedKey KEY_ENABLED;
    private final NamespacedKey KEY_ALLOW;

    private int taskId = -1;

    private final Map<UUID, Long> cooldownUntilMs = new HashMap<>();

    private final Material copperChestMaterial;

    public GolemFilterTask(CopperGolemFilterPlugin plugin) {
        this.plugin = plugin;
        this.KEY_ENABLED = new NamespacedKey(plugin, "filter_enabled");
        this.KEY_ALLOW = new NamespacedKey(plugin, "filter_allow");

        // 兼容：如果未来 Material 名字变了，至少不会炸
        this.copperChestMaterial = Material.matchMaterial("COPPER_CHEST");
        if (this.copperChestMaterial == null) {
            plugin.getLogger().warning("Material COPPER_CHEST not found! Return-to-chest will be disabled.");
        }
    }

    public void start() {
        // 延迟 10 tick 启动，每 5 tick 扫一次
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 10L, 5L);
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        cooldownUntilMs.clear();
    }

    private void tick() {
        long now = System.currentTimeMillis();

        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e.getType() != EntityType.COPPER_GOLEM) continue;

                var pdc = e.getPersistentDataContainer();
                Byte enabled = pdc.get(KEY_ENABLED, PersistentDataType.BYTE);
                if (enabled == null || enabled != (byte) 1) continue;

                long cd = cooldownUntilMs.getOrDefault(e.getUniqueId(), 0L);
                if (now < cd) continue;

                if (!(e instanceof LivingEntity golem)) continue;

                ItemStack held = golem.getEquipment() != null ? golem.getEquipment().getItem(EquipmentSlot.HAND) : null;
                if (held == null || held.getType().isAir()) continue;

                String raw = pdc.get(KEY_ALLOW, PersistentDataType.STRING);
                Set<String> allow = parseAllow(raw);
                if (allow.isEmpty()) continue; // 空：不限制

                String heldId = ItemIdUtil.normalize(held);
                if (heldId == null || !allow.contains(heldId)) {
                    // 不允许：退回铜箱 / 掉落
                    boolean returned = tryReturnToNearbyCopperChest(golem.getLocation(), held);

                    // 从手上拿走
                    golem.getEquipment().setItem(EquipmentSlot.HAND, null);

                    if (!returned) {
                        golem.getWorld().dropItemNaturally(golem.getLocation(), held);
                    }

                    // 1秒冷却，防抽搐
                    cooldownUntilMs.put(e.getUniqueId(), now + 1000);
                }
            }
        }
    }

    private Set<String> parseAllow(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        String[] parts = raw.split(";");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private boolean tryReturnToNearbyCopperChest(Location loc, ItemStack stack) {
        if (copperChestMaterial == null) return false;

        World w = loc.getWorld();
        if (w == null) return false;

        int r = 6; // 搜索半径，可配置
        Block best = null;
        double bestDist = Double.MAX_VALUE;

        int cx = loc.getBlockX(), cy = loc.getBlockY(), cz = loc.getBlockZ();
        for (int x = cx - r; x <= cx + r; x++) {
            for (int y = cy - r; y <= cy + r; y++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() != copperChestMaterial) continue;

                    double d = b.getLocation().distanceSquared(loc);
                    if (d < bestDist) {
                        bestDist = d;
                        best = b;
                    }
                }
            }
        }

        if (best == null) return false;
        if (!(best.getState() instanceof Container c)) return false;

        Inventory inv = c.getInventory();
        Map<Integer, ItemStack> left = inv.addItem(stack);
        return left.isEmpty();
    }
}
