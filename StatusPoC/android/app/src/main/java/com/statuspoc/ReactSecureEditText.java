package com.statuspoc;

import android.content.Context;
import android.text.TextWatcher;

import com.facebook.react.views.text.ReactTextUpdate;
import com.facebook.react.views.textinput.ReactEditText;

public class ReactSecureEditText extends ReactEditText {
  ReactSecureTextInputManager mManager;

  public ReactSecureEditText(ReactSecureTextInputManager manager, Context context) {
    super(context);

    this.mManager = manager;
  }

  @Override
  public final void addTextChangedListener(TextWatcher watcher) throws java.lang.SecurityException {
    if (!(watcher instanceof ReactSecureTextInputManager.SecureReactTextInputTextWatcher)) {
      // Explicitly disallow third-party subscribers
      throw new java.lang.SecurityException();
    }

    super.addTextChangedListener(watcher);
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    this.mManager.onDetachedFromWindow(this);
  }

  @Override
  public final void maybeSetText(ReactTextUpdate reactTextUpdate) {
    // no-op
  }
}