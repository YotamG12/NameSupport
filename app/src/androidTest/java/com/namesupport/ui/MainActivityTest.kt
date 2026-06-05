package com.namesupport.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namesupport.MainActivity
import com.namesupport.R
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun scanButtonIsDisplayedOnLaunch() {
        onView(withId(R.id.btnScan)).check(matches(isDisplayed()))
    }

    @Test
    fun applyButtonIsDisabledBeforeScan() {
        onView(withId(R.id.btnApply)).check(matches(not(isEnabled())))
    }

    @Test
    fun emptyStateIsShownOnLaunch() {
        onView(withId(R.id.tvEmpty)).check(matches(isDisplayed()))
    }

    @Test
    fun scanButtonClickTriggersPermissionOrScan() {
        // Clicking Scan should either launch a permission dialog or start loading.
        // We just verify the button responds without crashing.
        onView(withId(R.id.btnScan)).perform(click())
        onView(withId(R.id.btnScan)).check(matches(isDisplayed()))
    }
}
