package dev.ameruzily.gog;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import dev.ameruzily.gog.filter.GolemFilterTask;
import dev.ameruzily.gog.gui.GolemFilterGuiListener;

public final class CopperGolemFilterPlugin extends JavaPlugin {

    private GolemFilterTask filterTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // GUI + 交互监听
        Bukkit.getPluginManager().registerEvents(new GolemFilterGuiListener(this), this);

        // 过滤纠错任务
        this.filterTask = new GolemFilterTask(this);
        this.filterTask.start();

        getLogger().info("CopperGolemFilter enabled.");
    }

    @Override
    public void onDisable() {
        if (filterTask != null) filterTask.stop();
        getLogger().info("CopperGolemFilter disabled.");
    }
}
