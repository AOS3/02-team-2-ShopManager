package com.lion.shopmanager.ui.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.icu.text.DecimalFormat
import android.media.ExifInterface
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import java.io.InputStream

fun Fragment.showKeyboard(editText: EditText) {
    editText.requestFocus()
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
}

fun Fragment.hideKeyboard() {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = view?.findFocus() // 현재 포커스된 뷰 가져오기
    if (view != null) {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun TextInputLayout.setErrorMessage(msg: String?) {
    this.error = msg
    this.isErrorEnabled = !msg.isNullOrEmpty()
}

fun Int.formatToComma(isWon: Boolean): String {
    val pattern = if (isWon) "#,###원" else "#,###"
    return DecimalFormat(pattern).format(this)
}

fun Bitmap.fixOrientation(): Bitmap {
    // 너비가 높이보다 크다면 90도 회전
    return if (this.width > this.height) {
        this.rotate(90f)
    } else {
        this // 이미 올바른 방향
    }
}

// Bitmap을 회전하는 확장 함수
fun Bitmap.rotate(degrees: Float): Bitmap {
    if (degrees == 0f) return this // 회전이 필요 없는 경우
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
}