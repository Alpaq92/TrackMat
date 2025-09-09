package com.naviq.trackmat

import android.content.Context
import android.util.AttributeSet
import com.vanniktech.vntnumberpickerpreference.VNTNumberPickerPreference


class NumberPickerPreference(context: Context, attrs: AttributeSet?) :
    VNTNumberPickerPreference(context, attrs) {
    @Deprecated("Deprecated in Java")
    override fun setSummary(summary: CharSequence?) {
    }
}