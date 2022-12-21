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
    private val _uuid = UUID.randomUUID().toString()
    private val _nextLocalRequestCode = AtomicInteger()
    private val _mapLauncher = mutableMapOf<String, ActivityResultLauncher<*>>()

    fun registerResult(callback: ActivityResultCallback<ActivityResult>): ActivityResultLauncher<Intent> {
        return register(ActivityResultContracts.StartActivityForResult(), callback)
    }

    fun registerPermission(callback: ActivityResultCallback<Boolean>): ActivityResultLauncher<String> {
        return register(ActivityResultContracts.RequestPermission(), callback)
    }

    fun registerPermissions(callback: ActivityResultCallback<Map<String, Boolean>>): ActivityResultLauncher<Array<String>> {
        return register(ActivityResultContracts.RequestMultiplePermissions(), callback)
    }

    @Synchronized
    fun <I, O> register(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        if (Lifecycle.State.DESTROYED == _activity.lifecycle.currentState) {
            return emptyActivityResultLauncher(contract)
        }

        val key = generateKey()
        val internalCallback = ActivityResultCallback<O> {
            synchronized(this@FActivityResult) {
                _mapLauncher.remove(key)
            }
            callback.onActivityResult(it)
        }
        return _activity.activityResultRegistry.register(key, contract, internalCallback).also {
            _mapLauncher[key] = it
        }
    }

    /**
     * 取消注册
     */
    @Synchronized
    private fun unregisterLauncher() {
        _mapLauncher.values.forEach {
            it.unregister()
        }
        _mapLauncher.clear()
    }

    private fun generateKey(): String {
        return _uuid + "#" + _nextLocalRequestCode.getAndIncrement()
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

    init {
        require(activity is ComponentActivity) { "activity must be instance of ${ComponentActivity::class.java}" }
        _activity = activity

        val lifecycle = _activity.lifecycle
        if (Lifecycle.State.DESTROYED != lifecycle.currentState) {
            lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (Lifecycle.Event.ON_DESTROY == event) {
                        lifecycle.removeObserver(this)
                        unregisterLauncher()
                    }
                }
            })
        }
    }
}