package com.sueldo.app

import android.app.Application
import com.sueldo.app.data.AppDatabase
import com.sueldo.app.data.Repository

class SueldoApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = Repository(
            configDao = db.configDao(),
            gastoDao = db.gastoImportanteDao(),
            diaDao = db.diaDao()
        )
    }
}
