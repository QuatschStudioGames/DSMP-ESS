package de.qstudios.dsmp;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class WelcomePlugin extends JavaPlugin implements Listener {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private String serverName;
    private String serverColor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        
        String dcLink = getConfig().getString("discord-link", "https://discord.gg/invite/myServer");
        serverName = getConfig().getString("server-name", "DSMP-ESS-USER");
        serverColor = getConfig().getString("server-color", "gradient:#00ff88:#00ccff");

        getCommand("tps").setExecutor(new TestCommand());
        getCommand("ping").setExecutor(new PingCommand());
        getCommand("dc").setExecutor(new DcCommand(dcLink));
        getCommand("discord").setExecutor(new DcCommand(dcLink));
        getCommand("about").setExecutor(new AboutCommand(this));

        getLogger().info("DSMP-ESS loaded!");

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                this,
                task -> updateHeader(),
                20L,
                20L
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        checkExtensions(player);

        String rawOnline = PlaceholderAPI.setPlaceholders(player, "%server_online%");
        String rawVersion = PlaceholderAPI.setPlaceholders(player, "%viaversion_player_protocol_version%");
        
        boolean hasOnline = !rawOnline.equals("%server_online%");
        boolean hasVersion = !rawVersion.equals("%viaversion_player_protocol_version%");

        String onlineStr = hasOnline ? "Online: " + rawOnline + "," : "";
        String versionStr = hasVersion ? "Version: " + rawVersion : "";
        String suffix = (onlineStr.isEmpty() && versionStr.isEmpty()) ? "" : " !";

        String welcomeRaw = "Welcome, " + player.getName() + "! " + onlineStr + " " + versionStr + suffix;
        player.sendMessage(mm.deserialize(welcomeRaw));

        updatePlayerHeader(player);

        player.getScheduler().runDelayed(this, task -> {
            String act = PlaceholderAPI.setPlaceholders(player, "Activity: %plan_player_activity_group:" + player.getName() + "%");
            player.sendActionBar(mm.deserialize(act));
        }, null, 40L);
    }

    private void checkExtensions(Player p) {
        if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null && isMissing(p, "%viaversion_player_protocol_version%")) {
            getLogger().warning("PAPI Extension für ViaVersion fehlt!");
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null && isMissing(p, "%luckperms_groups%")) {
            getLogger().warning("PAPI Extension für LuckPerms fehlt!");
        }
    }

    private boolean isMissing(Player p, String placeholder) {
        return PlaceholderAPI.setPlaceholders(p, placeholder).equals(placeholder);
    }

    public void updatePlayerHeader(Player player) {
        String group = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
        String colorName = PlaceholderAPI.setPlaceholders(player, "%luckperms_meta_color%");
        if (colorName.isEmpty() || colorName.contains("%")) colorName = "white";

        Component listName = mm.deserialize("<" + colorName + ">[" + group.toUpperCase() + "] " + player.getName() + "</" + colorName + ">");
        player.playerListName(listName);

        @SuppressWarnings("deprecation")
        String tps = ChatColor.stripColor(PlaceholderAPI.setPlaceholders(player, "%spark_tps_5s%"));

        player.sendPlayerListHeaderAndFooter(
                mm.deserialize("<" + serverColor + "><bold>" + serverName + "</bold></gradient>\n<gray>Welcome, " + player.getName() + "!</gray>"),
                mm.deserialize("<yellow>TPS: " + tps + "</yellow>")
        );
    }

    private void updateHeader() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerHeader(player);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String group = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
        String color = PlaceholderAPI.setPlaceholders(player, "%luckperms_meta_color%");
        if (color.isEmpty() || color.contains("%")) color = "white";

        String prefixStr = "<" + color + ">[" + group.toUpperCase() + "] </" + color + ">";
        Component prefix = mm.deserialize(prefixStr);
        
        event.renderer((source, sourceDisplayName, message, viewer) -> 
            prefix.append(Component.text(player.getName() + ": ")).append(message));
    }

    public class TestCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            String tps = PlaceholderAPI.setPlaceholders(null, "%spark_tps_5s%, %spark_tps_1m%");
            s.sendMessage("§aTPS: " + tps);
            return true;
        }
    }

    public class PingCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            if (s instanceof Player p) s.sendMessage("§aPing: §e" + p.getPing() + "ms");
            return true;
        }
    }

    public class AboutCommand implements CommandExecutor {
        private final WelcomePlugin plugin;
        public AboutCommand(WelcomePlugin plugin) { this.plugin = plugin; }

        @SuppressWarnings("deprecation")
        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            s.sendMessage("§7Version: §b" + plugin.getDescription().getVersion());
            s.sendMessage("§7Powered by §dQuatschStudio Games");
            return true;
        }
    }

    public class DcCommand implements CommandExecutor {
        private final String link;
        public DcCommand(String link) { this.link = link; }

        @Override
        public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
            s.sendMessage(mm.deserialize("<click:open_url:'" + link + "'><hover:show_text:'" + link + "'>[Discord]</hover></click>"));
            return true;
        }
    }
}