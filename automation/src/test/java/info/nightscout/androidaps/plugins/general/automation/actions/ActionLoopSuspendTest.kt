package info.nightscout.androidaps.plugins.general.automation.actions

import info.nightscout.androidaps.automation.R
import info.nightscout.androidaps.plugins.general.automation.elements.InputDuration
import info.nightscout.androidaps.queue.Callback
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class ActionLoopSuspendTest : ActionsTestBase() {

    lateinit var sut: ActionLoopSuspend

    @Before
    fun setup() {

        `when`(resourceHelper.gs(R.string.suspendloop)).thenReturn("Suspend loop")
        `when`(resourceHelper.gs(ArgumentMatchers.eq(R.string.suspendloopforXmin), ArgumentMatchers.anyInt())).thenReturn("Suspend loop for %d min")
        `when`(resourceHelper.gs(R.string.alreadysuspended)).thenReturn("Already suspended")

        sut = ActionLoopSuspend(injector)
    }

    @Test fun friendlyNameTest() {
        Assert.assertEquals(R.string.suspendloop, sut.friendlyName())
    }

    @Test fun shortDescriptionTest() {
        sut.minutes = InputDuration(30, InputDuration.TimeUnit.MINUTES)
        Assert.assertEquals("Suspend loop for %d min", sut.shortDescription())
    }

    @Test fun iconTest() {
        Assert.assertEquals(R.drawable.ic_pause_circle_outline_24dp, sut.icon())
    }

    @Test fun doActionTest() {
        `when`(loopPlugin.isSuspended).thenReturn(false)
        sut.minutes = InputDuration(30, InputDuration.TimeUnit.MINUTES)
        sut.doAction(object : Callback() {
            override fun run() {}
        })
        Mockito.verify(loopPlugin, Mockito.times(1)).suspendLoop(30)

        // another call should keep it suspended, no more invocations
        `when`(loopPlugin.isSuspended).thenReturn(true)
        sut.doAction(object : Callback() {
            override fun run() {}
        })
        Mockito.verify(loopPlugin, Mockito.times(1)).suspendLoop(30)
    }

    @Test fun applyTest() {
        val a = ActionLoopSuspend(injector)
        a.minutes = InputDuration(20, InputDuration.TimeUnit.MINUTES)
        val b = ActionLoopSuspend(injector)
        b.apply(a)
        Assert.assertEquals(20, b.minutes.getMinutes().toLong())
    }

    @Test fun hasDialogTest() {
        val a = ActionLoopSuspend(injector)
        Assert.assertTrue(a.hasDialog())
    }
}