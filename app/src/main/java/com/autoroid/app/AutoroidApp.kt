package com.autoroid.app

import android.app.Application
import com.autoroid.app.core.native.NativeEngine
import com.autoroid.app.core.privilege.PrivilegeManager
import com.autoroid.app.feature.accessibility.AccessibilityController
import com.autoroid.app.feature.telephony.TelephonyController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoroidApp : Application() {

    lateinit var privilegeManager: PrivilegeManager
        private set

    lateinit var accessibilityController: AccessibilityController
        private set

    lateinit var telephonyController: TelephonyController
        private set

    lateinit var workflowRepository: com.autoroid.app.feature.workflow.repository.WorkflowRepository
        private set

    lateinit var workflowRunner: com.autoroid.app.feature.workflow.runner.WorkflowRunner
        private set

    lateinit var pointerLocationHelper: com.autoroid.app.feature.workflow.runner.PointerLocationHelper
        private set

    lateinit var updateManager: com.autoroid.app.feature.update.UpdateManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        privilegeManager = PrivilegeManager(this)
        accessibilityController = AccessibilityController(this, privilegeManager)
        telephonyController = TelephonyController(this, privilegeManager)
        workflowRepository = com.autoroid.app.feature.workflow.repository.WorkflowRepository(this)
        workflowRunner = com.autoroid.app.feature.workflow.runner.WorkflowRunner(this, privilegeManager)
        pointerLocationHelper = com.autoroid.app.feature.workflow.runner.PointerLocationHelper(privilegeManager)
        updateManager = com.autoroid.app.feature.update.UpdateManager(this, privilegeManager)

        // Initialize state asynchronously
        applicationScope.launch {
            privilegeManager.refresh()
            accessibilityController.refreshState()
            telephonyController.refreshSimState()
            workflowRepository.initialize()
            pointerLocationHelper.refreshState()
            updateManager.checkForUpdates()
        }
    }

    companion object {
        lateinit var instance: AutoroidApp
            private set
    }
}
