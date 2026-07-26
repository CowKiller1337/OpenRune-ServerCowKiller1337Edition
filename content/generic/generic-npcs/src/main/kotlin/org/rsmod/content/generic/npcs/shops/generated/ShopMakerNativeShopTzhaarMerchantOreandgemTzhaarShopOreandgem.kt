package org.rsmod.content.generic.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ShopMakerNativeShopTzhaarMerchantOreandgemTzhaarShopOreandgem @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3("npc.tzhaar_merchant_oreandgem") { player.openShopMakerNativeShopTzhaarMerchantOreandgemTzhaarShopOreandgem(it.npc) }
    }

    private fun Player.openShopMakerNativeShopTzhaarMerchantOreandgemTzhaarShopOreandgem(npc: Npc) {
        shops.open(
            player = this,
            title = "Tzhaar Ore and Gems",
            shopInv = "inv.tzhaar_shop_oreandgem",
            buyPercentage = 60.0,
            sellPercentage = 150.0,
            changePercentage = 2.0,
            currency = "currency.tokkul",
        )
    }
}
