package de.daaren.autocrafting

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

object Crafter : Listener {
    @EventHandler
    fun onPlace(e : HangingPlaceEvent) {
        if (e.entity.type != EntityType.ITEM_FRAME && e.entity.type != EntityType.GLOW_ITEM_FRAME) {
            return
        }
        Logger.info("Test: ${e.entity.location.x}, ${e.entity.location.y}, ${e.entity.location.z}")
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
        // val i: ItemFrame = e.entity as ItemFrame TODO mark itemframe with glowstone dust and tag, disable dropping of glowstone dust
        e.player!!.sendMessage("Created AutoCrafter at [${e.block.x}, ${e.block.y}, ${e.block.z}]!")
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

        // if target inventory is not full
        val run = Runnable {
            synchronized(this) {
                val targetBlock = e.block.getRelative(BlockFace.DOWN)
                if(targetBlock.state is Container) { // or item can stack
                    val craftedItem: ItemStack? = craftItemFromInventory(e.block)
                    Logger.info("Crafted Item: $craftedItem")
                    (targetBlock.state as Container).inventory.addItem(craftedItem)
                }


            }
        }
        Bukkit.getScheduler().runTaskLater(AutoCrafting.instance as Plugin, run, 1) // Wait until dispensed item is placed back in next tick

    }

    private fun craftItemFromInventory(dispenserBlock: Block): ItemStack? {
        val dispenser: Dispenser = dispenserBlock.state as Dispenser
        val inv = dispenser.inventory.storageContents
        val server = AutoCrafting.instance!!.server

        val dLoc = dispenserBlock.location
        val sourceBlock = dispenserBlock.getRelative(BlockFace.UP)
        if (sourceBlock.state !is Container) {
            return null
        }
        val sourceContainer: Container = sourceBlock.state as Container
        val sourceInv: Array<ItemStack?> = sourceContainer.inventory.storageContents
        val recipeItemsMap: Map<Material, Int> = mapFromInventory(inv, true)

        val recipe: Recipe? = server.getCraftingRecipe(inv, dLoc.world!!)
        if (recipe != null && sourceInventoryHasCraftingMaterials(recipeItemsMap, mapFromInventory(sourceInv, false))) {
            removeItemsFromSourceInventory(recipeItemsMap, sourceInv)
            return recipe.result
        }
        return null
    }

    private fun mapFromInventory(inv: Array<ItemStack?>, countOnlyOnce: Boolean): Map<Material, Int> {
        val map = mutableMapOf<Material, Int>()
        for(item: ItemStack? in inv) {
            if(item?.type != null) {
                map[item.type] = (map[item.type] ?: 0) + (if (countOnlyOnce) 1 else item.amount)
            }
        }
        return map
    }


    private fun sourceInventoryHasCraftingMaterials(recipeItemsMap: Map<Material, Int>, sourceInventoryMap: Map<Material, Int>): Boolean { // TODO replace with Inventory.contains
        for ((item, amount) in recipeItemsMap) {
            if((sourceInventoryMap[item] ?: 0) < amount) return false
        }
        return true
    }

    private fun removeItemsFromSourceInventory(recipeItemsMap: Map<Material, Int>, inv: Array<ItemStack?>) { // TODO replace with Inventory.remove
        for ((item, amount) in recipeItemsMap) {
            var remaining: Int = amount
            for (slot: ItemStack? in inv.filter {it?.type == item}) {
                if(slot!!.amount >= remaining) {
                    slot.amount -= remaining
                    break
                }
                remaining -= slot.amount
                slot.amount = 0
            }
        }
    }




















//    @EventHandler TODO remove key when breaking ItemFrame
//    fun onBreak(e : HangingBreakEvent) {
//        if (e.entity.type != EntityType.ITEM_FRAME && e.entity.type != EntityType.GLOW_ITEM_FRAME) {
//            return
//        }
//        Logger.info("Test: ${e.entity.location.x}, ${e.entity.location.y}, ${e.entity.location.z}")
//        if (e.block.type != Material.DISPENSER) {
//            return
//        }
//        if(e.block.state !is TileState)
//            return
//
//        if(e.player == null)
//            return
//        val state: TileState = e.block.state as TileState
//        val container: PersistentDataContainer = state.persistentDataContainer
//        val key = NamespacedKey(AutoCrafting.instance as Plugin, "auto-crafter")
//
//        container.set(key, PersistentDataType.INTEGER, 1)
//
//        state.update()
//
//        e.player!!.sendMessage("Created AutoCrafter at [${e.block.x}, ${e.block.y}, ${e.block.z}]!")
//    }
}