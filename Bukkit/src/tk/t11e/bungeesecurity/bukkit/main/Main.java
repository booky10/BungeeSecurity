package tk.t11e.bungeesecurity.bukkit.main;
// Created by booky10 in BungeeSecurity (20:11 05.05.20)

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import tk.t11e.bungeesecurity.AES;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
public final class Main extends JavaPlugin implements Listener, PluginMessageListener {

    private final List<UUID> securedPlayers = new ArrayList<>();
    private final HashMap<UUID, Integer> kickTasks = new HashMap<>();

    @Override
    public final void onEnable() {
        if (!getConfig().contains("secret")) getConfig().set("secret", UUID.randomUUID().toString());
        if (!getConfig().contains("key")) getConfig().set("key", UUID.randomUUID().toString());
        if (!getConfig().contains("address.name")) getConfig().set("address.name", "0.0.0.0");
        if (!getConfig().contains("address.enabled")) getConfig().set("address.enabled", false);
        saveConfig();
        reloadConfig();
        register();
    }

    private void register() {
        //Plugin Messages
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "bungee:security");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "bungee:security", this);

        //Listener
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public final void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (getConfig().getBoolean("address.enabled"))
            if (!isAddressValid(event.getRealAddress().toString())) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "§4Verification unsuccessful!");
                getLogger().warning(player.getUniqueId() + " (" + player.getName() + ") is not Verified!");
                getLogger().warning(String.format("Real Address: %s, Address: %s, Hostname: %s",
                        event.getRealAddress(), event.getAddress(), event.getHostname()));
                return;
            } else
                getLogger().info(player.getUniqueId() + " (" + player.getName() + ") is address verified!");
        securedPlayers.add(player.getUniqueId());

        if (player.hasPotionEffect(PotionEffectType.BLINDNESS))
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40,
                    255, true, false));

        BukkitRunnable kickTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!securedPlayers.contains(player.getUniqueId())) cancel();
                if (player.isOnline()) player.kickPlayer("§4Verification unsuccessful!");

                getLogger().warning(player.getUniqueId() + " (" + player.getName() + ") is not verified!");
                getLogger().warning(String.format("Real Address: %s, Address: %s, Hostname: %s",
                        event.getRealAddress(), event.getAddress(), event.getHostname()));
            }
        };
        kickTask.runTaskLater(this, 20 * 10/*seconds*/);
        kickTasks.put(player.getUniqueId(), kickTask.getTaskId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public final void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

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
            getLogger().info("Verification request was send for " + player.getUniqueId() +
                    " (" + player.getName() + ")!");
        }, 5);
    }

    @EventHandler
    public final void onAsyncChat(AsyncPlayerChatEvent event) {
        if (!securedPlayers.contains(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public final void onChat(PlayerChatEvent event) {
        if (!securedPlayers.contains(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public final void onCommand(PlayerCommandPreprocessEvent event) {
        if (!securedPlayers.contains(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public final void onInteract(PlayerInteractEvent event) {
        if (!securedPlayers.contains(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public final void onInventoryClick(InventoryClickEvent event) {
        if (!securedPlayers.contains(event.getWhoClicked().getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public final void onMove(PlayerMoveEvent event) {
        if (!securedPlayers.contains(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public final void onQuit(PlayerQuitEvent event) {
        securedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public final void onDamage(EntityDamageEvent event) {
        if (!securedPlayers.contains(event.getEntity().getUniqueId())) return;

        event.setCancelled(true);
    }

    @Override
    public final void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("bungee:security")) return;
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);
        if (!dataInput.readUTF().equals("verification")) return;

        AES aes = new AES();
        UUID verified = UUID.fromString(dataInput.readUTF());
        String decrypted = null;
        try {
            decrypted = aes.decrypt(dataInput.readUTF(), getKey());
        } catch (Exception exception) {
            getLogger().severe("Error decrypting data!");
        }

        if (decrypted == null || !decrypted.equals(getSecret())) {
            getLogger().warning("WARNING: Plugin Message with incorrect secret received!");
            Player target = Bukkit.getPlayer(verified);
            if (target == null)
                getLogger().severe("Also, the \"verified\" player is not online!");
            else
                target.kickPlayer("§4Verification unsuccessful!");
        } else if (kickTasks.containsKey(verified)) {
            Bukkit.getScheduler().cancelTask(kickTasks.get(verified));
            kickTasks.remove(verified);
            securedPlayers.remove(verified);
            getLogger().info("Verification successful for " + verified + "!");
        } else {
            getLogger().severe("Received Verification for unknown player!");
            getLogger().severe("Please look in your BungeeCord console!");
        }
    }

    private String getSecret() {
        reloadConfig();
        return getConfig().getString("secret");
    }

    private String getKey() {
        reloadConfig();
        return getConfig().getString("key");
    }

    private Boolean isAddressValid(String address) {
        reloadConfig();
        String configAddress = getConfig().getString("address.name");
        return address.equals("/" + configAddress)
                || address.equals(configAddress + "/" + configAddress)
                || address.equals(configAddress);
    }
}