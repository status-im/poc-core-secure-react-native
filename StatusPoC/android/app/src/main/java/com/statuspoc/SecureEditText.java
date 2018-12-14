package com.statuspoc;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.method.QwertyKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.views.textinput.ContentSizeWatcher;
import com.facebook.react.views.textinput.ReactTextInputLocalData;
import com.facebook.react.views.view.ReactViewBackgroundManager;
import javax.annotation.Nullable;

/**
 * A wrapper around the EditText that lets us better control what happens when an EditText gets
 * focused or blurred, and when to display the soft keyboard and when not to.
 *
 * ReactEditTexts have setFocusableInTouchMode set to false automatically because touches on the
 * EditText are managed on the JS side. This also removes the nasty side effect that EditTexts
 * have, which is that focus is always maintained on one of the EditTexts.
 */
public class SecureEditText extends EditText {
  private SecureTextInputManager mManager;
  private final InputMethodManager mInputMethodManager;
  // This component is controlled, so we want it to get focused only when JS ask it to do so.
  // Whenever android requests focus (which it does for random reasons), it will be ignored.
  private boolean mIsJSSettingFocus;
  private int mStagedInputType;
  private @Nullable Boolean mBlurOnSubmit;
  private boolean mDisableFullscreen;
  private @Nullable String mReturnKeyType;
  private @Nullable ContentSizeWatcher mContentSizeWatcher;
  private final InternalKeyListener mKeyListener;
  private float mLetterSpacingPt = 0;

  private ReactViewBackgroundManager mReactBackgroundManager;

  private static final KeyListener sKeyListener = QwertyKeyListener.getInstanceForFullKeyboard();

  public SecureEditText(SecureTextInputManager manager, Context context) {
    super(context);
    setFocusableInTouchMode(false);

    mManager = manager;
    mReactBackgroundManager = new ReactViewBackgroundManager(this);
    mInputMethodManager = (InputMethodManager)
        Assertions.assertNotNull(getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
    mIsJSSettingFocus = false;
    mBlurOnSubmit = null;
    mDisableFullscreen = false;
    mStagedInputType = getInputType();
    mKeyListener = new InternalKeyListener();
  }

  @Override
  public final void addTextChangedListener(TextWatcher watcher) throws java.lang.SecurityException {
    if (!(watcher instanceof SecureTextInputManager.SecureReactTextInputTextWatcher)) {
      // Explicitly disallow third-party subscribers
      throw new java.lang.SecurityException();
    }

    super.addTextChangedListener(watcher);
  }

  public void setContentSizeWatcher(ContentSizeWatcher contentSizeWatcher) {
    mContentSizeWatcher = contentSizeWatcher;
  }

  // After the text changes inside an EditText, TextView checks if a layout() has been requested.
  // If it has, it will not scroll the text to the end of the new text inserted, but wait for the
  // next layout() to be called. However, we do not perform a layout() after a requestLayout(), so
  // we need to override isLayoutRequested to force EditText to scroll to the end of the new text
  // immediately.
  // TODO: t6408636 verify if we should schedule a layout after a View does a requestLayout()
  @Override
  public boolean isLayoutRequested() {
    return false;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    onContentSizeChange();
  }

  // Consume 'Enter' key events: TextView tries to give focus to the next TextInput, but it can't
  // since we only allow JS to change focus, which in turn causes TextView to crash.
  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_ENTER && !isMultiline()) {
      hideSoftKeyboard();
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    ReactContext reactContext = (ReactContext) getContext();
    InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
    // if (inputConnection != null && mOnKeyPress) {
    //   inputConnection = new ReactEditTextInputConnectionWrapper(inputConnection, reactContext, this);
    // }

    if (isMultiline() && getBlurOnSubmit()) {
      // Remove IME_FLAG_NO_ENTER_ACTION to keep the original IME_OPTION
      outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
    }
    return inputConnection;
  }

