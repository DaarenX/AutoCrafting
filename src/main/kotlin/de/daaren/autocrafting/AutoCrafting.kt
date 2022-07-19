package de.daaren.autocrafting

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin


class AutoCrafting : JavaPlugin() {
    companion object {
        var instance: AutoCrafting? = null
        private set
    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(Crafter, this)
        addAutoCrafterRecipe()


        instance = this

        Bukkit.getLogger().info("[AutoCrafting] Enabled!")
    }

    private fun addAutoCrafterRecipe() {
        val item = ItemStack(Material.DISPENSER)
        val meta: ItemMeta = item.itemMeta ?: return

        meta.setDisplayName("AutoCrafter")
        val key = NamespacedKey(this, "AutoCrafter")
        meta.persistentDataContainer.set(key, PersistentDataType.INTEGER, 1)
        item.itemMeta = meta

        val recipe = ShapedRecipe(key, item)

        recipe.shape(
            "RHR",
            "SDC",
            "RSR")

        recipe.setIngredient('R', Material.REDSTONE_BLOCK)
        recipe.setIngredient('S', Material.STONE)
        recipe.setIngredient('H', Material.HOPPER)
        recipe.setIngredient('C', Material.CRAFTING_TABLE)
        recipe.setIngredient('D', Material.DISPENSER)

        Bukkit.addRecipe(recipe)
    }
}
