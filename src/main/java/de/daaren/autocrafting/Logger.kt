package de.daaren.autocrafting

import org.bukkit.Bukkit

object Logger {
    val log = Bukkit.getLogger()
    val prefix = "[AutoCrafting]"

    fun info(msg: String) {
        log.info("$prefix $msg")
    }
}