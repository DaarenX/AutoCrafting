package de.daaren.autocrafting

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class AutoCrafting : JavaPlugin() {
    companion object {
        var instance: AutoCrafting? = null
        private set;
    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(Crafter, this)

        instance = this;

        Bukkit.getLogger().info("[AutoCrafting] Enabled!")
    }
}