  @Override
  public void clearFocus() {
    setFocusableInTouchMode(false);
    super.clearFocus();
    hideSoftKeyboard();
  }

  @Override
  public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    // Always return true if we are already focused. This is used by android in certain places,
    // such as text selection.
    if (isFocused()) {
      return true;
    }
    if (!mIsJSSettingFocus) {
      return false;
    }
    setFocusableInTouchMode(true);
    boolean focused = super.requestFocus(direction, previouslyFocusedRect);
    showSoftKeyboard();
    return focused;
  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    // if (!mIsSettingTextFromJS && mListeners != null) {
    //   for (TextWatcher listener : mListeners) {
    //     listener.onTextChanged(s, start, before, count);
    //   }
    // }

    onContentSizeChange();
  }

  private boolean isMultiline() {
    return (getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
  }

  private boolean isSecureText() {
    return
      (getInputType() &
        (InputType.TYPE_NUMBER_VARIATION_PASSWORD |
          InputType.TYPE_TEXT_VARIATION_PASSWORD))
      != 0;
  }

  private void onContentSizeChange() {
    // if (mContentSizeWatcher != null) {
    //   mContentSizeWatcher.onLayout();
    // }

    setIntrinsicContentSize();
  }

  private void setIntrinsicContentSize() {
    ReactContext reactContext = (ReactContext) getContext();
    UIManagerModule uiManager = reactContext.getNativeModule(UIManagerModule.class);
    final ReactTextInputLocalData localData = new ReactTextInputLocalData(this);
    uiManager.setViewLocalData(getId(), localData);
  }

  private void updateImeOptions() {
    // Default to IME_ACTION_DONE
    int returnKeyFlag = EditorInfo.IME_ACTION_DONE;
    if (mReturnKeyType != null) {
      switch (mReturnKeyType) {
        case "go":
          returnKeyFlag = EditorInfo.IME_ACTION_GO;
          break;
        case "next":
          returnKeyFlag = EditorInfo.IME_ACTION_NEXT;
          break;
        case "none":
          returnKeyFlag = EditorInfo.IME_ACTION_NONE;
          break;
        case "previous":
          returnKeyFlag = EditorInfo.IME_ACTION_PREVIOUS;
          break;
        case "search":
          returnKeyFlag = EditorInfo.IME_ACTION_SEARCH;
          break;
        case "send":
          returnKeyFlag = EditorInfo.IME_ACTION_SEND;
          break;
        case "done":
          returnKeyFlag = EditorInfo.IME_ACTION_DONE;
          break;
      }
    }

    if (mDisableFullscreen) {
      setImeOptions(returnKeyFlag | EditorInfo.IME_FLAG_NO_FULLSCREEN);
    } else {
      setImeOptions(returnKeyFlag);
    }
  }

  public void setBlurOnSubmit(@Nullable Boolean blurOnSubmit) {
    mBlurOnSubmit = blurOnSubmit;
  }

  public boolean getBlurOnSubmit() {
    if (mBlurOnSubmit == null) {
      // Default blurOnSubmit
      return isMultiline() ? false : true;
    }

    return mBlurOnSubmit;
  }

  public void setDisableFullscreenUI(boolean disableFullscreenUI) {
    mDisableFullscreen = disableFullscreenUI;
    updateImeOptions();
  }

  public boolean getDisableFullscreenUI() {
    return mDisableFullscreen;
  }

  public void setReturnKeyType(String returnKeyType) {
    mReturnKeyType = returnKeyType;
    updateImeOptions();
  }

  public String getReturnKeyType() {
    return mReturnKeyType;
  }

  /*protected*/ int getStagedInputType() {
    return mStagedInputType;
  }

  /*package*/ void setStagedInputType(int stagedInputType) {
    mStagedInputType = stagedInputType;
  }

  /*package*/ void commitStagedInputType() {
    if (getInputType() != mStagedInputType) {
      int selectionStart = getSelectionStart();
      int selectionEnd = getSelectionEnd();
      setInputType(mStagedInputType);
      setSelection(selectionStart, selectionEnd);
    }
  }

  @Override
  public void setInputType(int type) {
    Typeface tf = super.getTypeface();
    super.setInputType(type);
    mStagedInputType = type;
    // Input type password defaults to monospace font, so we need to re-apply the font
    super.setTypeface(tf);

    // We override the KeyListener so that all keys on the soft input keyboard as well as hardware
    // keyboards work. Some KeyListeners like DigitsKeyListener will display the keyboard but not
    // accept all input from it
    mKeyListener.setInputType(type);
    setKeyListener(mKeyListener);
  }

  // VisibleForTesting from {@link TextInputEventsTestCase}.
  public void requestFocusFromJS() {
    mIsJSSettingFocus = true;
    requestFocus();
    mIsJSSettingFocus = false;
  }

  /* package */ void clearFocusFromJS() {
    clearFocus();
  }

  private boolean showSoftKeyboard() {
    return mInputMethodManager.showSoftInput(this, 0);
  }

  private void hideSoftKeyboard() {
    mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    this.mManager.onDetachedFromWindow(this);
  }

  @Override
  public void setBackgroundColor(int color) {
    mReactBackgroundManager.setBackgroundColor(color);
  }

  public void setBorderWidth(int position, float width) {
    mReactBackgroundManager.setBorderWidth(position, width);
  }

  public void setBorderColor(int position, float color, float alpha) {
    mReactBackgroundManager.setBorderColor(position, color, alpha);
  }

  public void setBorderRadius(float borderRadius) {
    mReactBackgroundManager.setBorderRadius(borderRadius);
  }

  public void setBorderRadius(float borderRadius, int position) {
    mReactBackgroundManager.setBorderRadius(borderRadius, position);
  }

  public void setBorderStyle(@Nullable String style) {
    mReactBackgroundManager.setBorderStyle(style);
  }

  public void setLetterSpacingPt(float letterSpacingPt) {
    mLetterSpacingPt = letterSpacingPt;
    updateLetterSpacing();
  }

  @Override
  public void setTextSize (float size) {
    super.setTextSize(size);
    updateLetterSpacing();
  }

  @Override
  public void setTextSize (int unit, float size) {
    super.setTextSize(unit, size);
    updateLetterSpacing();
  }

  protected void updateLetterSpacing() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setLetterSpacing(PixelUtil.toPixelFromSP(mLetterSpacingPt) / getTextSize());
    }
  }

  /*
   * This class is set as the KeyListener for the underlying TextView
   * It does two things
   *  1) Provides the same answer to getInputType() as the real KeyListener would have which allows
   *     the proper keyboard to pop up on screen
   *  2) Permits all keyboard input through
   */
  private static class InternalKeyListener implements KeyListener {

    private int mInputType = 0;

    public InternalKeyListener() {
    }

    public void setInputType(int inputType) {
      mInputType = inputType;
    }

    /*
     * getInputType will return whatever value is passed in.  This will allow the proper keyboard
     * to be shown on screen but without the actual filtering done by other KeyListeners
     */
    @Override
    public int getInputType() {
      return mInputType;
    }

    /*
     * All overrides of key handling defer to the underlying KeyListener which is shared by all
     * ReactEditText instances.  It will basically allow any/all keyboard input whether from
     * physical keyboard or from soft input.
     */
    @Override
    public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
      return sKeyListener.onKeyDown(view, text, keyCode, event);
    }

    @Override
    public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
      return sKeyListener.onKeyUp(view, text, keyCode, event);
    }

    @Override
    public boolean onKeyOther(View view, Editable text, KeyEvent event) {
      return sKeyListener.onKeyOther(view, text, event);
    }

    @Override
    public void clearMetaKeyState(View view, Editable content, int states) {
      sKeyListener.clearMetaKeyState(view, content, states);
    }
  }
}