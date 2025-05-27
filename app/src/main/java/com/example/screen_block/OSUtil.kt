package com.example.screen_block

import android.content.Context
import android.widget.Toast

class OSUtil {

    private var context:Context

    constructor(context: Context){
        this.context = context
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


}