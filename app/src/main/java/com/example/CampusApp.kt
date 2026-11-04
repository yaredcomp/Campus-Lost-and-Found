package com.example

import android.app.Application
import android.content.Context
import com.example.di.AppModule

class CampusApp : Application() {

    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appModule = AppModule(applicationContext)
    }

    companion object {
        lateinit var instance: CampusApp
            private set

        fun get(context: Context): CampusApp {
            return context.applicationContext as CampusApp
        }
    }
}
