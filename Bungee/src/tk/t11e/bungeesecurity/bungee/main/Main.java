package tk.t11e.bungeesecurity.bungee.main;
// Created by booky10 in BungeeSecurity (20:38 05.05.20)

import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main extends Plugin implements Listener {

    private final File configFile = new File(getDataFolder(), "config.yml");
    private Configuration config;

    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdirs();
            if (!configFile.exists())
                configFile.createNewFile();
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException exception) {
            getLogger().severe("Error while creating/loading \"config.yml\"!");
            getLogger().severe(exception.toString());
            return;
        }

        if (!config.contains("secret")) {
            config.set("secret", UUID.randomUUID().toString());
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
            } catch (IOException exception) {
                getLogger().severe("Error while saving default secret!");
                getLogger().severe(exception.toString());
                return;
            }
        }

        getProxy().registerChannel("bungee:security");
        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onServerConnected(ServerConnectEvent event) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            outputStream.writeUTF("verify");
            outputStream.writeUTF(event.getPlayer().getUniqueId().toString());
            outputStream.writeUTF(getSecret());
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
        getLogger().info("Prepared secret message for \"" + event.getPlayer().getName() + "\"!");

        getProxy().getScheduler().schedule(this, () -> {
            if (!event.getPlayer().isConnected()) return;

            event.getPlayer().sendData("bungee:security", byteArrayOutputStream.toByteArray());
            event.getPlayer().getServer().sendData("bungee:security",byteArrayOutputStream.toByteArray());
            getLogger().info("Send secret to player " + event.getPlayer().getUniqueId()
                    + " (" + event.getPlayer().getName() + ")!");
        }, 1, TimeUnit.SECONDS);
    }

    private String getSecret() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException exception) {
            getLogger().severe("Error while reloading \"config.yml\"!");
            getLogger().severe(exception.toString());
        }
        return config.getString("secret");
    }
}