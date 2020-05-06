package tk.t11e.bungeesecurity.bukkit.main;
// Created by booky10 in BungeeSecurity (20:11 05.05.20)

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
public class Main extends JavaPlugin implements Listener, PluginMessageListener {

    private final List<UUID> verifiedPlayers = new ArrayList<>();
    private final List<UUID> securedPlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        saveConfig();
        if (!getConfig().contains("secret")) {
            getConfig().set("secret", UUID.randomUUID().toString());
            saveConfig();
            reloadConfig();
        }

        Bukkit.getMessenger().registerIncomingPluginChannel(this, "bungee:security",
                this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (!verifiedPlayers.contains(player.getUniqueId())) {
            securedPlayers.add(player.getUniqueId());
            boolean hasBlindness = false;
            for (PotionEffect potionEffect : player.getActivePotionEffects())
                if (potionEffect.getType().equals(PotionEffectType.BLINDNESS)) {
                    hasBlindness = true;
                    break;
                }
            if (!hasBlindness)
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40,
                        255, true, false));

            player.sendMessage("§7[§bVerification§7]§c Your connection ist being verified...");

            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!player.isOnline()) return;
                if (!securedPlayers.contains(player.getUniqueId())) return;

                player.kickPlayer("§4Verification unsuccessful!");
                getLogger().warning(player.getUniqueId() + " (" + player.getName() + ")" +
                        " tried to connect without verification!");
                getLogger().warning(event.getHostname() + " " + event.getRealAddress().getHostAddress());
            }, 40);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (securedPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (securedPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (securedPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (securedPlayers.contains(event.getWhoClicked().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (securedPlayers.contains(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        securedPlayers.remove(event.getPlayer().getUniqueId());
        verifiedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("bungee:security")) return;
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);
        if (!dataInput.readUTF().equals("verify")) return;

        UUID verified = UUID.fromString(dataInput.readUTF());
        String secret = dataInput.readUTF();

        if (!secret.equals(getSecret()))
            getLogger().warning("WARNING: Plugin Message with incorrect secret received!");
        else if (securedPlayers.contains(verified)) {
            securedPlayers.remove(verified);
            Objects.requireNonNull(Bukkit.getPlayer(verified))
                    .sendMessage("§7[§bVerification§7]§a Verification successful!");
        } else if (!verifiedPlayers.contains(verified))
            verifiedPlayers.add(verified);
    }

    private String getSecret() {
        reloadConfig();
        return getConfig().getString("secret");
    }
}