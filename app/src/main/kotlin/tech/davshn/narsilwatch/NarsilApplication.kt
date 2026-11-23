package tech.davshn.narsilwatch

import android.app.Application
import tech.davshn.narsilwatch.sync.Refresh

class NarsilApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Refresh.schedule(this)
    }
}
