package com.smartnsoft.sample;

import android.util.Log;

import com.smartnsoft.webviewfragment.WebViewFragment;

/**
 * @author Ludovic Roland
 * @since 2019.01.14
 */
public final class MyWebviewFragment
    extends WebViewFragment
{

  private static final String TAG = MyWebviewFragment.class.getSimpleName();

  @Override
  protected void warn(String message, Throwable throwable)
  {
    Log.w(MyWebviewFragment.TAG, message, throwable);
  }

}
