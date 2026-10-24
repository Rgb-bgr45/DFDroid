package com.rgb.dfdroid;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

final class InputMethodHelper {
    static void toggle(Context c, View v) {
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (v != null) v.requestFocus();
        if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
}
