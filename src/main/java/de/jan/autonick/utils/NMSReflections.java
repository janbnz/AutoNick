package de.jan.autonick.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;

public class NMSReflections {

    public static void setField(Class<?> clazz, String fieldName, Object value) {
        try {
            final Field field = getField(clazz, fieldName);
            if (field == null) {
                return;
            }
            field.set(clazz, value);
            field.setAccessible(false);
        } catch (NoSuchFieldError | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static void setField(Object object, String fieldName, Object value) {
        try {
            final Field field = getField(object.getClass(), fieldName);
            if (field == null) {
                return;
            }
            field.set(object, value);
            field.setAccessible(false);
        } catch (NoSuchFieldError | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            final Field modifiers = field.getClass().getDeclaredField("modifiers");
            field.setAccessible(true);
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            return field;
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static Class<?> getNMSClass(String nmsClass) {
        try {
            return Class.forName("net.minecraft.server." + getVersion() + "." + nmsClass);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static Class<?> getCraftBukkitClass(String craftBukkitClass) {
        try {
            return Class.forName("org.bukkit.craftbukkit." + getVersion() + "." + craftBukkitClass);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }
}