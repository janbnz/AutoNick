/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick;

import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.seltrox.autonick.events.PlayerNickEvent;
import de.seltrox.autonick.utils.GameProfileBuilder;
import de.seltrox.autonick.utils.ItemBuilder;
import de.seltrox.autonick.utils.NMSReflections;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoNickAPI {

  private final HashMap<UUID, String> playerName = new HashMap<>();
  private final HashMap<String, Player> namePlayer = new HashMap<>();
  private final HashMap<Player, BukkitRunnable> run = new HashMap<>();
  private final HashMap<Player, String> oldDisplayname = new HashMap<>();
  private final HashMap<Player, String> nicks = new HashMap<>();
  private final HashMap<Player, String> realUUIDS = new HashMap<>();

  private List<String> names = new ArrayList<>();
  private List<String> skins = new ArrayList<>();
  private final ArrayList<Player> nickedPlayers = new ArrayList<>();

  private final Random random = new Random();

  public AutoNickAPI() {
    initializeNames();
  }

  private void initializeNames() {
    names = AutoNick.getInstance().getConfig().getStringList("Names");
    skins = AutoNick.getInstance().getConfig().getStringList("Skins");
  }

  public void nickPlayer(Player player, String nick) {
    playerName.put(player.getUniqueId(), player.getName());
    namePlayer.put(nick, player);
    realUUIDS.put(player, player.getUniqueId().toString());
    oldDisplayname.put(player, player.getPlayerListName());

    String tabName = (AutoNick.getConfiguration().getBoolean("changeTabname") ? AutoNick
        .getConfiguration().getString("tabName").replace("{NICKNAME}", nick)
        : nick);
    player.setDisplayName(tabName);
    player.setCustomName(tabName);
    player.setPlayerListName(tabName);
    player.setCustomNameVisible(true);

    changeName(nick, player);

    try {
      final Class<?> craftPlayer = NMSReflections.getCraftBukkitClass("entity.CraftPlayer");
      if (craftPlayer == null) {
        throw new NullPointerException(
            "Error while trying to nick player! The CraftPlayer is null!");
      }
      final Method playerHandle = craftPlayer.getMethod("getHandle");
      final Object entityPlayer = playerHandle.invoke(player);

      Object newEntityPlayer = Array.newInstance(entityPlayer.getClass(), 1);
      Array.set(newEntityPlayer, 0, entityPlayer);

      final String version = NMSReflections.getVersion();
      Object playerAddPacket;
      Object playerRemovePacket;

      if (version.equals("v1_7_R4")) {
        Class<?> playOutPlayerInfo = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo");
        if (playOutPlayerInfo == null) {
          throw new NullPointerException(
              "Error while trying to nick player! PacketPlayOutPlayerInfo is null!");
        }
        playerRemovePacket = playOutPlayerInfo
            .getMethod("removePlayer", NMSReflections.getNMSClass("EntityPlayer"))
            .invoke(playOutPlayerInfo, newEntityPlayer);
        playerAddPacket = playOutPlayerInfo
            .getMethod("addPlayer", NMSReflections.getNMSClass("EntityPlayer"))
            .invoke(playOutPlayerInfo, newEntityPlayer);
      } else {
        Object playOutTitle = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getDeclaredClasses()[(version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
            : 2].getField("REMOVE_PLAYER").get(null);
        Class<?> constructorArray = Array.newInstance(NMSReflections.getNMSClass("EntityPlayer"), 0)
            .getClass();
        Constructor<?> packetConstructor = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getConstructor(
                NMSReflections.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[
                    (version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
                        : 2],
                constructorArray);
        playerRemovePacket = packetConstructor.newInstance(playOutTitle, newEntityPlayer);

        Object playOutTitleAdd = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getDeclaredClasses()[(version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
            : 2].getField("ADD_PLAYER").get(null);
        Class<?> constructorArrayAdd = Array
            .newInstance(NMSReflections.getNMSClass("EntityPlayer"), 0).getClass();
        Constructor<?> packetConstructorAdd = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getConstructor(
                NMSReflections.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[
                    (version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
                        : 2],
                constructorArrayAdd);
        playerAddPacket = packetConstructorAdd.newInstance(playOutTitleAdd, newEntityPlayer);
      }

      sendPacket(playerRemovePacket);
      sendPacket(playerAddPacket);
    } catch (NoSuchFieldError | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
      ex.printStackTrace();
    }

    names.remove(nick);
    nickedPlayers.add(player);
    nicks.put(player, nick);

    if (AutoNick.getConfiguration().isBungeeCord()) {
      String tableName = AutoNick.getConfiguration().getString("TableName");
      AutoNick.getInstance().getMySql().update(
          "UPDATE " + tableName + " SET NickName='" + nick + "' WHERE UUID='" + player
              .getUniqueId().toString() + "'");
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
      run.get(player).runTaskLater(AutoNick.getInstance(),
          AutoNick.getConfiguration().getInteger("NickDelayTime") * 20);
    }
  }

  public void nickPlayer(Player player) {
    nickPlayer(player, getRandomNickname());
  }

  public void unnick(Player player) {
    realUUIDS.remove(player);
    String name = getRealName(player.getName());

    try {
      final Class<?> craftPlayer = NMSReflections.getCraftBukkitClass("entity.CraftPlayer");
      if (craftPlayer == null) {
        throw new NullPointerException(
            "Error while trying to unnick player! The CraftPlayer is null!");
      }
      final Method playerHandle = craftPlayer.getMethod("getHandle");
      final Object entityPlayer = playerHandle.invoke(player);

      Object newEntityPlayer = Array.newInstance(entityPlayer.getClass(), 1);
      Array.set(newEntityPlayer, 0, entityPlayer);

      final String version = NMSReflections.getVersion();
      Object playerAddPacket;
      Object playerRemovePacket;

      if (version.equals("v1_7_R4")) {
        Class<?> playOutPlayerInfo = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo");
        if (playOutPlayerInfo == null) {
          throw new NullPointerException(
              "Error while trying to unnick player! The PacketPlayOutPlayerInfo is null!");
        }
        playerRemovePacket = playOutPlayerInfo
            .getMethod("removePlayer", NMSReflections.getNMSClass("EntityPlayer"))
            .invoke(playOutPlayerInfo, newEntityPlayer);
        playerAddPacket = playOutPlayerInfo
            .getMethod("addPlayer", NMSReflections.getNMSClass("EntityPlayer"))
            .invoke(playOutPlayerInfo, newEntityPlayer);
      } else {
        Object playOutTitle = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getDeclaredClasses()[(version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
            : 2].getField("REMOVE_PLAYER").get(null);
        Class<?> constructorArray = Array.newInstance(NMSReflections.getNMSClass("EntityPlayer"), 0)
            .getClass();
        Constructor<?> packetConstructor = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getConstructor(
                NMSReflections.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[
                    (version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
                        : 2],
                constructorArray);
        playerRemovePacket = packetConstructor.newInstance(playOutTitle, newEntityPlayer);

        Object playOutTitleAdd = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getDeclaredClasses()[(version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
            : 2].getField("ADD_PLAYER").get(null);
        Class<?> constructorArrayAdd = Array
            .newInstance(NMSReflections.getNMSClass("EntityPlayer"), 0).getClass();
        Constructor<?> packetConstructorAdd = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getConstructor(
                NMSReflections.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[
                    (version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
                        : 2],
                constructorArrayAdd);
        playerAddPacket = packetConstructorAdd.newInstance(playOutTitleAdd, newEntityPlayer);
      }

      sendPacket(playerRemovePacket);
      sendPacket(playerAddPacket);
    } catch (NoSuchFieldError | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
      ex.printStackTrace();
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
      AutoNick.getInstance().getMySql().update(
          "UPDATE " + tableName + " SET NickName='" + player.getName() + "' WHERE UUID='" + player
              .getUniqueId().toString() + "'");
    }

    if (AutoNick.getConfiguration().getBoolean("changeSkin")) {
      changeSkin(player, player.getUniqueId());
    }

    if (AutoNick.getConfiguration().getBoolean("NickDelay")) {
      run.put(player, new BukkitRunnable() {
        public void run() {
          run.get(player).cancel();
          run.remove(player);
        }
      });
      run.get(player).runTaskLater(AutoNick.getInstance(),
          AutoNick.getConfiguration().getInteger("NickDelayTime") * 20);
    }
  }

  public void quit(Player player) {
    changeName(player.getName(), player);
    nickedPlayers.remove(player);
  }

  private void changeSkin(Player player, UUID uuid) {
    try {
      GameProfile gameProfile = (GameProfile) player.getClass().getMethod("getProfile")
          .invoke(player);
      gameProfile.getProperties().removeAll("textures");

      GameProfile skinGameProfile = GameProfileBuilder.fetch(uuid);
      Collection<Property> props = skinGameProfile.getProperties().get("textures");
      gameProfile.getProperties().putAll("textures", props);

      final Class<?> craftPlayer = NMSReflections.getCraftBukkitClass("entity.CraftPlayer");
      final Method playerHandle = craftPlayer.getMethod("getHandle");
      final Object entityPlayer = playerHandle.invoke(player);

      Object newEntityPlayer = Array.newInstance(entityPlayer.getClass(), 1);
      Array.set(newEntityPlayer, 0, entityPlayer);

      Bukkit.getOnlinePlayers().forEach(currentPlayer -> {
        if (!(currentPlayer.getName().equals(player.getName()))) {
          try {
            sendPacket(currentPlayer, NMSReflections.getNMSClass("PacketPlayOutEntityDestroy")
                .getConstructor(int[].class).newInstance(new int[]{player.getEntityId()}));
          } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            ex.printStackTrace();
          }
        }
      });

      final String version = NMSReflections.getVersion();
      Object playerRemovePacket;
      Object playerAddPacket;

      if (version.equals("v1_7_R4")) {
        Class<?> playOutPlayerInfo = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo");
        playerRemovePacket = playOutPlayerInfo
            .getMethod("removePlayer", NMSReflections.getNMSClass("EntityPlayer"))
            .invoke(playOutPlayerInfo, newEntityPlayer);
        playerAddPacket = playOutPlayerInfo
            .getMethod("addPlayer", NMSReflections.getNMSClass("EntityPlayer"))
            .invoke(playOutPlayerInfo, newEntityPlayer);
      } else {
        Object playOutTitle = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getDeclaredClasses()[(version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
            : 2].getField("REMOVE_PLAYER").get(null);
        Class<?> constructorArray = Array.newInstance(NMSReflections.getNMSClass("EntityPlayer"), 0)
            .getClass();
        Constructor<?> packetConstructor = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getConstructor(
                NMSReflections.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[
                    (version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
                        : 2],
                constructorArray);
        playerRemovePacket = packetConstructor.newInstance(playOutTitle, newEntityPlayer);

        Object playOutTitleAdd = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getDeclaredClasses()[(version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
            : 2].getField("ADD_PLAYER").get(null);
        Class<?> constructorArrayAdd = Array
            .newInstance(NMSReflections.getNMSClass("EntityPlayer"), 0).getClass();
        Constructor<?> packetConstructorAdd = NMSReflections.getNMSClass("PacketPlayOutPlayerInfo")
            .getConstructor(
                NMSReflections.getNMSClass("PacketPlayOutPlayerInfo").getDeclaredClasses()[
                    (version.startsWith("v1_1") && !(version.equals("1_10_R1"))) ? 1
                        : 2],
                constructorArrayAdd);
        playerAddPacket = packetConstructorAdd.newInstance(playOutTitleAdd, newEntityPlayer);
      }

      sendPacket(playerRemovePacket);

      new BukkitRunnable() {
        public void run() {
          try {
            sendPacket(playerAddPacket);

            Object entitySpawnPacket = NMSReflections.getNMSClass("PacketPlayOutNamedEntitySpawn")
                .getConstructor(NMSReflections.getNMSClass("EntityHuman"))
                .newInstance(entityPlayer);

            Bukkit.getOnlinePlayers().forEach(currentPlayer -> {
              if (!(currentPlayer.getName().equals(player.getName()))) {
                sendPacket(currentPlayer, entitySpawnPacket);
              }
            });

            int heldItemSlot = player.getInventory().getHeldItemSlot();
            int food = player.getFoodLevel();
            double health = player.getHealth();
            float xp = player.getExp();
            int level = player.getLevel();
            boolean flying = player.isFlying();

            final Class<?> craftPlayer = NMSReflections.getCraftBukkitClass("entity.CraftPlayer");
            final Method playerHandle = craftPlayer.getMethod("getHandle");
            final Object entityPlayer = playerHandle.invoke(player);

            final Object respawnPacket;
            Object worldClient = entityPlayer.getClass().getMethod("getWorld").invoke(entityPlayer);
            Object worldData = worldClient.getClass().getMethod("getWorldData").invoke(worldClient);
            Object interactManager = entityPlayer.getClass().getField("playerInteractManager")
                .get(entityPlayer);

            final String version = NMSReflections.getVersion();
            if (version.startsWith("v1_16")) {
              Object craftWorld = player.getWorld().getClass().getMethod("getHandle")
                  .invoke(player.getWorld());
              Class<?> enumGameMode = NMSReflections.getNMSClass("EnumGamemode");
              respawnPacket = version.equals("v1_16_R1") ?
                  NMSReflections.getNMSClass("PacketPlayOutRespawn")
                      .getConstructor(NMSReflections.getNMSClass("ResourceKey"),
                          NMSReflections.getNMSClass("ResourceKey"), long.class, enumGameMode,
                          enumGameMode, boolean.class,
                          boolean.class, boolean.class)
                      .newInstance(craftWorld.getClass().getMethod("getTypeKey")
                              .invoke(craftWorld), craftWorld.getClass().getMethod("getDimensionKey")
                              .invoke(craftWorld),
                          NMSReflections.getNMSClass("BiomeManager").getMethod("a", long.class)
                              .invoke(null, player.getWorld().getSeed()),
                          interactManager.getClass().getMethod("getGameMode")
                              .invoke(interactManager),
                          interactManager.getClass().getMethod("c")
                              .invoke(interactManager),
                          craftWorld.getClass().getMethod("isDebugWorld").invoke(craftWorld),
                          craftWorld.getClass().getMethod("isFlatWorld").invoke(craftWorld), true)
                  : NMSReflections.getNMSClass("PacketPlayOutRespawn")
                      .getConstructor(NMSReflections.getNMSClass("DimensionManager"),
                          NMSReflections.getNMSClass("ResourceKey"), long.class, enumGameMode,
                          enumGameMode, boolean.class, boolean.class, boolean.class)
                      .newInstance(
                          craftWorld.getClass().getMethod("getDimensionManager").invoke(craftWorld),
                          craftWorld.getClass().getMethod("getDimensionKey")
                              .invoke(craftWorld),
                          NMSReflections.getNMSClass("BiomeManager").getMethod("a", long.class)
                              .invoke(null, player.getWorld().getSeed()),
                          interactManager.getClass().getMethod("getGameMode")
                              .invoke(interactManager),
                          interactManager.getClass().getMethod("c").invoke(interactManager),
                          craftWorld.getClass().getMethod("isDebugWorld").invoke(craftWorld),
                          craftWorld.getClass().getMethod("isFlatWorld").invoke(craftWorld), true);
            } else if (version.startsWith("v1_15")) {
              Class<?> dimensionManager = NMSReflections.getNMSClass("DimensionManager");
              Class<?> worldType = NMSReflections.getNMSClass("WorldType");
              Class<?> enumGameMode = NMSReflections.getNMSClass("EnumGamemode");
              respawnPacket = NMSReflections.getNMSClass("PacketPlayOutRespawn")
                  .getConstructor(dimensionManager, long.class, worldType, enumGameMode)
                  .newInstance(dimensionManager.getMethod("a", int.class)
                          .invoke(dimensionManager, player.getWorld().getEnvironment().getId()),
                      Hashing.sha256().hashLong(player.getWorld().getSeed()).asLong(),
                      worldType.getMethod("getType", String.class).
                          invoke(worldType, player.getWorld().getWorldType().getName()),
                      enumGameMode.getMethod("getById", int.class)
                          .invoke(enumGameMode, player.getGameMode().getValue()));
            } else if (version.startsWith("v1_14")) {
              Class<?> dimensionManager = NMSReflections.getNMSClass("DimensionManager");
              Class<?> worldType = NMSReflections.getNMSClass("WorldType");
              Class<?> enumGameMode = NMSReflections.getNMSClass("EnumGamemode");
              respawnPacket = NMSReflections.getNMSClass("PacketPlayOutRespawn")
                  .getConstructor(dimensionManager, worldType, enumGameMode)
                  .newInstance(dimensionManager.getMethod("a", int.class)
                          .invoke(dimensionManager, player.getWorld().getEnvironment().getId()),
                      worldType.getMethod("getType", String.class)
                          .invoke(worldType, player.getWorld().getWorldType().getName()),
                      enumGameMode.getMethod("getById", int.class)
                          .invoke(enumGameMode, player.getGameMode().getValue()));
            } else if (version.equals("v1_13_R2")) {
              Object craftWorld = player.getWorld().getClass().getMethod("getHandle")
                  .invoke(player.getWorld());
              respawnPacket = NMSReflections.getNMSClass("PacketPlayOutRespawn")
                  .getConstructor(NMSReflections.getNMSClass("DimensionManager"),
                      NMSReflections.getNMSClass("EnumDifficulty"),
                      NMSReflections.getNMSClass("WorldType"),
                      NMSReflections.getNMSClass("EnumGamemode"))
                  .newInstance(worldClient.getClass().getDeclaredField("dimension").get(craftWorld),
                      worldClient.getClass().getMethod("getDifficulty").invoke(worldClient),
                      worldData.getClass().getMethod("getType").invoke(worldData),
                      interactManager.getClass().getMethod("getGameMode").invoke(interactManager));
            } else {
              Class<?> enumGameMode =
                  (version.equals("v1_8_R2") || version.equals("v1_8_R3") || version
                      .equals("v1_9_R1")
                      ||
                      version.equals("v1_9_R2")) ? NMSReflections.getNMSClass("WorldSettings")
                      .getDeclaredClasses()[0] :
                      NMSReflections.getNMSClass("EnumGamemode");

              respawnPacket = NMSReflections.getNMSClass("PacketPlayOutRespawn")
                  .getConstructor(int.class, NMSReflections.getNMSClass("EnumDifficulty"),
                      NMSReflections.getNMSClass("WorldType"), enumGameMode)
                  .newInstance(player.getWorld().getEnvironment().getId(),
                      (version.equals("v1_7_R4") ? NMSReflections.getNMSClass("World")
                          .getDeclaredField("difficulty").get(worldClient)
                          : worldClient.getClass().getMethod("getDifficulty").invoke(worldClient)),
                      worldData.getClass().getMethod("getType").invoke(worldData),
                      interactManager.getClass().getMethod("getGameMode").invoke(interactManager));
            }

            sendPacket(player, respawnPacket);

            player.teleport(new Location(player.getWorld(), player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(),
                player.getLocation().getYaw(), player.getLocation().getPitch()));

            player.updateInventory();
            player.getInventory().setHeldItemSlot(heldItemSlot);
            player.setHealth(health);
            player.setExp(xp);
            player.setLevel(level);
            player.setFoodLevel(food);
            player.setFlying(flying);
          } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException | InstantiationException | NullPointerException ex) {
            ex.printStackTrace();
          }
        }
      }.runTaskLater(AutoNick.getInstance(), 4L);
    } catch (NoSuchFieldError | NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException | InstantiationException | NullPointerException ex) {
      ex.printStackTrace();
    }
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

  private void sendPacket(Object packet) {
    Bukkit.getOnlinePlayers().forEach(player -> sendPacket(player, packet));
  }

  private void sendPacket(Player player, Object packet) {
    try {
      final Object playerHandle = player.getClass().getMethod("getHandle").invoke(player);
      final Object playerConnection = playerHandle.getClass().getDeclaredField("playerConnection")
          .get(playerHandle);
      playerConnection.getClass().getMethod("sendPacket", NMSReflections.getNMSClass("Packet"))
          .invoke(playerConnection, packet);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException ex) {
      ex.printStackTrace();
    }
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
      ResultSet resultSet = AutoNick.getInstance().getMySql()
          .query("SELECT * FROM " + tableName + " WHERE UUID='" + uuid + "'");
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
      ResultSet resultSet = AutoNick.getInstance().getMySql()
          .query("SELECT * FROM " + tableName + " WHERE UUID='" + uuid + "'");
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
      AutoNick.getInstance().getMySql().update(
          "INSERT INTO " + tableName + "(UUID, Activated, NickName) VALUES ('" + player
              .getUniqueId().toString() + "', '" + 0 + "', '" + player.getName() + "');");
    }

    int state;
    if (hasNickActivated(player.getUniqueId().toString())) {
      state = 0;
      player.sendMessage(AutoNick.getConfiguration().getString("DeactivateMessage"));
      if (AutoNick.getConfiguration().getBoolean("NickItem")) {
        player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
            new ItemBuilder(
                Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDDeactivated")))
                .setDisplayName(AutoNick.getConfiguration().getString("ItemNameDeactivated"))
                .build());
      }
    } else {
      state = 1;
      player.sendMessage(AutoNick.getConfiguration().getString("ActivateMessage"));
      if (AutoNick.getConfiguration().getBoolean("NickItem")) {
        player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
            new ItemBuilder(
                Material.getMaterial(AutoNick.getConfiguration().getInteger("ItemIDActivated")))
                .setDisplayName(AutoNick.getConfiguration().getString("ItemNameActivated"))
                .build());
      }
    }
    AutoNick.getInstance().getMySql().update(
        "UPDATE " + tableName + " SET Activated='" + state + "' WHERE UUID='" + player.getUniqueId()
            .toString() + "'");

    if (AutoNick.getConfiguration().getBoolean("NickDelay")) {
      run.put(player, new BukkitRunnable() {
        public void run() {
          run.get(player).cancel();
          run.remove(player);
        }
      });
      run.get(player).runTaskLater(AutoNick.getInstance(),
          AutoNick.getConfiguration().getInteger("NickDelayTime") * 20);
    }
  }

  public void removeFromDatabase(Player player) {
    String tableName = AutoNick.getConfiguration().getString("TableName");
    AutoNick.getInstance().getMySql().update(
        "DELETE FROM " + tableName + " WHERE UUID='" + player.getUniqueId().toString() + "'");
  }

  private void changeName(String name, Player player) {
    try {
      Method getHandle = player.getClass().getMethod("getHandle");
      Object entityPlayer = getHandle.invoke(player);
      /*
       * These methods are no longer needed, as we can just access the
       * profile using handle.getProfile. Also, because we can just use
       * the method, which will not change, we don't have to do any
       * field-name look-ups.
       */
      boolean gameProfileExists = false;
      // Some 1.7 versions had the GameProfile class in a different package
      try {
        Class.forName("net.minecraft.util.com.mojang.authlib.GameProfile");
        gameProfileExists = true;
      } catch (ClassNotFoundException ignored) {

      }
      try {
        Class.forName("com.mojang.authlib.GameProfile");
        gameProfileExists = true;
      } catch (ClassNotFoundException ignored) {

      }
      if (!gameProfileExists) {
        /*
         * Only 1.6 and lower servers will run this code.
         *
         * In these versions, the name wasn't stored in a GameProfile object,
         * but as a String in the (final) name field of the EntityHuman class.
         * Final (non-static) fields can actually be modified by using
         * {@link java.lang.reflect.Field#setAccessible(boolean)}
         */
        Field nameField = entityPlayer.getClass().getSuperclass().getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(entityPlayer, name);
      } else {
        // Only 1.7+ servers will run this code
        Object profile = entityPlayer.getClass().getMethod("getProfile").invoke(entityPlayer);
        Field ff = profile.getClass().getDeclaredField("name");
        ff.setAccessible(true);
        ff.set(profile, name);
      }
      // In older versions, Bukkit.getOnlinePlayers() returned an Array instead of a Collection.
      if (Bukkit.class.getMethod("getOnlinePlayers").getReturnType()
          == Collection.class) {
        Collection<? extends Player> players = (Collection<? extends Player>) Bukkit.class
            .getMethod("getOnlinePlayers").invoke(null);
        for (Player p : players) {
          p.hidePlayer(player);
          p.showPlayer(player);
        }
      } else {
        Player[] players = ((Player[]) Bukkit.class.getMethod("getOnlinePlayers").invoke(null));
        for (Player p : players) {
          p.hidePlayer(player);
          p.showPlayer(player);
        }
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
      e.printStackTrace();
    }
  }

  public String getRealUUID(Player player) {
    return realUUIDS.get(player);
  }
}