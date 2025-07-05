package org.oplimit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OPLimit extends JavaPlugin implements Listener {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日 HH:mm:ss");
    private File logFile;
    private FileWriter fileWriter;
    private BufferedWriter bufferedWriter;

    @Override
    public void onEnable() {
        // 创建插件数据文件夹
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("❌ 无法创建插件目录");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 初始化记录文件
        logFile = new File(getDataFolder(), "op-commands.log");
        try {
            if (!logFile.exists() && !logFile.createNewFile()) {
                getLogger().severe("❌ 无法创建记录文件");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 创建文件写入器
            fileWriter = new FileWriter(logFile, true);
            bufferedWriter = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            getLogger().severe("❌ 文件初始化错误: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 注册事件
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("✅ OPLimit 插件已启用");
    }

    @Override
    public void onDisable() {
        // 关闭线程池
        executor.shutdown();

        // 关闭文件写入器
        try {
            if (bufferedWriter != null) bufferedWriter.close();
            if (fileWriter != null) fileWriter.close();
        } catch (IOException e) {
            getLogger().warning("⚠️ 关闭文件写入器时出错: " + e.getMessage());
        }

        getLogger().info("🛑 OPLimit 插件已禁用");
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // 只记录OP玩家的命令
        if (player.isOp()) {
            // 格式化日志条目
            String logEntry = String.format("[%s] - %s - %s",
                    dateFormat.format(new Date()),
                    player.getName(),
                    event.getMessage().trim());

            // 异步写入日志
            executor.execute(() -> {
                try {
                    bufferedWriter.write(logEntry);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } catch (IOException e) {
                    getLogger().warning("📝 写入记录失败: " + e.getMessage());
                }
            });
        }
    }
}