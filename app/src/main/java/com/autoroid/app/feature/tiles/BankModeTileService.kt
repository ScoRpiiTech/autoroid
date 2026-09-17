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

class BankModeTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val accessibilityController by lazy {
        AutoroidApp.instance.accessibilityController
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            accessibilityController.toggleBankMode()
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = accessibilityController.isBankModeActive.value

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_bank_mode)
        tile.subtitle = if (isActive) "Protected (Clean)" else "Normal (Active)"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_shield)
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
