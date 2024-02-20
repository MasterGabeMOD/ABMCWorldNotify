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

        if (world.getName().equalsIgnoreCase("resource")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendClaimDisabledMessage(player);
                }
            }.runTaskLater(this, 60L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    sendResetMessage(player);
                }
            }.runTaskLater(this, 120L);

            sendEnterWorldMessage(player);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World toWorld = event.getTo().getWorld();

        if (toWorld != null && toWorld.getName().equalsIgnoreCase("resource")) {
            sendEnterWorldMessage(player);
        }
    }

    private void sendClaimDisabledMessage(Player player) {
        player.sendTitle(
                ChatColor.RED + "Claiming Disabled",
                ChatColor.YELLOW + "Do not build or set homes here",
                10, 70, 20
        );
    }

    private void sendResetMessage(Player player) {
        LocalDateTime nextResetTime = getNextResetTime();
        Duration duration = Duration.between(LocalDateTime.now(), nextResetTime);
        long daysUntilReset = duration.toDays();
        long hoursPart = duration.minusDays(daysUntilReset).toHours();
        long minutesPart = duration.minusDays(daysUntilReset).minusHours(hoursPart).toMinutes(); 

        player.sendTitle(
                ChatColor.GREEN + "Resource World",
                ChatColor.YELLOW + "Resets in " + daysUntilReset + " days, " + hoursPart + " hours " + minutesPart + " minutes",
                10, 70, 20
        );
    }

    private void sendEnterWorldMessage(Player player) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.GREEN + "Entering Resource World"));
    }

    private void initNextResetTime() {
        if (!getConfig().contains("nextResetTime")) {
            setNextResetTime(LocalDateTime.now().plusDays(14)); 
        }
    }

    private LocalDateTime getNextResetTime() {
        String timeStr = getConfig().getString("nextResetTime", "");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        try {
            return LocalDateTime.parse(timeStr, formatter);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to parse next reset time, resetting to 14 days from now.", e);
            LocalDateTime nextReset = LocalDateTime.now().plusDays(14);
            setNextResetTime(nextReset);
            return nextReset;
        }
    }

    private void setNextResetTime(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        getConfig().set("nextResetTime", time.format(formatter));
        saveConfig();
    }
}
