package info.nightscout.androidaps.plugins.configBuilder

import info.nightscout.androidaps.core.R
import info.nightscout.androidaps.interfaces.ActivePlugin
import info.nightscout.androidaps.interfaces.ConfigBuilder
import info.nightscout.androidaps.interfaces.Insulin
import info.nightscout.androidaps.interfaces.PluginType
import info.nightscout.androidaps.interfaces.Sensitivity
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType
import info.nightscout.androidaps.utils.JsonHelper
import info.nightscout.androidaps.utils.sharedPreferences.SP
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunningConfiguration @Inject constructor(
    private val activePlugin: ActivePlugin,
    private val configBuilder: ConfigBuilder,
    private val sp: SP,
    private val aapsLogger: AAPSLogger
) {

    private var counter = 0
    private val every = 20 // Send only every 20 device status to save traffic

    // called in AAPS mode only
    fun configuration(): JSONObject {
        val json = JSONObject()
        if (counter++ % every == 0)
            try {
                val insulinInterface = activePlugin.activeInsulin
                val sensitivityInterface = activePlugin.activeSensitivity
                val pumpInterface = activePlugin.activePump
                val overviewInterface = activePlugin.activeOverview
                val safetyInterface = activePlugin.activeSafety

                json.put("insulin", insulinInterface.id.value)
                json.put("insulinConfiguration", insulinInterface.configuration())
                json.put("sensitivity", sensitivityInterface.id.value)
                json.put("sensitivityConfiguration", sensitivityInterface.configuration())
                json.put("overviewConfiguration", overviewInterface.configuration())
                json.put("safetyConfiguration", safetyInterface.configuration())
                json.put("pump", pumpInterface.model().description)
            } catch (e: JSONException) {
                aapsLogger.error("Unhandled exception", e)
            }
        return json
    }

    // called in NSClient mode only
    fun apply(configuration: JSONObject) {
        if (configuration.has("insulin")) {
            val insulin = Insulin.InsulinType.fromInt(JsonHelper.safeGetInt(configuration, "insulin", Insulin.InsulinType.UNKNOWN.value))
            for (p in activePlugin.getSpecificPluginsListByInterface(Insulin::class.java)) {
                val insulinPlugin = p as Insulin
                if (insulinPlugin.id == insulin) {
                    if (!p.isEnabled()) {
                        aapsLogger.debug(LTag.CORE, "Changing insulin plugin to ${insulin.name}")
                        configBuilder.performPluginSwitch(p, true, PluginType.INSULIN)
                    }
                    insulinPlugin.applyConfiguration(configuration.getJSONObject("insulinConfiguration"))
                }
            }
        }

        if (configuration.has("sensitivity")) {
            val sensitivity = Sensitivity.SensitivityType.fromInt(JsonHelper.safeGetInt(configuration, "sensitivity", Sensitivity.SensitivityType.UNKNOWN.value))
            for (p in activePlugin.getSpecificPluginsListByInterface(Sensitivity::class.java)) {
                val sensitivityPlugin = p as Sensitivity
                if (sensitivityPlugin.id == sensitivity) {
                    if (!p.isEnabled()) {
                        aapsLogger.debug(LTag.CORE, "Changing sensitivity plugin to ${sensitivity.name}")
                        configBuilder.performPluginSwitch(p, true, PluginType.SENSITIVITY)
                    }
                    sensitivityPlugin.applyConfiguration(configuration.getJSONObject("sensitivityConfiguration"))
                }
            }
        }

        if (configuration.has("pump")) {
            val pumpType = JsonHelper.safeGetString(configuration, "pump", PumpType.GENERIC_AAPS.description)
            sp.putString(R.string.key_virtualpump_type, pumpType)
            activePlugin.activePump.pumpDescription.fillFor(PumpType.getByDescription(pumpType))
            aapsLogger.debug(LTag.CORE, "Changing pump type to $pumpType")
        }

        if (configuration.has("overviewConfiguration"))
            activePlugin.activeOverview.applyConfiguration(configuration.getJSONObject("overviewConfiguration"))

        if (configuration.has("safetyConfiguration"))
            activePlugin.activeSafety.applyConfiguration(configuration.getJSONObject("safetyConfiguration"))
    }
}