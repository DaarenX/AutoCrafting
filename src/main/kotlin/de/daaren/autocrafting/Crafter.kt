package de.daaren.autocrafting

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.*
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.logging.Logger

object Crafter : Listener {
    private val logger: Logger = Bukkit.getLogger()
    @EventHandler
    fun createAutoCrafter(e : HangingPlaceEvent) {
        if (e.entity.type != EntityType.ITEM_FRAME && e.entity.type != EntityType.GLOW_ITEM_FRAME) {
            return
        }
        if (e.block.type != Material.DISPENSER) {
            return
        }
        if(e.block.state !is TileState)
            return

        if(e.player == null)
            return
        val state: TileState = e.block.state as TileState
        val container: PersistentDataContainer = state.persistentDataContainer
        val key = NamespacedKey(AutoCrafting.instance as Plugin, "is-auto-crafter")

        container.set(key, PersistentDataType.INTEGER, 1)

        state.update()
        e.player?.sendMessage("Created AutoCrafter at [${e.block.x}, ${e.block.y}, ${e.block.z}]!")
    }

    @EventHandler
    fun runCraftOnDispense(e: BlockDispenseEvent) {
        if(e.block.state !is TileState) {
            return
        }
        val state: TileState = e.block.state as TileState
        val container: PersistentDataContainer = state.persistentDataContainer
        val key = NamespacedKey(AutoCrafting.instance as Plugin, "is-auto-crafter")
        if(!container.has(key, PersistentDataType.INTEGER)) {
            return
        }

        e.isCancelled = true

        val run = Runnable {
            synchronized(this) { //probably not needed
                val direction = e.block.location.direction
                val targetBlock = e.block.getRelative(direction.blockX, direction.blockY, direction.blockZ)
                if(targetBlock.state !is Container) {
                    return@Runnable
                }
                val targetInv = (targetBlock.state as Container).inventory
                val recipe: Recipe = getCraftingRecipeFromInventory(e.block) ?: return@Runnable
                val sourceBlock = e.block.getRelative(BlockFace.UP)
                if (sourceBlock.state !is Container) {
                    return@Runnable
                }
                val sourceInv: Inventory = (sourceBlock.state as Container).inventory

                if(!itemFitsInTargetInventory(targetInv, recipe.result) || !checkAndRemoveCraftingMaterialsFromSourceInventory(recipe, sourceInv)) {
                    return@Runnable
                }
                targetInv.addItem(recipe.result)
            }
        }
        Bukkit.getScheduler().runTaskLater(AutoCrafting.instance as Plugin, run, 1) // Wait until dispensed item is restored in next tick

    }

    private fun itemFitsInTargetInventory(targetInv: Inventory, item: ItemStack): Boolean {
        val targetInvCopy = Bukkit.createInventory(null, targetInv.size)
        targetInvCopy.storageContents = targetInv.storageContents.clone()
        return targetInvCopy.addItem(item).isEmpty() // NotEmpty -> StepInventory is too tight 🥵🥵🥵
    }

    private fun getCraftingRecipeFromInventory(dispenserBlock : Block): Recipe? {
        val dispenser: Dispenser = dispenserBlock.state as Dispenser
        val inv = dispenser.inventory.storageContents
        val dLoc = dispenserBlock.location
        return Bukkit.getCraftingRecipe(inv, dLoc.world!!)
    }

    private fun checkAndRemoveCraftingMaterialsFromSourceInventory(recipe: Recipe?, sourceInv: Inventory): Boolean {
        val ingredients: Array<ItemStack> = when (recipe) {
            is ShapelessRecipe -> recipe.ingredientList.toTypedArray()
            is ShapedRecipe -> recipe.ingredientMap.values.toTypedArray()
            else ->  {
                logger.warning("[AutoCrafting] Recipe error") // shouldn't occur?
                return false
            }
        }
        val sourceInvCopy = Bukkit.createInventory(null,sourceInv.size)
        sourceInvCopy.storageContents = sourceInv.storageContents.clone()
        if(sourceInv.removeItem(*ingredients).isNotEmpty()) return false
        sourceInv.removeItem(*ingredients)
        logger.finest("[AutoCrafting] Crafted Item: ${recipe.result}")
        return true
    }
}