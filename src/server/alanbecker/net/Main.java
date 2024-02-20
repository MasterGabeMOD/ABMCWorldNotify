package server.alanbecker.net;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ABMCWorldNotify has been enabled!");
        saveDefaultConfig();
        initNextResetTime();
    }

    @Override
    public void onDisable() {
        getLogger().info("ABMCWorldNotify has been disabled!");
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();

        if (worldName.equalsIgnoreCase("resource") || worldName.equalsIgnoreCase("world_nether") || worldName.equalsIgnoreCase("world_the_end")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendClaimDisabledMessage(player);
                }
            }.runTaskLater(this, 60L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    sendResetMessage(player, worldName);
                }
            }.runTaskLater(this, 120L);

            sendEnterWorldMessage(player, worldName);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World toWorld = event.getTo().getWorld();

        if (toWorld != null && (toWorld.getName().equalsIgnoreCase("resource") || toWorld.getName().equalsIgnoreCase("world_nether") || toWorld.getName().equalsIgnoreCase("world_the_end"))) {
            sendEnterWorldMessage(player, toWorld.getName());
        }
    }

    private void sendClaimDisabledMessage(Player player) {
        player.sendTitle(
                ChatColor.RED + "Claiming Disabled",
                ChatColor.YELLOW + "Do not build or set homes here",
                10, 70, 20
        );
    }

    private void sendResetMessage(Player player, String worldName) {
        LocalDateTime nextResetTime = getNextResetTime(worldName);
        Duration duration = Duration.between(LocalDateTime.now(), nextResetTime);
        long daysUntilReset = duration.toDays();
        long hoursPart = duration.minusDays(daysUntilReset).toHours();
        long minutesPart = duration.minusDays(daysUntilReset).minusHours(hoursPart).toMinutes();

        String worldDisplayName = getWorldDisplayName(worldName);
        player.sendTitle(
                ChatColor.GREEN + worldDisplayName,
                ChatColor.YELLOW + "Resets in " + daysUntilReset + " days, " + hoursPart + " hours, " + minutesPart + " minutes",
                10, 70, 20
        );
    }

    private void sendEnterWorldMessage(Player player, String worldName) {
        String worldDisplayName = getWorldDisplayName(worldName);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "Entering " + worldDisplayName));
    }

    private void initNextResetTime() {
        initWorldResetTime("resource", 14);
        initWorldResetTime("world_nether", 30);
        initWorldResetTime("world_the_end", 30);
    }

    private void initWorldResetTime(String worldName, long daysUntilReset) {
        String path = worldName + ".nextResetTime";
        if (!getConfig().contains(path)) {
            setNextResetTime(worldName, LocalDateTime.now().plusDays(daysUntilReset));
        }
    }

    private LocalDateTime getNextResetTime(String worldName) {
        String path = worldName + ".nextResetTime";
        String timeStr = getConfig().getString(path, "");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        try {
            LocalDateTime nextResetTime = LocalDateTime.parse(timeStr, formatter);
            if (LocalDateTime.now().isAfter(nextResetTime)) {
                long daysToAdd = "resource".equals(worldName) ? 14 : 30;
                LocalDateTime newResetTime = LocalDateTime.now().plusDays(daysToAdd);
                setNextResetTime(worldName, newResetTime);
                return newResetTime;
            }
            return nextResetTime;
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to parse next reset time for " + worldName + ", resetting.", e);
            long daysToAdd = "resource".equals(worldName) ? 14 : 30; 
            LocalDateTime newResetTime = LocalDateTime.now().plusDays(daysToAdd);
            setNextResetTime(worldName, newResetTime);
            return newResetTime;
        }
    }

    private void setNextResetTime(String worldName, LocalDateTime time) {
        String path = worldName + ".nextResetTime";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        getConfig().set(path, time.format(formatter));
        saveConfig();
    }

    private String getWorldDisplayName(String worldName) {
        switch (worldName.toLowerCase()) {
            case "resource":
                return "Resource World";
            case "world_nether":
                return "Nether World";
            case "world_the_end":
                return "End World";
            default:
                return "World";
        }
    }
}
