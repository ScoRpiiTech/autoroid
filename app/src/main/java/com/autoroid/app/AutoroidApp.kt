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

    lateinit var simScheduleRepository: com.autoroid.app.feature.telephony.schedule.repository.SimScheduleRepository
        private set

    lateinit var simScheduleManager: com.autoroid.app.feature.telephony.schedule.SimScheduleManager
        private set

    lateinit var imsRepository: com.autoroid.app.feature.telephony.ims.repository.ImsRepository
        private set

    lateinit var imsController: com.autoroid.app.feature.telephony.ims.ImsController
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this

        privilegeManager = PrivilegeManager(this)
        accessibilityController = AccessibilityController(this, privilegeManager)
        telephonyController = TelephonyController(this, privilegeManager)
        simScheduleRepository = com.autoroid.app.feature.telephony.schedule.repository.SimScheduleRepository(this)
        simScheduleManager = com.autoroid.app.feature.telephony.schedule.SimScheduleManager(
            this,
            simScheduleRepository,
            telephonyController,
            privilegeManager
        )
        imsRepository = com.autoroid.app.feature.telephony.ims.repository.ImsRepository(this)
        imsController = com.autoroid.app.feature.telephony.ims.ImsController(
            this,
            imsRepository,
            telephonyController,
            privilegeManager
        )
        workflowRepository = com.autoroid.app.feature.workflow.repository.WorkflowRepository(this)
        workflowRunner = com.autoroid.app.feature.workflow.runner.WorkflowRunner(this, privilegeManager)
        pointerLocationHelper = com.autoroid.app.feature.workflow.runner.PointerLocationHelper(privilegeManager)
        updateManager = com.autoroid.app.feature.update.UpdateManager(this, privilegeManager)

        // Initialize state asynchronously with robust error boundaries
        applicationScope.launch {
            try {
                privilegeManager.refresh()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "Privilege refresh note: ${t.message}")
            }
            try {
                accessibilityController.refreshState()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "Accessibility refresh note: ${t.message}")
            }
            try {
                telephonyController.refreshSimState()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "Telephony refresh note: ${t.message}")
            }
            try {
                simScheduleManager.onBoot()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "SimScheduleManager onBoot note: ${t.message}")
            }
            try {
                imsController.onBoot()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "ImsController onBoot note: ${t.message}")
            }
            try {
                workflowRepository.initialize()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "WorkflowRepository initialize note: ${t.message}")
            }
            try {
                pointerLocationHelper.refreshState()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "PointerLocationHelper refresh note: ${t.message}")
            }
            try {
                updateManager.checkForUpdates()
            } catch (t: Throwable) {
                android.util.Log.w("AutoroidApp", "UpdateManager checkForUpdates note: ${t.message}")
            }
        }
    }

    companion object {
        lateinit var instance: AutoroidApp
            private set
    }
}
