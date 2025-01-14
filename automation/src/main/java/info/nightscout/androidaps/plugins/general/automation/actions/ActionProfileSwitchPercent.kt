package info.nightscout.androidaps.plugins.general.automation.actions

import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.automation.R
import info.nightscout.androidaps.data.PumpEnactResult
import info.nightscout.androidaps.database.entities.UserEntry
import info.nightscout.androidaps.database.entities.UserEntry.Sources
import info.nightscout.androidaps.database.entities.ValueWithUnit
import info.nightscout.androidaps.interfaces.ProfileFunction
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.logging.UserEntryLogger
import info.nightscout.androidaps.plugins.general.automation.elements.Comparator
import info.nightscout.androidaps.plugins.general.automation.elements.InputDuration
import info.nightscout.androidaps.plugins.general.automation.elements.InputPercent
import info.nightscout.androidaps.plugins.general.automation.elements.LabelWithElement
import info.nightscout.androidaps.plugins.general.automation.elements.LayoutBuilder
import info.nightscout.androidaps.plugins.general.automation.triggers.TriggerProfilePercent
import info.nightscout.androidaps.queue.Callback
import info.nightscout.androidaps.utils.JsonHelper
import info.nightscout.androidaps.utils.resources.ResourceHelper
import org.json.JSONObject
import javax.inject.Inject

class ActionProfileSwitchPercent(injector: HasAndroidInjector) : Action(injector) {

    @Inject lateinit var resourceHelper: ResourceHelper
    @Inject lateinit var profileFunction: ProfileFunction
    @Inject lateinit var uel: UserEntryLogger

    var pct = InputPercent()
    var duration = InputDuration(30, InputDuration.TimeUnit.MINUTES)

    override fun friendlyName(): Int = R.string.profilepercentage
    override fun shortDescription(): String =
        if (duration.value == 0) resourceHelper.gs(R.string.startprofileforever, pct.value.toInt())
        else resourceHelper.gs(R.string.startprofile, pct.value.toInt(), duration.value)

    @DrawableRes override fun icon(): Int = R.drawable.ic_actions_profileswitch

    init {
        precondition = TriggerProfilePercent(injector, 100.0, Comparator.Compare.IS_EQUAL)
    }

    override fun doAction(callback: Callback) {
        if (profileFunction.createProfileSwitch(duration.value, pct.value.toInt(), 0)) {
            uel.log(
                UserEntry.Action.PROFILE_SWITCH,
                Sources.Automation,
                title + ": " + resourceHelper.gs(R.string.startprofile, pct.value.toInt(), duration.value),
                ValueWithUnit.Percent(pct.value.toInt()),
                ValueWithUnit.Minute(duration.value)
            )
        } else {
            aapsLogger.error(LTag.AUTOMATION, "Final profile not valid")
        }
        callback.result(PumpEnactResult(injector).success(true).comment(R.string.ok))?.run()
    }

    override fun generateDialog(root: LinearLayout) {
        LayoutBuilder()
            .add(LabelWithElement(resourceHelper, resourceHelper.gs(R.string.percent_u), "", pct))
            .add(LabelWithElement(resourceHelper, resourceHelper.gs(R.string.duration_min_label), "", duration))
            .build(root)
    }

    override fun hasDialog(): Boolean = true

    override fun toJSON(): String {
        val data = JSONObject()
            .put("percentage", pct.value)
            .put("durationInMinutes", duration.value)
        return JSONObject()
            .put("type", this.javaClass.name)
            .put("data", data)
            .toString()
    }

    override fun fromJSON(data: String): Action {
        val o = JSONObject(data)
        pct.value = JsonHelper.safeGetDouble(o, "percentage")
        duration.value = JsonHelper.safeGetInt(o, "durationInMinutes")
        return this
    }

    override fun isValid(): Boolean =
        pct.value >= InputPercent.MIN &&
            pct.value <= InputPercent.MAX &&
            duration.value > 0
}