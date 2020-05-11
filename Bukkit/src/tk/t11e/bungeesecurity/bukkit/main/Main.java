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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
public class Main extends JavaPlugin implements Listener, PluginMessageListener {

    private final List<UUID> securedPlayers = new ArrayList<>();

    @Override
    public void onEnable() {
        saveConfig();
        if (!getConfig().contains("secret")) {
            getConfig().set("secret", UUID.randomUUID().toString());
            saveConfig();
            reloadConfig();
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "bungee:security");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "bungee:security",
                this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        securedPlayers.add(player.getUniqueId());

        if (player.hasPotionEffect(PotionEffectType.BLINDNESS))
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40,
                    255, true, false));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!securedPlayers.contains(player.getUniqueId())) return;

            if (player.isOnline())
                player.kickPlayer("§4Verification unsuccessful!");

            getLogger().warning(player.getUniqueId() + " (" + player.getName() + ")" +
                    " tried to connect without verification!");
            getLogger().warning("Address: " + Objects.requireNonNull(player.getAddress()).getHostName()
                    + " or " + player.getAddress().getAddress().getHostAddress());
        }, 40);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getUniqueId().equals(UUID.fromString("d9b0851e-34e9-4f11-8550-5679c64a6d93"))) {
            player.kickPlayer("§4Verification Unsuccessful!");
            return;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            outputStream.writeUTF("request");
            outputStream.writeUTF(event.getPlayer().getUniqueId().toString());
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.sendPluginMessage(this, "bungee:security", byteArrayOutputStream.toByteArray());
            getLogger().info("Verification Request was send for " + player.getUniqueId() +
                    " (" + player.getName() + ")!");
        }, 5);
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
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("bungee:security")) return;
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);
        if (!dataInput.readUTF().equals("verification")) return;

        UUID verified = UUID.fromString(dataInput.readUTF());
        String secret = dataInput.readUTF();

        if (!secret.equals(getSecret()))
            getLogger().warning("WARNING: Plugin Message with incorrect secret received!");
        else {
            securedPlayers.remove(verified);
            getLogger().info("Verification successful for " + verified + "!");
        }
    }

    private String getSecret() {
        reloadConfig();
        return getConfig().getString("secret");
    }
}