package ru.edenor.uncopyableMaps

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class UncopyableMaps : JavaPlugin(), Listener {

  private lateinit var uncopyableKey: NamespacedKey

  override fun onEnable() {
    uncopyableKey = NamespacedKey(this, "uncopyable")
    server.pluginManager.registerEvents(this, this)
  }

  @EventHandler
  fun onUseHoneycomb(event: PlayerInteractEntityEvent) {
    if (event.hand != EquipmentSlot.HAND) return

    val player = event.player
    val item = player.inventory.itemInMainHand ?: return
    if (item.type != Material.HONEYCOMB) return

    val entity = event.rightClicked
    if (entity !is ItemFrame) return

    val frameItem = entity.item ?: return
    if (frameItem.type != Material.FILLED_MAP) return

    val meta = frameItem.itemMeta ?: return

    if (meta.persistentDataContainer.has(uncopyableKey, PersistentDataType.BYTE)) {
      player.sendRichMessage("<red>Эта карта уже защищена от копирования!")
      return
    }

    meta.persistentDataContainer.set(uncopyableKey, PersistentDataType.BYTE, 1.toByte())

    val lore = meta.lore()?.toMutableList() ?: mutableListOf()
    lore.add(Component.text("Нельзя копировать", NamedTextColor.RED))
    meta.lore(lore)

    frameItem.itemMeta = meta
    entity.setItem(frameItem)

    player.sendRichMessage("<green>Эта карта теперь защищена от копирования!")

    if (item.amount > 1) {
      item.amount -= 1
    } else {
      player.inventory.setItemInMainHand(null)
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
      event.result = ItemStack(Material.AIR)
    }
  }

  @EventHandler
  fun onSmithing(event: PrepareSmithingEvent) {
    val result = event.result ?: return
    if (isUncopyable(result)) {
      event.result = ItemStack(Material.AIR)
    }
  }

  @EventHandler
  fun onCrafterCraft(event: CrafterCraftEvent) {
    val result = event.result
    if (isUncopyable(result)) {
      event.isCancelled = true
      logger.info("Автокрафтер попытался скопировать защищённую карту у блока: ${event.block.location}")
    }
  }


  private fun isUncopyable(item: ItemStack?): Boolean {
    if (item == null) return false
    val meta = item.itemMeta ?: return false
    return meta.persistentDataContainer.has(uncopyableKey, PersistentDataType.BYTE)
  }
}
