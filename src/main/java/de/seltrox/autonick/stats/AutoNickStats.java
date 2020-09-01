/*
 * (C) Copyright 2020, Jan Benz, http://seltrox.de.
 *
 * This software is released under the terms of the Unlicense.
 * See https://unlicense.org/
 * for more information.
 *
 */

package de.seltrox.autonick.stats;

import de.seltrox.autonick.AutoNick;
import org.bstats.bukkit.Metrics;

public class AutoNickStats {

    public AutoNickStats(AutoNick plugin) {
        final int pluginId = 8730;
        final Metrics metrics = new Metrics(plugin, pluginId);
    }

}