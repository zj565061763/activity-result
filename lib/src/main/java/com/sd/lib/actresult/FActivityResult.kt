package com.sd.lib.actresult

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class FActivityResult(activity: Activity) {
    private val _activity: ComponentActivity
    private val _launcherHolder = mutableMapOf<String, ActivityResultLauncher<*>>()

    private val _uuid = UUID.randomUUID().toString()
    private val _nextLocalRequestCode = AtomicInteger()

    fun startActivityForResult(callback: ActivityResultCallback<ActivityResult>): ActivityResultLauncher<Intent> {
        return register(ActivityResultContracts.StartActivityForResult(), callback)
    }

    @Synchronized
    fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        if (_activity.isFinishing) {
            return emptyActivityResultLauncher(contract)
        }

        val key = generateKey()
        val internalCallback = ActivityResultCallback<O> {
            synchronized(this@FActivityResult) {
                _launcherHolder.remove(key)
            }
            callback.onActivityResult(it)
        }

        return _activity.activityResultRegistry.register(key, contract, internalCallback).also {
            _launcherHolder[key] = it
        }
    }

    /**
     * 取消注册
     */
    @Synchronized
    private fun unregister() {
        _launcherHolder.values.forEach {
            it.unregister()
        }
        _launcherHolder.clear()
    }

    private fun generateKey(): String {
        return _uuid + "#" + _nextLocalRequestCode.getAndIncrement()
    }

    private val _lifecycleEventObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (Lifecycle.Event.ON_DESTROY == event) {
                source.lifecycle.removeObserver(this)
                unregister()
            }
        }
    }

    init {
        require(activity is ComponentActivity) { "activity should be instance of ${ComponentActivity::class.java}" }
        _activity = activity
        _activity.lifecycle.run {
            if (Lifecycle.State.DESTROYED != currentState) {
                addObserver(_lifecycleEventObserver)
            }
        }
    }
}

private fun <I, O> emptyActivityResultLauncher(contract: ActivityResultContract<I, O>): ActivityResultLauncher<I> {
    return object : ActivityResultLauncher<I>() {
        override fun launch(input: I, options: ActivityOptionsCompat?) {
        }

        override fun unregister() {
        }

        override fun getContract(): ActivityResultContract<I, *> {
            return contract
        }
    }
}