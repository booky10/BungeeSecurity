# BungeeSecurity

A Bungee and Bukkit plugin, which secures your Bungeecord Network with AES encryption and the [OnlyProxyJoin](https://www.spigotmc.org/resources/12932/) princible!

This Secures your BungeeCord system with very little performance impact on performance. You have to install the Bukkit version on your Bukkit server and the BungeeCord on your Bungeecord server.

**This requires no libraries!**

**Configuration:**
If you don't configure this plugin, you will not
be able to join your server, until you configured it.
The default config should look like this:
```YAML
secret: 099f78a7-0a6b-4f88-abd2-1ca6802d94fa
key: fb8fc4d3-f78e-4a61-b26c-9cd042c6ec04
address:
  name: 0.0.0.0
  enabled: true
```
Just change the secret in every available
configuration file of yours to the same, but keep it private.
When a player connects to a server,
the secret is send to the server, so that the
server knows, that the player is connecting through
the real proxy.

**The "address" section is only available on your backend server!**

If you want to use the "address" section, change "enabled" to "true" an reload or restart your server. If you join now, you can not join the server. Look in your console and find a message, that looks like this:
```
Real Address: <address>, Address: <address>, Hostname: <address>
```
Then put in the "name" field of the "address" section the address, that is labeled with "Real Address". When you reload/restart you server now, you should no longer be kicked.

If you find any issues, create an issue [here](https://github.com/booky10/BungeeSecurity/issues), don't leave a negative review!
