package tk.t11e.bungeesecurity.bungee.main;
// Created by booky10 in BungeeSecurity (20:38 05.05.20)

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import tk.t11e.bungeesecurity.AES;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@SuppressWarnings({"ResultOfMethodCallIgnored", "UnstableApiUsage"})
public final class Main extends Plugin implements Listener {

    private final File configFile = new File(getDataFolder(), "config.yml");
    private Configuration config;
    private final ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);

    @Override
    public final void onEnable() {
        createConfig();
        register();
    }

    private void register() {
        //Plugin Messages
        getProxy().registerChannel("bungee:security");

        //Listener
        getProxy().getPluginManager().registerListener(this, this);
    }

    private void createConfig() {
        try {
            //Creating config file/folder
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            if (!configFile.exists()) configFile.createNewFile();

            //Initial loading
            reloadConfig();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }

        //Setting default values
        if (!config.contains("secret")) config.set("secret", UUID.randomUUID().toString());
        if (!config.contains("key")) config.set("key", UUID.randomUUID().toString());

        //Save and reload
        saveConfig();
        reloadConfig();
    }

    private void saveConfig() {
        try {
            provider.save(config, configFile);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void reloadConfig() {
        try {
            config = provider.load(configFile);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @EventHandler
    public final void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("bungee:security")) return;

        ByteArrayDataInput dataInput = ByteStreams.newDataInput(event.getData());
        if (!dataInput.readUTF().equals("request")) return;

        UUID requested = UUID.fromString(dataInput.readUTF());

        ProxiedPlayer target = getProxy().getPlayer(requested);
        if (target != null && target.isConnected()) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream);
            AES aes = new AES();

            try {
                outputStream.writeUTF("verification");
                outputStream.writeUTF(requested.toString());
                outputStream.writeUTF(aes.encrypt(getSecret(), getKey()));
            } catch (IOException exception) {
                getLogger().severe("Error while writing plugin message!");
            } catch (Exception exception) {
                getLogger().severe("An Error happened, while encrypting secret!");
                return;
            }
            target.sendData("bungee:security", byteArrayOutputStream.toByteArray());
            target.getServer().sendData("bungee:security", byteArrayOutputStream.toByteArray());

            getLogger().info("Send secret to player " + requested + " (" + target.getName() + ")!");
        } else {
            getLogger().severe("Received invalid verification request for an unknown player!");
            getLogger().severe("Maybe one of your bukkit servers is getting hacked!");
            getLogger().severe("UUID: " + requested);
        }
    }

    private String getSecret() {
        //Reloading config
        reloadConfig();

        //Getting secret
        return config.getString("secret");
    }

    private String getKey() {
        //Reloading config
        reloadConfig();

        //Getting Key
        return config.getString("key");
    }
}