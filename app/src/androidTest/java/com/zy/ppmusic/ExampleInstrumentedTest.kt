package com.zy.ppmusic

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.zy.ppmusic.mvp.view.MediaActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Rule
    var mActivityRule = ActivityTestRule(MediaActivity::class.java)

    @Test
    fun useAppContext() {
        onView(withId(R.id.control_action_loop_model)).perform(click())
    }
}
