package de.eric_scheibler.tactileclock.tasker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.core.content.ContextCompat

import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

import de.eric_scheibler.tactileclock.utils.ApplicationInstance
import de.eric_scheibler.tactileclock.utils.TactileClockService


class VibrateTimeConfigActivity : Activity(), TaskerPluginConfigNoInput {
    override val context get() = applicationContext
    private val taskerHelper by lazy { VibrateTimeHelper(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskerHelper.finishForTasker()
    }
}

class VibrateTimeHelper(config: TaskerPluginConfig<Unit>) : TaskerPluginConfigHelperNoOutputOrInput<VibrateTimeRunner>(config) {
    override val runnerClass: Class<VibrateTimeRunner> get() = VibrateTimeRunner::class.java
}

class VibrateTimeRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        val vibrateTimeIntent = Intent(
                ApplicationInstance.getContext(), TactileClockService::class.java)
        vibrateTimeIntent.setAction(TactileClockService.ACTION_VIBRATE_TIME)
        ContextCompat.startForegroundService( 
                ApplicationInstance.getContext(), vibrateTimeIntent)
        return TaskerPluginResultSucess()
    }
}
