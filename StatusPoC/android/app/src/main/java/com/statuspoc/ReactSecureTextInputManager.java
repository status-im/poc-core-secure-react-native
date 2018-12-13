package com.statuspoc;

import android.util.Log;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.util.ArrayMap;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewDefaults;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.text.ReactTextUpdate;
import com.facebook.react.views.textinput.ReactEditText;
import com.facebook.react.views.textinput.ReactTextInputManager;

/**
 * Manages instances of SecureTextInput.
 */
@ReactModule(name = ReactSecureTextInputManager.REACT_CLASS)
public class ReactSecureTextInputManager extends ReactTextInputManager {

  protected static final String REACT_CLASS = "RCTSecureTextInput";
  private static final ArrayMap<String, String> registrationMap = new ArrayMap<String, String>();
  private static final ArrayMap<EditText, String> viewToIdMap = new ArrayMap<EditText, String>();
  private static final ArrayMap<String, EditText> idToViewMap = new ArrayMap<String, EditText>();

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @ReactProp(name = "registrationID")
  public void setRegistrationId(ReactSecureEditText view, String id) throws java.lang.SecurityException, IllegalArgumentException {
    if (view == null) {
      throw new IllegalArgumentException("view");
    }
    if (id == null) {
      throw new IllegalArgumentException("id");
    }

    if (!registrationMap.containsKey(id)) {
      registrationMap.put(id, view.getText().toString());
    }
    viewToIdMap.put(view, id);
    idToViewMap.put(id, view);

    view.addTextChangedListener(new SecureReactTextInputTextWatcher(view));
    view.setOnKeyPress(false);
  }

  public static String getText(final String id) {
    return registrationMap.get(id);
  }

  public static void setText(final String id, final String value) {
    registrationMap.put(id, value);

    final EditText view = idToViewMap.get(id);
    if (view != null) {
      view.setText(value);
    }
  }

  @Override
  public final ReactEditText createViewInstance(ThemedReactContext context) {
    ReactSecureEditText editText = new ReactSecureEditText(this, context);
    int inputType = editText.getInputType();
    editText.setInputType(inputType & (~InputType.TYPE_TEXT_FLAG_MULTI_LINE));
    editText.setReturnKeyType("done");
    editText.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        (int) Math.ceil(PixelUtil.toPixelFromSP(ViewDefaults.FONT_SIZE_SP)));
    return editText;
  }

  void onDetachedFromWindow(ReactSecureEditText view) {
    final String id = this.viewToIdMap.get(view);
    if (id == null) {
      Log.d(REACT_CLASS, "unknown ReactSecureEditText detached");
      return;
    }

    this.viewToIdMap.remove(view);
    this.idToViewMap.remove(id);
    this.registrationMap.remove(id);
  }

  class SecureReactTextInputTextWatcher implements TextWatcher {

    private ReactSecureEditText mEditText;
    private String mPreviousText;

    public SecureReactTextInputTextWatcher(
        final ReactSecureEditText editText) {
      mEditText = editText;
      mPreviousText = null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      // Incoming charSequence gets mutated before onTextChanged() is invoked
      mPreviousText = s.toString();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      // Rearranging the text (i.e. changing between singleline and multiline attributes) can
      // also trigger onTextChanged
      if (count == 0 && before == 0) {
        return;
      }

      Assertions.assertNotNull(mPreviousText);
      String newText = s.toString().substring(start, start + count);
      String oldText = mPreviousText.substring(start, start + before);
      // Don't send same text changes
      if (count == before && newText.equals(oldText)) {
        return;
      }

      newText = s.toString();
      //Log.d(REACT_CLASS, "newText: " + newText);
      registrationMap.put(viewToIdMap.get(mEditText), newText);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
  }

  @Override
  public void updateExtraData(ReactEditText view, Object extraData) {
    if (extraData instanceof ReactTextUpdate) {
      ReactTextUpdate update = (ReactTextUpdate) extraData;

      view.setPadding(
          (int) update.getPaddingLeft(),
          (int) update.getPaddingTop(),
          (int) update.getPaddingRight(),
          (int) update.getPaddingBottom());

      final String id = viewToIdMap.get(view);
      final String text = getText(id);
      view.setText(text);
      registrationMap.put(id, text);
    }
  }

  @Override
  protected final void addEventEmitters(
      final ThemedReactContext reactContext,
      final ReactEditText editText) {
    editText.setOnEditorActionListener(
        new TextView.OnEditorActionListener() {
          @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
            // Any 'Enter' action will do
            if ((actionId & EditorInfo.IME_MASK_ACTION) > 0 ||
                actionId == EditorInfo.IME_NULL) {
              boolean blurOnSubmit = editText.getBlurOnSubmit();
              boolean isMultiline = ((editText.getInputType() &
                InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0);

              // Motivation:
              // * blurOnSubmit && isMultiline => Clear focus; prevent default behaviour (return true);
              // * blurOnSubmit && !isMultiline => Clear focus; prevent default behaviour (return true);
              // * !blurOnSubmit && isMultiline => Perform default behaviour (return false);
              // * !blurOnSubmit && !isMultiline => Prevent default behaviour (return true).

              if (blurOnSubmit) {
                editText.clearFocus();
              }

              // Prevent default behavior except when we want it to insert a newline.
              return blurOnSubmit || !isMultiline;
            }

            return true;
          }
        });
  }

  @Override
  public final void setOnKeyPress(final ReactEditText view, boolean onKeyPress) {
    // Explicitly disallow changing this property
    throw new java.lang.SecurityException();
  }
}