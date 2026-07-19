package org.rsmod.content.skills.fishing

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FishBarrelScript : PluginScript() {
    override fun ScriptContext.startup() {
        FishBarrel.items.forEach { barrel ->
            onOpHeld1(barrel) { fillBarrel() }
            onOpHeld3(barrel) { checkBarrel() }
            onOpHeld4(barrel) { emptyBarrel() }
        }
        onOpHeld2(FishBarrel.closed) { openBarrel() }
        onOpHeld2(FishBarrel.open) { closeBarrel() }
        onOpHeld2(FishBarrel.sackClosed) { openBarrel() }
        onOpHeld2(FishBarrel.sackOpen) { closeBarrel() }
    }

    private fun ProtectedAccess.fillBarrel() {
        val barrel = FishBarrel.inventory(player)
        if (barrel.isFull()) {
            mes("Your fish barrel is full.")
            return
        }

        var moved = 0
        for (slot in inv.indices) {
            if (barrel.isFull()) {
                break
            }
            val obj = inv[slot] ?: continue
            val internal = FishBarrel.itemKey(obj.id) ?: continue
            if (!FishBarrel.canStore(internal)) {
                continue
            }
            moved += invMoveFromSlot(inv, barrel, slot, obj.count, strict = false).completed()
        }

        if (moved == 0) {
            mes("You don't have any fish that can go in the barrel.")
        } else {
            mes("You fill the fish barrel.")
        }
    }

    private fun ProtectedAccess.emptyBarrel() {
        val barrel = FishBarrel.inventory(player)
        if (barrel.isEmpty()) {
            mes("Your fish barrel is empty.")
            return
        }
        val moved = invMoveInv(barrel, inv, intoCapacity = inv.size).completed()
        if (moved == 0) {
            mes("You don't have any free space in your inventory.")
        } else {
            mes("You empty the fish barrel.")
        }
    }

    private fun ProtectedAccess.checkBarrel() {
        val count = FishBarrel.inventory(player).occupiedSpace()
        val text =
            if (count == 0) {
                "Your fish barrel is empty."
            } else {
                "Your fish barrel contains $count of ${FishBarrel.capacity} fish."
            }
        mes(text)
    }

    private fun ProtectedAccess.openBarrel() {
        if (invReplace(inv, FishBarrel.closed, 1, FishBarrel.open).failure) {
            invReplace(inv, FishBarrel.sackClosed, 1, FishBarrel.sackOpen)
        }
    }

    private fun ProtectedAccess.closeBarrel() {
        if (invReplace(inv, FishBarrel.open, 1, FishBarrel.closed).failure) {
            invReplace(inv, FishBarrel.sackOpen, 1, FishBarrel.sackClosed)
        }
    }
}

internal object FishBarrel {
    const val closed = "obj.fish_barrel_closed"
    const val open = "obj.fish_barrel_open"
    const val sackClosed = "obj.fish_sack_barrel_closed"
    const val sackOpen = "obj.fish_sack_barrel_open"
    const val capacity = 28

    val items = listOf(closed, open, sackClosed, sackOpen)

    fun isOpen(player: Player): Boolean = open in player.inv || sackOpen in player.inv

    fun inventory(player: Player): Inventory = player.invMap.getOrPut("inv.fish_barrel")

    fun canStore(item: String): Boolean = item in FishingData.fishBarrelEligible

    fun storeCatch(player: Player, item: String, count: Int): Int {
        if (!isOpen(player) || !canStore(item) || count <= 0) {
            return 0
        }
        val barrel = inventory(player)
        var stored = 0
        repeat(count) {
            if (barrel.isFull()) {
                return stored
            }
            val result = player.invAdd(barrel, item, ignoreVirtualStorage = true)
            if (result.success) {
                stored++
            }
        }
        return stored
    }

    fun itemKey(id: Int): String? = ServerCacheManager.getItem(id)?.let { type ->
        runCatching { dev.openrune.rscm.RSCM.getReverseMapping(RSCMType.OBJ, type.id) }.getOrNull()
    }
}
