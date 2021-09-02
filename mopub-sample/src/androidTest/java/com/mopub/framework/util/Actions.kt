// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.framework.util

import android.view.View
import android.webkit.WebView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import java.lang.Exception
import java.util.concurrent.TimeoutException

object Actions {
    private const val TIMEOUT = 10000L // milliseconds
    private const val REPEAT = 10

    @JvmStatic
    fun loopMainThreadUntilIdle() {
        onView(isRoot()).perform(loopMainThreadAction())
    }

    @JvmStatic
    @JvmOverloads
    fun loopMainThreadAtLeast(millis: Long = TIMEOUT) {
        onView(isRoot()).perform(loopMainThreadAction(millis))
    }

    @JvmStatic
    @JvmOverloads
    fun findView(viewMatcher: Matcher<View>, timeoutMs: Long = TIMEOUT): ViewInteraction {
        var exception: Throwable? = null
        for (i in 1..REPEAT) {
            try {
                onView(isRoot()).perform(findAction(viewMatcher, timeoutMs / REPEAT))
                return onView(viewMatcher)
            } catch (e: Throwable) {
                exception = e
            }
        }
        if (exception != null) {
            throw exception
        }

        return onView(viewMatcher)
    }

    @JvmStatic
    @JvmOverloads
    fun findViewSafe(viewMatcher: Matcher<View>, timeoutMs: Long = 1000): Boolean {
        try {
            findView(viewMatcher, timeoutMs)
        } catch (ex: Exception) {
            return false
        }
        return true
    }

    @JvmStatic
    fun clickElement(viewMatcher: Matcher<View>) {
        var exception: Throwable? = null
        for (i in 1..REPEAT) {
            try {
                loopMainThreadUntilIdle()
                onView((isRoot())).perform(clickAction(viewMatcher, TIMEOUT / REPEAT))
                return
            } catch (e: Throwable) {
                exception = e
            }
        }
        if (exception != null) {
            throw exception
        }
    }

    @JvmStatic
    fun assertWebView(url: String) {
        loopMainThreadAtLeast(1000)
        onView(isRoot()).perform(
            object : ViewAction {
                override fun getConstraints() = isRoot()

                override fun getDescription() =
                    "wait for a WebView with URL <$url> for $TIMEOUT millis."


                override fun perform(uiController: UiController, view: View) {
                    uiController.loopMainThreadUntilIdle()
                    val endTime = System.currentTimeMillis() + TIMEOUT
                    do {
                        for (child in TreeIterables.breadthFirstViewTraversal(view)) {
                            if (child is WebView) {
                                if (child.url.toString().startsWith(url)) {
                                    child.stopLoading()
                                    uiController.loopMainThreadUntilIdle()
                                    return
                                }
                            }
                        }
                        uiController.loopMainThreadForAtLeast(100)
                    } while (System.currentTimeMillis() < endTime)

                    throw PerformException.Builder()
                        .withActionDescription(this.description)
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(TimeoutException())
                        .build()
                }
            }
        )
    }

    private fun loopMainThreadAction(millis: Long = 10): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "Wait until UI thread is free for at least $millis millisecond."
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()
                if (millis > 0) {
                    uiController.loopMainThreadForAtLeast(millis)
                }
            }
        }
    }

    private fun findAction(viewMatcher: Matcher<View>, millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "Wait for a specific view with matcher <$viewMatcher> during $millis millis."
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()
                val endTime = System.currentTimeMillis() + millis
                do {
                    for (child in TreeIterables.breadthFirstViewTraversal(view)) {
                        if (viewMatcher.matches(child)) {
                            uiController.loopMainThreadUntilIdle()
                            return
                        }
                    }
                    uiController.loopMainThreadForAtLeast(100)
                } while (System.currentTimeMillis() < endTime)

                throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(TimeoutException())
                    .build()
            }
        }
    }

    private fun clickAction(viewMatcher: Matcher<View>, millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "Wait for a specific view with matcher <$viewMatcher> during $millis millis."
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()
                val endTime = System.currentTimeMillis() + millis
                do {
                    for (child in TreeIterables.breadthFirstViewTraversal(view)) {
                        if (viewMatcher.matches(child) && child.isEnabled) {
                            if (child.performClick()) {
                                uiController.loopMainThreadUntilIdle()
                                return
                            }
                        }
                    }
                    uiController.loopMainThreadForAtLeast(100)
                } while (System.currentTimeMillis() < endTime)

                throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(TimeoutException())
                    .build()
            }
        }
    }
}
