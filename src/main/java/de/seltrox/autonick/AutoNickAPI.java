/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.seltrox.autonick.events.PlayerNickEvent;
import de.seltrox.autonick.utils.GameProfileBuilder;
import de.seltrox.autonick.utils.ItemBuilder;
import de.seltrox.autonick.utils.UUIDFetcher;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AutoNickAPI {

    private HashMap<UUID, String> playerName = new HashMap<>();
    private HashMap<String, Player> namePlayer = new HashMap<>();
    private HashMap<Player, BukkitRunnable> run = new HashMap<>();
    private HashMap<Player, String> oldDisplayname = new HashMap<>();
    private HashMap<Player, String> nicks = new HashMap<>();
    private HashMap<Player, String> realUUIDS = new HashMap<>();

    private List<String> names = new ArrayList<>();
    private List<String> skins = new ArrayList<>();
    private ArrayList<Player> nickedPlayers = new ArrayList<>();

    private Random random = new Random();

    public AutoNickAPI() {
        initializeNames();
    }

    private void initializeNames() {
        names = AutoNick.getInstance().getConfig().getStringList("Names");
        skins = AutoNick.getInstance().getConfig().getStringList("Skins");
    }

    public boolean hasDelayParsed(Player player) {
        return !run.containsKey(player);
    }

    public void nickPlayer(Player player, String nick) {
        playerName.put(player.getUniqueId(), player.getName());
        namePlayer.put(nick, player);
        realUUIDS.put(player, player.getUniqueId().toString());

        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer nmsWorld = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();
        EntityPlayer newEntityPlayer = new EntityPlayer(nmsServer, nmsWorld, new GameProfile(player.getUniqueId(), nick), new PlayerInteractManager(nmsWorld));
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        for (Player players : Bukkit.getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) players).getHandle().playerConnection;
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer));
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, newEntityPlayer));
        }

        oldDisplayname.put(player, player.getPlayerListName());

        String tabName;
        if (AutoNick.getConfiguration().getBoolean("changeTabname")) {
            tabName = AutoNick.getConfiguration().getString("tabName").replace("{NICKNAME}", nick);
        } else {
            tabName = nick;
        }

        player.setDisplayName(tabName);
        player.setCustomName(tabName);
        player.setPlayerListName(tabName);
        player.setCustomNameVisible(true);

        changeName(nick, player);

        names.remove(nick);
        nickedPlayers.add(player);
        nicks.put(player, nick);

        if (AutoNick.getConfiguration().isBungeeCord()) {
            String tableName = AutoNick.getConfiguration().getString("TableName");
            AutoNick.getInstance().getMySql().update("UPDATE " + tableName + " SET NickName='" + nick + "' WHERE UUID='" + player.getUniqueId().toString() + "'");
        }

        Bukkit.getPluginManager().callEvent(new PlayerNickEvent(player, nick));

        if (AutoNick.getConfiguration().getBoolean("changeSkin")) {
            changeSkin(player);
        }

        if (AutoNick.getConfiguration().getBoolean("NickDelay")) {
            run.put(player, new BukkitRunnable() {
                public void run() {
                    run.get(player).cancel();
                    run.remove(player);
                }
            });
            run.get(player).runTaskLater(AutoNick.getInstance(), AutoNick.getConfiguration().getInteger("NickDelayTime") * 20);
        }
    }

    public void nickPlayer(Player player) {
        nickPlayer(player, getRandomNickname());
    }

    public void unnick(Player player) {
        realUUIDS.remove(player);
        String name = getRealName(player.getName());
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer nmsWorld = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();
        EntityPlayer newEntityPlayer = new EntityPlayer(nmsServer, nmsWorld, new GameProfile(player.getUniqueId(), name), new PlayerInteractManager(nmsWorld));
        EntityPlayer entityPlayer = new EntityPlayer(nmsServer, nmsWorld, new GameProfile(player.getUniqueId(), player.getCustomName()), new PlayerInteractManager(nmsWorld));

        for (Player players : Bukkit.getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) players).getHandle().playerConnection;
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer));
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, newEntityPlayer));
        }

        names.add(player.getName());

        player.setDisplayName(oldDisplayname.get(player));
        player.setCustomName(oldDisplayname.get(player));
        player.setCustomNameVisible(true);
        player.setPlayerListName(player.getName());
        nickedPlayers.remove(player);
        oldDisplayname.remove(player);
        nicks.remove(player);

        Bukkit.getPluginManager().callEvent(new PlayerNickEvent(player, player.getName()));
        changeName(name, player);

        if (AutoNick.getConfiguration().isBungeeCord()) {
            String tableName = AutoNick.getConfiguration().getString("TableName");
            AutoNick.getInstance().getMySql().update("UPDATE " + tableName + " SET NickName='" + player.getName() + "' WHERE UUID='" + player.getUniqueId().toString() + "'");
        }

        if (AutoNick.getConfiguration().getBoolean("changeSkin")) {
            changeSkin(player, UUIDFetcher.getUUID(name));
        }

        if (AutoNick.getConfiguration().getBoolean("NickDelay")) {
            run.put(player, new BukkitRunnable() {
                public void run() {
                    run.get(player).cancel();
                    run.remove(player);
                }
            });
            run.get(player).runTaskLater(AutoNick.getInstance(), AutoNick.getConfiguration().getInteger("NickDelayTime") * 20);
        }
    }

    public void quit(Player player) {
        changeName(player.getName(), player);
        nickedPlayers.remove(player);
    }

    private void changeSkin(Player player, UUID uuid) {
        CraftPlayer cp = (CraftPlayer) player;
        GameProfile skingp = cp.getProfile();
        try {
            skingp = GameProfileBuilder.fetch(uuid);
        } catch (Exception e) {
            try {
                skingp = GameProfileBuilder.fetch(UUID.fromString("4d0c325f-1dd4-403a-8268-3ccf42e981a4"));
            } catch (IOException ignored) {
            }
        }

        Collection<Property> props = skingp.getProperties().get("textures");
        cp.getProfile().getProperties().removeAll("textures");
        cp.getProfile().getProperties().putAll("textures", props);

        sendPacket(new PacketPlayOutEntityDestroy(cp.getEntityId()));
        sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, cp.getHandle()));

        new BukkitRunnable() {
            public void run() {
                sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, cp.getHandle()));
                PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(cp.getHandle());
                for (Player all : Bukkit.getOnlinePlayers()) {
                    if (!all.getName().equals(cp.getName())) {
                        ((CraftPlayer) all).getHandle().playerConnection.sendPacket(spawn);
                    }
                }

                int heldItemSlot = player.getInventory().getHeldItemSlot();
                int food = player.getFoodLevel();
                double health = player.getHealth();
                float xp = player.getExp();
                int level = player.getLevel();
                boolean flying = player.isFlying();

                cp.getHandle().playerConnection.sendPacket(new PacketPlayOutRespawn(cp.getHandle().getWorld().worldProvider.getDimension(),
                        cp.getHandle().getWorld().getDifficulty(), cp.getHandle().getWorld().worldData.getType(),
                        WorldSettings.EnumGamemode.getById(cp.getGameMode().getValue())));

                cp.getHandle().playerConnection.teleport(new Location(player.getWorld(), cp.getHandle().locX, cp.getHandle().locY, cp.getHandle().locZ, cp.getHandle().yaw, cp.getHandle().pitch));
                player.updateInventory();
                player.getInventory().setHeldItemSlot(heldItemSlot);
                player.setHealth(health);
                player.setExp(xp);
                player.setLevel(level);
                player.setFoodLevel(food);
                player.setFlying(flying);
            }
        }.runTaskLater(AutoNick.getInstance(), 4L);
    }

    private void changeSkin(Player player) {
        changeSkin(player, UUID.fromString(skins.get(random.nextInt(skins.size()))));
    }

    public boolean isNicked(Player player) {
        return nickedPlayers.contains(player);
    }

    public String getRealName(String name) {
        if (namePlayer.containsKey(name)) {
            Player player = namePlayer.get(name);
            if (playerName.containsKey(player.getUniqueId())) {
                return playerName.get(player.getUniqueId());
            }
        }
        return null;
    }

    private void sendPacket(Packet packet) {
        Bukkit.getOnlinePlayers().forEach(all -> ((CraftPlayer) all).getHandle().playerConnection.sendPacket(packet));
    }

    public String getNickname(Player player) {
        return nicks.get(player);
    }

    public boolean hasDelay(Player player) {
        return run.containsKey(player);
    }

    public ArrayList<Player> getNickedPlayers() {
        return nickedPlayers;
    }

    public boolean isPlayerExisting(String uuid) {
        try {
            String tableName = AutoNick.getConfiguration().getString("TableName");
            ResultSet resultSet = AutoNick.getInstance().getMySql().query("SELECT * FROM " + tableName + " WHERE UUID='" + uuid + "'");
            if (resultSet.next()) {
                return resultSet.getString("UUID") != null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public String getRandomNickname() {
        return names.get(random.nextInt(names.size()));
    }

    public boolean hasNickActivated(String uuid) {
        if (!isPlayerExisting(uuid)) {
            return false;
        }
        try {
            String tableName = AutoNick.getConfiguration().getString("TableName");
            ResultSet resultSet = AutoNick.getInstance().getMySql().query("SELECT * FROM " + tableName + " WHERE UUID='" + uuid + "'");
            if (resultSet.next()) {
                return resultSet.getInt("Activated") == 1;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public void toggleNick(Player player) {
        String tableName = AutoNick.getConfiguration().getString("TableName");
        if (!isPlayerExisting(player.getUniqueId().toString())) {
            AutoNick.getInstance().getMySql().update("INSERT INTO " + tableName + "(UUID, Activated, NickName) VALUES ('" + player.getUniqueId().toString() + "', '" + 0 + "', '" + player.getName() + "');");
        }

        int state;
        if (hasNickActivated(player.getUniqueId().toString())) {
            state = 0;
            player.sendMessage(AutoNick.getConfiguration().getString("DeactivateMessage"));
            if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")))
                        .setDisplayName(AutoNick.getConfiguration().getString("ItemNameDeactivated")).build());
            }
        } else {
            state = 1;
            player.sendMessage(AutoNick.getConfiguration().getString("ActivateMessage"));
            if (AutoNick.getConfiguration().getBoolean("NickItem")) {
                player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated")))
                        .setDisplayName(AutoNick.getConfiguration().getString("ItemNameActivated")).build());
            }
        }
        AutoNick.getInstance().getMySql().update("UPDATE " + tableName + " SET Activated='" + state + "' WHERE UUID='" + player.getUniqueId().toString() + "'");

        if (AutoNick.getConfiguration().getBoolean("NickDelay")) {
            run.put(player, new BukkitRunnable() {
                public void run() {
                    run.get(player).cancel();
                    run.remove(player);
                }
            });
            run.get(player).runTaskLater(AutoNick.getInstance(), AutoNick.getConfiguration().getInteger("NickDelayTime") * 20);
        }
    }

    public void removeFromDatabase(Player player) {
        String tableName = AutoNick.getConfiguration().getString("TableName");
        AutoNick.getInstance().getMySql().update("DELETE FROM " + tableName + " WHERE UUID='" + player.getUniqueId().toString() + "'");
    }

    private void changeName(String name, Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle", (Class<?>[]) null);
            Object entityPlayer = getHandle.invoke(player);
            Class<?> entityHuman = entityPlayer.getClass().getSuperclass();
            Field bH = entityHuman.getDeclaredField("bH");
            bH.setAccessible(true);
            bH.set(entityPlayer, new GameProfile(player.getUniqueId(), name));
            for (Player players : Bukkit.getOnlinePlayers()) {
                players.hidePlayer(player);
                players.showPlayer(player);
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public String getRealUUID(Player player) {
        return realUUIDS.get(player);
    }
}