package ru.edenor.uncopyableMaps

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.BoundingBox
import kotlin.random.Random

class UncopyableMaps : JavaPlugin(), Listener {

  private val uncopyableKey: NamespacedKey = NamespacedKey(this, "uncopyable")

  override fun onEnable() {
    server.pluginManager.registerEvents(this, this)
  }

  @EventHandler
  fun onUseHoneycomb(event: PlayerInteractEntityEvent) {
    if (event.hand != EquipmentSlot.HAND) return

    val player = event.player
    val item = player.inventory.itemInMainHand
    if (item.type != Material.HONEYCOMB) return

    val entity = event.rightClicked
    if (entity !is ItemFrame) return

    val frameItem = entity.item
    if (frameItem.type != Material.FILLED_MAP) return

    val meta = frameItem.itemMeta ?: return

    if (meta.persistentDataContainer.has(uncopyableKey, PersistentDataType.STRING)) {
      val author = meta.persistentDataContainer.get(uncopyableKey, PersistentDataType.STRING)
      event.player.sendMessage("Автор карты: $author")
      event.isCancelled = true
      return
    }

    meta.persistentDataContainer.set(uncopyableKey, PersistentDataType.STRING, event.player.name)

    val lore = meta.lore()?.toMutableList() ?: mutableListOf()
    lore.add(Component.text("Автор: ${event.player.name}", NamedTextColor.GRAY))
    lore.add(Component.text("Нельзя копировать", NamedTextColor.RED))
    meta.lore(lore)

    frameItem.itemMeta = meta
    entity.setItem(frameItem)
    event.isCancelled = true

    spawnParticles(entity)

    player.playSound(
      player, Sound.BLOCK_SIGN_WAXED_INTERACT_FAIL, SoundCategory.BLOCKS, 1f, 1f
    )

    if (item.amount > 1) {
      item.amount -= 1
    } else {
      player.inventory.setItemInMainHand(null)
    }
  }

  private fun spawnParticles(entity: ItemFrame) {
    val world = entity.world
    val block = entity.location.block
    val facing = entity.facing
    val offset = 0.01

    val box = when (facing) {
      BlockFace.UP -> BoundingBox(
        block.x.toDouble(), block.y.toDouble(), block.z.toDouble(),
        block.x + 1.0, block.y + 0.5 - offset, block.z + 1.0
      )

      BlockFace.DOWN -> BoundingBox(
        block.x.toDouble(), block.y + 0.5 + offset, block.z.toDouble(),
        block.x + 1.0, block.y + 1.0, block.z + 1.0
      )

      BlockFace.NORTH -> BoundingBox(
        block.x.toDouble(), block.y.toDouble(), block.z + 0.5 + offset,
        block.x + 1.0, block.y + 1.0, block.z + 1.0
      )

      BlockFace.SOUTH -> BoundingBox(
        block.x.toDouble(), block.y.toDouble(), block.z.toDouble(),
        block.x + 1.0, block.y + 1.0, block.z + 0.5 - offset
      )

      BlockFace.EAST -> BoundingBox(
        block.x.toDouble(), block.y.toDouble(), block.z.toDouble(),
        block.x + 0.5 - offset, block.y + 1.0, block.z + 1.0
      )

      BlockFace.WEST -> BoundingBox(
        block.x + 0.5 + offset, block.y.toDouble(), block.z.toDouble(),
        block.x + 1.0, block.y + 1.0, block.z + 1.0
      )

      else -> BoundingBox.of(block)
    }

    repeat(20) {
      val x = Random.nextDouble(box.minX, box.maxX)
      val y = Random.nextDouble(box.minY, box.maxY)
      val z = Random.nextDouble(box.minZ, box.maxZ)

      world.spawnParticle(Particle.WAX_ON, x, y, z, 1, 0.0, 0.0, 0.0, 0.0)
    }
  }


  @EventHandler
  fun onCraft(event: PrepareItemCraftEvent) {
    val result = event.inventory.result ?: return
    if (isUncopyable(result)) {
      event.inventory.result = ItemStack(Material.AIR)
    }
  }

  @EventHandler
  fun onResult(event: PrepareResultEvent) {
    val result = event.result ?: return
    if (isUncopyable(result)) {
      if (event.inventory.type === InventoryType.CARTOGRAPHY) {
        val con = event.inventory.contents[1]
        if (con != null && con.type === Material.GLASS_PANE) {
          return
        }
      }
      if (event.inventory.type === InventoryType.ANVIL) {
        return
      }
      event.result = ItemStack(Material.AIR)
    }
  }

  @EventHandler
  fun onCrafterCraft(event: CrafterCraftEvent) {
    val result = event.result
    if (isUncopyable(result)) {
      event.isCancelled = true
    }
  }

  private fun isUncopyable(item: ItemStack?): Boolean {
    if (item == null) return false
    val meta = item.itemMeta ?: return false
    return meta.persistentDataContainer.has(uncopyableKey, PersistentDataType.STRING)
  }
}
