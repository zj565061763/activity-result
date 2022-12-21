package com.sd.demo.activity_result

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
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
                _activityResult.register(ActivityResultContracts.RequestPermission()) {
                    logMsg { "register RequestPermission result:$it" }
                }.launch(Manifest.permission.CAMERA)
            }
            R.id.btn_picture -> {
                _activityResult.registerForActivityResult {
                    logMsg { "register picture ${it.data}" }
                }.launch(
                    Intent(Intent.ACTION_PICK, null).apply {
                        this.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                    }
                )
            }
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("activity-result-demo", block())
}