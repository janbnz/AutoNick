package de.jan.autonick;

import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.jan.autonick.database.DatabaseRegistry;
import de.jan.autonick.events.PlayerNickEvent;
import de.jan.autonick.utils.GameProfileBuilder;
import de.jan.autonick.utils.ItemBuilder;
import de.jan.autonick.utils.NMSReflections;
import de.jan.autonick.player.NickPlayer;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoNickAPI {

    private final HashMap<UUID, String> playerName = new HashMap<>();
    private final HashMap<String, Player> namePlayer = new HashMap<>();
    private final HashMap<Player, BukkitRunnable> run = new HashMap<>();

    private final HashMap<Player, String> oldDisplayName = new HashMap<>(), nicks = new HashMap<>(), realUUIDS = new HashMap<>();
    private final HashMap<Player, String> oldPlayerListName = new HashMap<>();

    private List<String> names = new ArrayList<>(), skins = new ArrayList<>();
    private final ArrayList<Player> nickedPlayers = new ArrayList<>();

    private final Random random = new Random();

    private static boolean joinNickDisabled = false;

    public AutoNickAPI() {
        this.initializeNames();
    }

    private void initializeNames() {
        this.names = AutoNick.getInstance().getConfig().getStringList("Names").stream().filter(name -> name.length() <= 16 && name.length() >= 3).collect(Collectors.toList());
        this.skins = AutoNick.getInstance().getConfig().getStringList("Skins");
    }

    public void nickPlayer(Player player, String nick) {
        playerName.put(player.getUniqueId(), player.getName());
        namePlayer.put(nick, player);
        realUUIDS.put(player, player.getUniqueId().toString());
        oldDisplayName.put(player, player.getDisplayName());
        oldPlayerListName.put(player, player.getPlayerListName());

        String tabName = (AutoNick.getConfiguration().getBoolean("changeTabname") ? AutoNick
                .getConfiguration().getString("tabName").replace("{NICKNAME}", nick) : nick);

        player.setDisplayName(tabName);
        player.setCustomName(tabName);
        player.setPlayerListName(tabName);
        player.setCustomNameVisible(true);

        changeName(nick, player);

        refreshPlayer(player);

        names.remove(nick);
        nickedPlayers.add(player);
        nicks.put(player, nick);

        if (AutoNick.getConfiguration().isBungeeCord())
            DatabaseRegistry.getDatabase().setNickname(player.getUniqueId(), nick);

        Bukkit.getPluginManager().callEvent(new PlayerNickEvent(player, nick));

        if (AutoNick.getConfiguration().getBoolean("changeSkin")) {
            changeSkin(player);
        }

        if (AutoNick.getConfiguration().getBoolean("nickItem.delay.enabled")) {
            run.put(player, new BukkitRunnable() {
                public void run() {
                    run.get(player).cancel();
                    run.remove(player);
                }
            });
            run.get(player).runTaskLater(AutoNick.getInstance(),
                    AutoNick.getConfiguration().getInteger("nickItem.delay.time") * 20L);
        }
    }

    public void onPlayerInteract(Player player) {
        if (AutoNick.getConfiguration().isBungeeCord()) {
            this.toggleNick(player);
        } else {
            if (this.isNicked(player)) {
                player.sendMessage(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.unnickMessage").replace("{NICKNAME}", player.getCustomName()));
                this.unnick(player);
                if (AutoNick.getConfiguration().getBoolean("nickItem.enabled")) {
                    player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.deactivatedItem.id")))
                            .setDisplayName(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.itemName")).build());
                }
            } else {
                this.nickPlayer(player);
                if (AutoNick.getConfiguration().getBoolean("nickItem.enabled")) {
                    player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(), new ItemBuilder(Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.activatedItem.id")))
                            .setDisplayName(AutoNick.getConfiguration().getString("nickItem.activatedItem.itemName")).build());
                }
                player.sendMessage(AutoNick.getConfiguration().getString("nickItem.activatedItem.nickMessage").replace("{NICKNAME}", AutoNick.getApi().getNickname(player)));
            }
        }
    }

    public void nickPlayer(Player player) {
        nickPlayer(player, getRandomNickname());
    }

    public void refreshPlayer(Player player) {
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
        } catch (NoSuchFieldError | InstantiationException | InvocationTargetException | NoSuchMethodException |
                 IllegalAccessException | NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }

    public void unnick(Player player) {
        realUUIDS.remove(player);
        String name = getRealName(player.getName());

        refreshPlayer(player);

        names.add(player.getName());

        player.setDisplayName(oldDisplayName.get(player));
        player.setCustomName(oldDisplayName.get(player));
        player.setCustomNameVisible(true);
        player.setPlayerListName(oldPlayerListName.get(player));
        nickedPlayers.remove(player);
        oldDisplayName.remove(player);
        oldPlayerListName.remove(player);
        nicks.remove(player);

        Bukkit.getPluginManager().callEvent(new PlayerNickEvent(player, player.getName()));
        changeName(name, player);

        if (AutoNick.getConfiguration().isBungeeCord())
            DatabaseRegistry.getDatabase().setNickname(player.getUniqueId(), player.getName());

        if (AutoNick.getConfiguration().getBoolean("changeSkin")) {
            changeSkin(player, player.getUniqueId());
        }

        if (AutoNick.getConfiguration().getBoolean("nickItem.delay.enabled")) {
            run.put(player, new BukkitRunnable() {
                public void run() {
                    run.get(player).cancel();
                    run.remove(player);
                }
            });
            run.get(player).runTaskLater(AutoNick.getInstance(),
                    AutoNick.getConfiguration().getInteger("nickItem.delay.time") * 20L);
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
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException ex) {
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
                        if (craftPlayer == null) {
                            return;
                        }
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
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                             NoSuchFieldException | InstantiationException | NullPointerException ex) {
                        ex.printStackTrace();
                    }
                }
            }.runTaskLater(AutoNick.getInstance(), 4L);
        } catch (NoSuchFieldError | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 NoSuchFieldException | InstantiationException | NullPointerException ex) {
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

    public String getRandomNickname() {
        return names.get(random.nextInt(names.size()));
    }

    public void getPlayerInformation(UUID uuid, DatabaseRegistry.Callback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(AutoNick.getInstance(), () -> {
            final NickPlayer player = new NickPlayer(uuid, DatabaseRegistry.getDatabase().isExisting(uuid),
                    DatabaseRegistry.getDatabase().hasNickActivated(uuid));
            Bukkit.getScheduler().runTask(AutoNick.getInstance(), () -> callback.onSuccess(player));
        });
    }

    public void toggleNick(Player player) {
        this.getPlayerInformation(player.getUniqueId(), new DatabaseRegistry.Callback() {
            @Override
            public void onSuccess(NickPlayer nickPlayer) {
                if (!(nickPlayer.isExisting())) DatabaseRegistry.getDatabase().createPlayer(player);

                int state;
                if (nickPlayer.isNickActivated()) {
                    state = 0;
                    player.sendMessage(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.deactivateMessage"));
                    if (AutoNick.getConfiguration().getBoolean("nickItem.enabled")) {
                        player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                                new ItemBuilder(
                                        Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.deactivatedItem.id")))
                                        .setDisplayName(AutoNick.getConfiguration().getString("nickItem.deactivatedItem.itemName"))
                                        .build());
                    }
                } else {
                    state = 1;
                    player.sendMessage(AutoNick.getConfiguration().getString("nickItem.activatedItem.activateMessage"));
                    if (AutoNick.getConfiguration().getBoolean("nickItem.enabled")) {
                        player.getInventory().setItem(AutoNick.getConfiguration().getNickItemSlot(),
                                new ItemBuilder(
                                        Material.getMaterial(AutoNick.getConfiguration().getInteger("nickItem.activatedItem.id")))
                                        .setDisplayName(AutoNick.getConfiguration().getString("nickItem.activatedItem.itemName"))
                                        .build());
                    }
                }
                DatabaseRegistry.getDatabase().setNickState(player.getUniqueId(), state);

                if (AutoNick.getConfiguration().getBoolean("nickItem.delay.enabled")) {
                    run.put(player, new BukkitRunnable() {
                        public void run() {
                            run.get(player).cancel();
                            run.remove(player);
                        }
                    });
                    run.get(player).runTaskLater(AutoNick.getInstance(),
                            AutoNick.getConfiguration().getInteger("nickItem.delay.time") * 20L);
                }
            }

            @Override
            public void onFailure(Exception exception) {
            }
        });
    }

    public void removeFromDatabase(Player player) {
        DatabaseRegistry.getDatabase().deletePlayer(player.getUniqueId());
    }

    public void changeName(String name, Player player) {
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
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public String getRealUUID(Player player) {
        return realUUIDS.get(player);
    }

    public static void setJoinNickDisabled(boolean joinNickDisabled) {
        AutoNickAPI.joinNickDisabled = joinNickDisabled;
    }

    public static boolean isJoinNickDisabled() {
        return joinNickDisabled;
    }
}