package com.statuspoc;

import android.util.Log;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.EditText;
import android.util.ArrayMap;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.BaseViewManager;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.Spacing;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.ViewDefaults;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.annotations.ReactPropGroup;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.text.ReactTextUpdate;
import com.facebook.react.views.textinput.ContentSizeWatcher;
import com.facebook.react.views.textinput.ReactTextInputShadowNode;
import com.facebook.react.views.textinput.ReactContentSizeChangedEvent;
import com.facebook.yoga.YogaConstants;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Manages instances of SecureTextInput2.
 */
@ReactModule(name = SecureTextInputManager.REACT_CLASS)
public class SecureTextInputManager extends BaseViewManager<SecureEditText, LayoutShadowNode> {

  protected static final String REACT_CLASS = "RCTSecureTextInput2";
  private static final ArrayMap<String, String> registrationMap = new ArrayMap<String, String>();
  private static final ArrayMap<EditText, String> viewToIdMap = new ArrayMap<EditText, String>();
  private static final ArrayMap<String, EditText> idToViewMap = new ArrayMap<String, EditText>();

  private static final int[] SPACING_TYPES = {
      Spacing.ALL, Spacing.LEFT, Spacing.RIGHT, Spacing.TOP, Spacing.BOTTOM,
  };

  private static final int FOCUS_TEXT_INPUT = 1;
  private static final int BLUR_TEXT_INPUT = 2;

  private static final int INPUT_TYPE_KEYBOARD_NUMBER_PAD = InputType.TYPE_CLASS_NUMBER; 
  private static final int INPUT_TYPE_KEYBOARD_DECIMAL_PAD = INPUT_TYPE_KEYBOARD_NUMBER_PAD |
          InputType.TYPE_NUMBER_FLAG_DECIMAL;
  private static final int INPUT_TYPE_KEYBOARD_NUMBERED = INPUT_TYPE_KEYBOARD_DECIMAL_PAD |
          InputType.TYPE_NUMBER_FLAG_SIGNED;
  private static final int PASSWORD_VISIBILITY_FLAG = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &
        ~InputType.TYPE_TEXT_VARIATION_PASSWORD;
  private static final int KEYBOARD_TYPE_FLAGS = INPUT_TYPE_KEYBOARD_NUMBERED |
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS |
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_PHONE |
            PASSWORD_VISIBILITY_FLAG;

  private static final String KEYBOARD_TYPE_EMAIL_ADDRESS = "email-address";
  private static final String KEYBOARD_TYPE_NUMERIC = "numeric";
  private static final String KEYBOARD_TYPE_DECIMAL_PAD = "decimal-pad";
  private static final String KEYBOARD_TYPE_NUMBER_PAD = "number-pad";
  private static final String KEYBOARD_TYPE_PHONE_PAD = "phone-pad";
  private static final String KEYBOARD_TYPE_VISIBLE_PASSWORD = "visible-password";
  // private static final InputFilter[] EMPTY_FILTERS = new InputFilter[0];
  private static final int UNSET = -1;
          
  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @ReactProp(name = "registrationID")
  public void setRegistrationId(SecureEditText view, String id) throws java.lang.SecurityException, IllegalArgumentException {
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
    // view.setOnKeyPress(false);
  }

  public static String getText(final String id) {
    return registrationMap.get(id);
  }

  public static void setText(final String id, final String value) {
    registrationMap.put(id, value);

    Log.d(REACT_CLASS, "setText called with " + value);
    final EditText view = idToViewMap.get(id);
    if (view != null) {
      view.setText(value);
      Log.d(REACT_CLASS, "called setText on EditText with " + value);
    }
  }

