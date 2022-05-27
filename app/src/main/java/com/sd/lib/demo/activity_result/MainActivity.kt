package com.sd.lib.demo.activity_result

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.sd.lib.actresult.FActivityResult

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val _activityResult = FActivityResult(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_permission -> {
                _activityResult.registerPermission {
                    Log.i(TAG, "registerPermission result:$it")
                }.launch(Manifest.permission.CAMERA)
            }
            R.id.btn_picture -> {
                // 需要先申请权限
                _activityResult.register(ActivityResultContracts.TakePicturePreview()) {
                    Log.i(TAG, "register TakePicturePreview result:$it")
                }.launch(null)
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}