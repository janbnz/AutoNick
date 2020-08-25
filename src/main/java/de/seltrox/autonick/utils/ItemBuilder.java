/*
 * (C) Copyright 2019, Seltrox. Jan, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.utils;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;

/**
 * With this class you can create items easily in one line
 */
public class ItemBuilder {

    /**
     * Is the ItemStack which will be returned at the end
     */
    private final ItemStack itemStack;

    /**
     * Is the lore of the item
     */
    private ArrayList<String> lore = new ArrayList<>();

    /**
     * This constructor will init the ItemStack
     *
     * @param material is the item material
     */
    public ItemBuilder(Material material) {
        itemStack = new ItemStack(material);
    }

    /**
     * This constructor will init the ItemStack
     *
     * @param material is the item material
     * @param subId    is the optional SubID of the item
     */
    public ItemBuilder(Material material, byte subId) {
        itemStack = new ItemStack(material, 1, subId);
    }

    /**
     * This method will set the display name of the item
     *
     * @param displayName is the display name
     */
    public ItemBuilder setDisplayName(String displayName) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(displayName);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * This method will set the lore of the item
     *
     * @param lore is the lore
     */
    public ItemBuilder setLore(ArrayList<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        this.lore = lore;
        return this;
    }

    /**
     * This method will set the lore of the item
     *
     * @param lore is the lore
     */
    public ItemBuilder addLore(String lore) {
        ItemMeta meta = itemStack.getItemMeta();
        this.lore.add(lore);
        meta.setLore(this.lore);
        itemStack.setItemMeta(meta);
        return this;
    }


    /**
     * This method will set your item as a skull
     *
     * @param skullOwner The owner of the head
     */
    public ItemBuilder setHead(String skullOwner) {
        SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setOwner(skullOwner);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * This method will add an enchantment to the item
     *
     * @param enchantment The enchantment
     * @param level       The level of the enchantment
     */
    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addEnchant(enchantment, level, false);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * This method will color the item
     *
     * @param color The color
     */
    public ItemBuilder setColor(Color color) {
        if (itemStack.getType() == Material.BANNER) {
            BannerMeta bannerMeta = (BannerMeta) itemStack.getItemMeta();
            bannerMeta.setBaseColor(DyeColor.getByColor(color));
            itemStack.setItemMeta(bannerMeta);
        } else {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemStack.getItemMeta();
            leatherArmorMeta.setColor(color);
            itemStack.setItemMeta(leatherArmorMeta);
        }
        return this;
    }

    /**
     * This method will set your item unbreakable or not
     *
     * @param unbreakable Is true if the item should be unbreakable
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.spigot().setUnbreakable(unbreakable);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * This method will set the amount of the item
     *
     * @param amount The amount of the item
     */
    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * This method will add an ItemFlag to the item
     *
     * @param itemFlag The flag which should be set
     */
    public ItemBuilder addItemFlag(ItemFlag itemFlag) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(itemFlag);
        itemStack.setItemMeta(meta);
        return this;
    }

    /**
     * The ArrayList is empty if there is no lore
     *
     * @return the item lore as ArrayList
     */
    public ArrayList<String> getLore() {
        return lore;
    }

    /**
     * This method will build the item
     *
     * @return the item stack
     */
    public ItemStack build() {
        return itemStack;
    }

}