package com.zy.ppmusic;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.zy.ppmusic.mvp.view.MediaActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author y-slience
 * @date 2018/3/21
 */
@RunWith(AndroidJUnit4.class)
public class Test01 {
    @Rule
    public ActivityTestRule<MediaActivity> mRule = new ActivityTestRule<>(MediaActivity.class);

    @Test
    public void justTest() {
        Espresso.onView(ViewMatchers.withId(R.id.control_action_loop_model)).perform(ViewActions.click());
    }
}
