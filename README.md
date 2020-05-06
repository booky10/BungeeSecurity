# BungeeSecurity

A Bungee and Bukkit plugin, which secures your Bungeecord **FOREVER**!

This Secures your BungeeCord system with very little performance impact.
You have to install the Bukkit version on your Bukkit server and the BungeeCord on your Bungeecord server.

**This requires no libraries!**

**Configuration:**
If you don't configure this plugin, you will not
be able to join your server, until you configured it.
The default config should look like this:
config.yml (Bukkit/Bungee)
```YAML
secret: 099f78a7-0a6b-4f88-abd2-1ca6802d94fa
```
Just change the secret in **every** available
configuration file of yours to the same, but keep it private.
When a player connects to a server,
the secret is send to the server, so that the
server knows, that the player is connecting through
the real proxy.

If you find any issues, create an issue report it [here](),
and don't leave a negative review!