  @Override
  public final SecureEditText createViewInstance(ThemedReactContext context) {
    SecureEditText editText = new SecureEditText(this, context);
    int inputType = editText.getInputType();
    editText.setInputType(inputType & (~InputType.TYPE_TEXT_FLAG_MULTI_LINE));
    // editText.setReturnKeyType("done");
    editText.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        (int) Math.ceil(PixelUtil.toPixelFromSP(ViewDefaults.FONT_SIZE_SP)));
    return editText;
  }

  @Override
  public LayoutShadowNode createShadowNodeInstance() {
    return new ReactTextInputShadowNode();
  }

  @Override
  public Class<? extends LayoutShadowNode> getShadowNodeClass() {
    return ReactTextInputShadowNode.class;
  }

  void onDetachedFromWindow(SecureEditText view) {
    final String id = this.viewToIdMap.get(view);
    if (id == null) {
      Log.d(REACT_CLASS, "unknown SecureEditText detached");
      return;
    }

    this.viewToIdMap.remove(view);
    this.idToViewMap.remove(id);
    this.registrationMap.remove(id);
  }

  class SecureReactTextInputTextWatcher implements TextWatcher {

    private SecureEditText mEditText;
    private String mPreviousText;

    public SecureReactTextInputTextWatcher(
        final SecureEditText editText) {
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

  private class SecureReactContentSizeWatcher implements ContentSizeWatcher {
    private SecureEditText mEditText;
    private EventDispatcher mEventDispatcher;
    private int mPreviousContentWidth = 0;
    private int mPreviousContentHeight = 0;

    public SecureReactContentSizeWatcher(SecureEditText editText) {
      mEditText = editText;
      ReactContext reactContext = (ReactContext) editText.getContext();
      mEventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
    }

    @Override
    public void onLayout() {
      int contentWidth = mEditText.getWidth();
      int contentHeight = mEditText.getHeight();

      // Use instead size of text content within EditText when available
      if (mEditText.getLayout() != null) {
        contentWidth = mEditText.getCompoundPaddingLeft() + mEditText.getLayout().getWidth() +
          mEditText.getCompoundPaddingRight();
        contentHeight = mEditText.getCompoundPaddingTop() + mEditText.getLayout().getHeight() +
          mEditText.getCompoundPaddingBottom();
      }

      if (contentWidth != mPreviousContentWidth || contentHeight != mPreviousContentHeight) {
        mPreviousContentHeight = contentHeight;
        mPreviousContentWidth = contentWidth;

        mEventDispatcher.dispatchEvent(
          new ReactContentSizeChangedEvent(
            mEditText.getId(),
            PixelUtil.toDIPFromPixel(contentWidth),
            PixelUtil.toDIPFromPixel(contentHeight)));
      }
    }
  }

  @Override
  public @Nullable Map<String, Integer> getCommandsMap() {
    return MapBuilder.of("focusTextInput", FOCUS_TEXT_INPUT, "blurTextInput", BLUR_TEXT_INPUT);
  }

  @Override
  public void receiveCommand(
      SecureEditText reactEditText,
      int commandId,
      @Nullable ReadableArray args) {
    switch (commandId) {
      case FOCUS_TEXT_INPUT:
        reactEditText.requestFocusFromJS();
        break;
      case BLUR_TEXT_INPUT:
        reactEditText.clearFocusFromJS();
        break;
    }
  }

  @Override
  public void updateExtraData(SecureEditText view, Object extraData) {
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
      Log.d(REACT_CLASS, "called setText on EditText from updateExtraData with " + text);
      registrationMap.put(id, text);
    }
  }

  @ReactProp(name = "blurOnSubmit")
  public void setBlurOnSubmit(SecureEditText view, @Nullable Boolean blurOnSubmit) {
    view.setBlurOnSubmit(blurOnSubmit);
  }

  @ReactProp(name = "onContentSizeChange", defaultBoolean = false)
  public void setOnContentSizeChange(final SecureEditText view, boolean onContentSizeChange) {
    if (onContentSizeChange) {
      view.setContentSizeWatcher(new SecureReactContentSizeWatcher(view));
    } else {
      view.setContentSizeWatcher(null);
    }
  }

  @ReactProp(name = "editable", defaultBoolean = true)
  public void setEditable(SecureEditText view, boolean editable) {
    view.setEnabled(editable);
  }

  @ReactProp(name = "multiline", defaultBoolean = false)
  public void setMultiline(SecureEditText view, boolean multiline) {
    updateStagedInputTypeFlag(
        view,
        multiline ? 0 : InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0);
  }

  @ReactProp(name = "secureTextEntry", defaultBoolean = false)
  public void setSecureTextEntry(SecureEditText view, boolean password) {
    updateStagedInputTypeFlag(
        view,
        password ? 0 :
            InputType.TYPE_NUMBER_VARIATION_PASSWORD | InputType.TYPE_TEXT_VARIATION_PASSWORD,
        password ? InputType.TYPE_TEXT_VARIATION_PASSWORD : 0);
    checkPasswordType(view);
  }

  @ReactProp(name = "keyboardType")
  public void setKeyboardType(SecureEditText view, @Nullable String keyboardType) {
    int flagsToSet = InputType.TYPE_CLASS_TEXT;
    if (KEYBOARD_TYPE_NUMERIC.equalsIgnoreCase(keyboardType)) {
      flagsToSet = INPUT_TYPE_KEYBOARD_NUMBERED;
    } else if (KEYBOARD_TYPE_NUMBER_PAD.equalsIgnoreCase(keyboardType)) {
      flagsToSet = INPUT_TYPE_KEYBOARD_NUMBER_PAD;
    } else if (KEYBOARD_TYPE_DECIMAL_PAD.equalsIgnoreCase(keyboardType)) {
      flagsToSet = INPUT_TYPE_KEYBOARD_DECIMAL_PAD;
    } else if (KEYBOARD_TYPE_EMAIL_ADDRESS.equalsIgnoreCase(keyboardType)) {
      flagsToSet = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT;
    } else if (KEYBOARD_TYPE_PHONE_PAD.equalsIgnoreCase(keyboardType)) {
      flagsToSet = InputType.TYPE_CLASS_PHONE;
    } else if (KEYBOARD_TYPE_VISIBLE_PASSWORD.equalsIgnoreCase(keyboardType)) {
      // This will supercede secureTextEntry={false}. If it doesn't, due to the way
      //  the flags work out, the underlying field will end up a URI-type field.
      flagsToSet = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    }
    updateStagedInputTypeFlag(
        view,
        KEYBOARD_TYPE_FLAGS,
        flagsToSet);
    checkPasswordType(view);
  }

  @ReactProp(name = "returnKeyType")
  public void setReturnKeyType(SecureEditText view, String returnKeyType) {
    view.setReturnKeyType(returnKeyType);
  }

  @ReactProp(name = "disableFullscreenUI", defaultBoolean = false)
  public void setDisableFullscreenUI(SecureEditText view, boolean disableFullscreenUI) {
    view.setDisableFullscreenUI(disableFullscreenUI);
  }

  private static final int IME_ACTION_ID = 0x670;

  @ReactProp(name = "returnKeyLabel")
  public void setReturnKeyLabel(SecureEditText view, String returnKeyLabel) {
    view.setImeActionLabel(returnKeyLabel, IME_ACTION_ID);
  }

  @ReactPropGroup(names = {
      ViewProps.BORDER_RADIUS,
      ViewProps.BORDER_TOP_LEFT_RADIUS,
      ViewProps.BORDER_TOP_RIGHT_RADIUS,
      ViewProps.BORDER_BOTTOM_RIGHT_RADIUS,
      ViewProps.BORDER_BOTTOM_LEFT_RADIUS
  }, defaultFloat = YogaConstants.UNDEFINED)
  public void setBorderRadius(SecureEditText view, int index, float borderRadius) {
    if (!YogaConstants.isUndefined(borderRadius)) {
      borderRadius = PixelUtil.toPixelFromDIP(borderRadius);
    }

    if (index == 0) {
      view.setBorderRadius(borderRadius);
    } else {
      view.setBorderRadius(borderRadius, index - 1);
    }
  }

  @ReactProp(name = "borderStyle")
  public void setBorderStyle(SecureEditText view, @Nullable String borderStyle) {
    view.setBorderStyle(borderStyle);
  }

  @ReactPropGroup(names = {
      ViewProps.BORDER_WIDTH,
      ViewProps.BORDER_LEFT_WIDTH,
      ViewProps.BORDER_RIGHT_WIDTH,
      ViewProps.BORDER_TOP_WIDTH,
      ViewProps.BORDER_BOTTOM_WIDTH,
  }, defaultFloat = YogaConstants.UNDEFINED)
  public void setBorderWidth(SecureEditText view, int index, float width) {
    if (!YogaConstants.isUndefined(width)) {
      width = PixelUtil.toPixelFromDIP(width);
    }
    view.setBorderWidth(SPACING_TYPES[index], width);
  }

  @ReactPropGroup(names = {
      "borderColor", "borderLeftColor", "borderRightColor", "borderTopColor", "borderBottomColor"
  }, customType = "Color")
  public void setBorderColor(SecureEditText view, int index, Integer color) {
    float rgbComponent = color == null ? YogaConstants.UNDEFINED : (float) ((int)color & 0x00FFFFFF);
    float alphaComponent = color == null ? YogaConstants.UNDEFINED : (float) ((int)color >>> 24);
    view.setBorderColor(SPACING_TYPES[index], rgbComponent, alphaComponent);
  }

  @Override
  protected void onAfterUpdateTransaction(SecureEditText view) {
    super.onAfterUpdateTransaction(view);
    view.commitStagedInputType();
  }

  // Sets the correct password type, since numeric and text passwords have different types
  private static void checkPasswordType(SecureEditText view) {
    if ((view.getStagedInputType() & INPUT_TYPE_KEYBOARD_NUMBERED) != 0 &&
        (view.getStagedInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0) {
      // Text input type is numbered password, remove text password variation, add numeric one
      updateStagedInputTypeFlag(
          view,
          InputType.TYPE_TEXT_VARIATION_PASSWORD,
          InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }
  }

  private static void updateStagedInputTypeFlag(
      SecureEditText view,
      int flagsToUnset,
      int flagsToSet) {
    view.setStagedInputType((view.getStagedInputType() & ~flagsToUnset) | flagsToSet);
  }

  @Override
  protected final void addEventEmitters(
      final ThemedReactContext reactContext,
      final SecureEditText editText) {
    // editText.setOnEditorActionListener(
    //     new TextView.OnEditorActionListener() {
    //       @Override
    //       public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
    //         // Any 'Enter' action will do
    //         if ((actionId & EditorInfo.IME_MASK_ACTION) > 0 ||
    //             actionId == EditorInfo.IME_NULL) {
    //           boolean blurOnSubmit = editText.getBlurOnSubmit();
    //           boolean isMultiline = ((editText.getInputType() &
    //             InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0);

    //           // Motivation:
    //           // * blurOnSubmit && isMultiline => Clear focus; prevent default behaviour (return true);
    //           // * blurOnSubmit && !isMultiline => Clear focus; prevent default behaviour (return true);
    //           // * !blurOnSubmit && isMultiline => Perform default behaviour (return false);
    //           // * !blurOnSubmit && !isMultiline => Prevent default behaviour (return true).

    //           if (blurOnSubmit) {
    //             editText.clearFocus();
    //           }

    //           // Prevent default behavior except when we want it to insert a newline.
    //           return blurOnSubmit || !isMultiline;
    //         }

    //         return true;
    //       }
    //     });
  }
}