// CustomDialogModule.java

package com.statuspoc;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class CustomDialogModule extends ReactContextBaseJavaModule {

  public CustomDialogModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return "CustomDialog";
  }

  @ReactMethod
  public void show() {
    new FireMissilesDialogFragment().show(getReactApplicationContext().getCurrentActivity().getFragmentManager(), "");
  }
}
