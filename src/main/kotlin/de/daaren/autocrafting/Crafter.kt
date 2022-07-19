package de.daaren.autocrafting

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.*
import org.bukkit.block.data.Directional
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.logging.Logger

object Crafter : Listener {
    private val logger: Logger = Bukkit.getLogger()
    private val autoCrafterKey = NamespacedKey(AutoCrafting.instance as Plugin, "AutoCrafter")

    @EventHandler
    fun placeAutoCrafter(e: BlockPlaceEvent) {
        if(e.itemInHand.type != Material.DISPENSER) return

        if(e.itemInHand.itemMeta?.persistentDataContainer?.has(autoCrafterKey, PersistentDataType.INTEGER) != true) return

        val state: TileState = e.block.state as TileState
        val container: PersistentDataContainer = state.persistentDataContainer


        container.set(autoCrafterKey, PersistentDataType.INTEGER, 1)

        state.update()
        e.player.sendMessage("Placed AutoCrafter at [${e.block.x}, ${e.block.y}, ${e.block.z}]!") // TODO remove
    }

    @EventHandler
    fun breakAutoCrafter(e: BlockBreakEvent) {
        if(e.block.state !is TileState) {
            return
        }
        val state: TileState = e.block.state as TileState
        val container: PersistentDataContainer = state.persistentDataContainer
        if(!container.has(autoCrafterKey, PersistentDataType.INTEGER)) {
            return
        }

        e.isDropItems = false

        val item: ItemStack = e.block.drops.first()
        val meta: ItemMeta = item.itemMeta ?: return
        meta.persistentDataContainer.set(autoCrafterKey, PersistentDataType.INTEGER, 1)
        item.itemMeta = meta

        e.block.world.dropItem(e.block.location, item)

    }

    @EventHandler
    fun disableHoppers(e: InventoryMoveItemEvent) {
        if(e.source.holder !is Dispenser) return
        val dispenser: Dispenser = e.source.holder as Dispenser
        if(!dispenser.persistentDataContainer.has(autoCrafterKey, PersistentDataType.INTEGER)) return
        e.isCancelled = true
    }

    @EventHandler
    fun runCraftOnDispense(e: BlockDispenseEvent) {
        if(e.block.state !is TileState) {
            return
        }
        val state: TileState = e.block.state as TileState
        val container: PersistentDataContainer = state.persistentDataContainer


        if(!container.has(autoCrafterKey, PersistentDataType.INTEGER)) {
            return
        }
        logger.finest("[AutoCrafting] AutoCrafter found")
        e.isCancelled = true

        val run = Runnable {

            val direction = (e.block.blockData as Directional).facing.direction
            val targetBlock = e.block.getRelative(direction.blockX, direction.blockY, direction.blockZ)
            if(targetBlock.state !is Container) {
                return@Runnable
            }
            logger.finest("[AutoCrafting] TargetContainer found")
            val targetInv = (targetBlock.state as Container).inventory
            val recipe: Recipe = getCraftingRecipeFromInventory(e.block) ?: return@Runnable
            logger.finest("[AutoCrafting] Crafting recipe found: ${recipe.result}")
            val sourceBlock = e.block.getRelative(BlockFace.UP)
            if (sourceBlock.state !is Container) {
                return@Runnable
            }
            logger.finest("[AutoCrafting] SourceContainer found")
            val sourceInv: Inventory = (sourceBlock.state as Container).inventory

            if(!itemFitsInTargetInventory(targetInv, recipe.result) || !checkAndRemoveCraftingMaterialsFromSourceInventory(recipe, sourceInv)) {
                logger.finest("[AutoCrafting] Item doesn't fit or not enough Crafting Materials")
                return@Runnable
            }
            logger.finest("[AutoCrafting] Adding item to inventory")
            targetInv.addItem(recipe.result)
            e.block.state.update()
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
        if(sourceInvCopy.removeItem(*ingredients).isNotEmpty()) return false
        sourceInv.removeItem(*ingredients)
        logger.info("[AutoCrafting] Crafted Item: ${recipe.result}")
        return true
    }
}
