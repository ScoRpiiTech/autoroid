package com.autoroid.app.feature.tiles

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.autoroid.app.AutoroidApp
import com.autoroid.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SimSwitchTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val telephonyController by lazy {
        AutoroidApp.instance.telephonyController
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            telephonyController.refreshSimState()
            updateTileState()
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            telephonyController.toggleAlternateSim()
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val activeSub = telephonyController.activeDataSubId.value
        val slots = telephonyController.simSlots.value
        val activeSlot = slots.firstOrNull { it.subscriptionId == activeSub }

        tile.state = Tile.STATE_ACTIVE
        tile.label = getString(R.string.tile_sim_switch)
        tile.subtitle = activeSlot?.displayName ?: if (activeSub != null) "Sub $activeSub" else "Auto"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_sim_card)
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
