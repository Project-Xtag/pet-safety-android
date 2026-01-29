package com.petsafety.app

import android.app.Application

/**
 * Simple test Application class that doesn't use Hilt.
 * Used by Robolectric tests to avoid WorkManager initialization issues.
 */
class TestApplication : Application()
