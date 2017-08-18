package com.smartnsoft.webviewfragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.smartnsoft.droid4me.support.v4.app.SmartFragment;

/**
 * @author Édouard Mercier
 * @since 2014.04.21
 */
@Deprecated
public abstract class LegacyWebViewFragment<AggregateClass>
    extends SmartFragment<AggregateClass>
{

  protected enum LayoutViewStringDrawableResource
  {
    // The layouts
    LayoutWebView,
    // The View identifiers
    IdWebView, IdLoading, IdWebViewActionBar, IdTitle, IdPreviousButton, IdNextButton, IdRefreshButton,
    // The strings
    StringBack, StringForward, StringRefresh, StringOpenBrowser,
    // The drawables
    DrawableBarPreviousDefault, DrawableBarNextDefault, DrawableBarPreviousDisabled, DrawableBarNextDisabled, DrawableBarRefresh, DrawableBarOpenBrowser
  }

  public static final String PAGE_URL = "pageUrl";

  public static final String SCREEN_TITLE = "screenTitle";

  private String url;

  private WebView webView;

  private View loading;

  private boolean webViewRestored = false;

  @Override
  public void onRetrieveDisplayObjects()
  {
  }

  @SuppressWarnings("deprecation")
  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    final View view = inflater.inflate(getLayoutResourceId(LayoutViewStringDrawableResource.LayoutWebView), container, false);
    webView = view.findViewById(getViewResourceId(LayoutViewStringDrawableResource.IdWebView));
    final WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    // We enable Flash and Javascript in order to have the WatTv videos working
    if (VERSION.SDK_INT >= 8 && VERSION.SDK_INT <= 18)
    {
      webSettings.setPluginState(PluginState.ON);
    }
    loading = view.findViewById(getViewResourceId(LayoutViewStringDrawableResource.IdLoading));
    loading.setVisibility(View.INVISIBLE);
    if (savedInstanceState != null && savedInstanceState.isEmpty() == false)
    {
      webView.restoreState(savedInstanceState);
      webViewRestored = true;
    }
    else
    {
      webViewRestored = false;
    }
    // This call is positioned at this level, so as to prevent issues with the "onCreateOptionsMenu()" method
    setHasOptionsMenu(true);
    {
      // We listen to scroll events
      final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener()
      {
        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY)
        {
          if (distanceY > 0)
          {
            hideActionBarIfNecessary();
          }
          else if (distanceY < 0)
          {
            showActionBarIfNecessary();
          }
          return false;
        }
      };
      final GestureDetector gestureDetector = new GestureDetector(getActivity().getApplicationContext(), gestureListener);
      // This is essential, otherwise the long-press events are not transmitted properly!
      gestureDetector.setIsLongpressEnabled(false);
      webView.setOnTouchListener(new View.OnTouchListener()
      {
        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
          gestureDetector.onTouchEvent(event);
          return false;
        }
      });
    }
    getActivity().setTitle(getActivity().getIntent().getStringExtra(LegacyWebViewFragment.SCREEN_TITLE));
    getActivity().supportInvalidateOptionsMenu();
    return view;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
  {
    super.onCreateOptionsMenu(menu, menuInflater);
    {
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, getStringResourceId(LayoutViewStringDrawableResource.StringBack)).setIcon(
          getDrawableResourceId(webView.canGoBack() == true ? LayoutViewStringDrawableResource.DrawableBarPreviousDefault
              : LayoutViewStringDrawableResource.DrawableBarPreviousDisabled)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
              {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem)
                {
                  if (webView.canGoBack() == true)
                  {
                    webView.goBack();
                  }
                  return true;
                }
              });
      // In order to be compatible with Android v2.3-: see
      // http://stackoverflow.com/questions/17873648/android-support-library-actionbar-not-working-in-2-3-device
      MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
    {
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, getStringResourceId(LayoutViewStringDrawableResource.StringRefresh)).setIcon(
          getDrawableResourceId(LayoutViewStringDrawableResource.DrawableBarRefresh)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
          {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
              webView.reload();
              return true;
            }
          });
      MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
    {
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, getStringResourceId(LayoutViewStringDrawableResource.StringForward)).setIcon(
          getDrawableResourceId(webView.canGoForward() == true ? LayoutViewStringDrawableResource.DrawableBarNextDefault
              : LayoutViewStringDrawableResource.DrawableBarNextDisabled)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
              {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem)
                {
                  if (webView.canGoForward() == true)
                  {
                    webView.goForward();
                  }
                  return true;
                }
              });
      MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
    {
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, getStringResourceId(LayoutViewStringDrawableResource.StringOpenBrowser)).setIcon(
          getDrawableResourceId(LayoutViewStringDrawableResource.DrawableBarOpenBrowser)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
          {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
              final String actualUrl = webView.getUrl() != null ? webView.getUrl() : url;
              try
              {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(actualUrl)));
              }
              catch (Exception exception)
              {
                if (log.isWarnEnabled())
                {
                  log.warn("Could not open the native browser application for displaying the Internet page with URL '" + url + "'", exception);
                }
              }
              return true;
            }
          });
      MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
    }
  }

  @Override
  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    url = getActivity().getIntent().getStringExtra(LegacyWebViewFragment.PAGE_URL);
  }

  @Override
  public void onFulfillDisplayObjects()
  {
    final WebViewClient webViewClient = new WebViewClient()
    {

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
      {
        if (isAlive() == true)
        {
          showActionBarIfNecessary();
          getActivity().supportInvalidateOptionsMenu();
        }

        return super.shouldOverrideUrlLoading(view, request);
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon)
      {
        super.onPageStarted(view, url, favicon);
        if (isAlive() == true)
        {
          // loading.setVisibility(View.VISIBLE);
          getActivity().supportInvalidateOptionsMenu();
          getActivity().setProgressBarIndeterminateVisibility(true);
        }
      }

      @Override
      public void onPageFinished(WebView view, String url)
      {
        super.onPageFinished(view, url);
        if (isAlive() == true)
        {
          if (getActivity().getIntent().hasExtra(LegacyWebViewFragment.SCREEN_TITLE) == false)
          {
            if (getActivity() instanceof ActionBarActivity)
            {
              ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(webView.getTitle());
            }
            else if (getActivity() instanceof AppCompatActivity)
            {
              ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(webView.getTitle());
            }
            else
            {
              getActivity().getActionBar().setTitle(webView.getTitle());
            }
          }

          getActivity().supportInvalidateOptionsMenu();
          getActivity().setProgressBarIndeterminateVisibility(false);
        }
      }

      @Override
      public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
      {
        // TODO: decide something

        super.onReceivedError(view, request, error);
      }

    };
    webView.setWebViewClient(webViewClient);

    if (webViewRestored == false)
    {
      webView.loadUrl(url);
    }

    // For the Android v2.3- support: read
    // http://stackoverflow.com/questions/17879026/2-3-android-device-natigation-drawer-not-geting-method-getactionbar
    getActivity().supportInvalidateOptionsMenu();
  }

  @Override
  public void onSynchronizeDisplayObjects()
  {
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    webView.saveState(outState);
  }

  protected int getLayoutResourceId(LayoutViewStringDrawableResource layoutIdResource)
  {
    switch (layoutIdResource)
    {
    case LayoutWebView:
    default:
      return getResources().getIdentifier("web_view", "layout", getActivity().getPackageName());
    }
  }

  protected int getViewResourceId(LayoutViewStringDrawableResource layoutIdResource)
  {
    final String resourceName;
    switch (layoutIdResource)
    {
    case IdWebView:
      resourceName = "webview";
      break;
    case IdLoading:
      resourceName = "loading";
      break;
    case IdWebViewActionBar:
      resourceName = "webViewActionBar";
      break;
    case IdTitle:
      resourceName = "title";
      break;
    case IdPreviousButton:
      resourceName = "previousButton";
      break;
    case IdNextButton:
      resourceName = "nextButton";
      break;
    case IdRefreshButton:
      resourceName = "refreshButton";
      break;
    default:
      resourceName = "";
      break;
    }
    return getResources().getIdentifier(resourceName, "id", getActivity().getPackageName());
  }

  protected int getStringResourceId(LayoutViewStringDrawableResource layoutIdResource)
  {
    final String resourceName;
    switch (layoutIdResource)
    {
    case StringBack:
      resourceName = "WebView_back";
      break;
    case StringForward:
      resourceName = "WebView_forward";
      break;
    case StringRefresh:
      resourceName = "WebView_refresh";
      break;
    case StringOpenBrowser:
      resourceName = "WebView_openBrowser";
      break;
    default:
      resourceName = "";
      break;
    }
    return getResources().getIdentifier(resourceName, "string", getActivity().getPackageName());
  }

  protected int getDrawableResourceId(LayoutViewStringDrawableResource layoutIdResource)
  {
    final String resourceName;
    switch (layoutIdResource)
    {
    case DrawableBarPreviousDefault:
      resourceName = "ic_web_view_bar_previous_default";
      break;
    case DrawableBarNextDefault:
      resourceName = "ic_web_view_bar_next_default";
      break;
    case DrawableBarPreviousDisabled:
      resourceName = "ic_web_view_bar_previous_disabled";
      break;
    case DrawableBarNextDisabled:
      resourceName = "ic_web_view_bar_next_disabled";
      break;
    case DrawableBarRefresh:
      resourceName = "ic_bar_refresh";
      break;
    case DrawableBarOpenBrowser:
      resourceName = "ic_bar_open_browser";
      break;
    default:
      resourceName = "";
      break;
    }
    return getResources().getIdentifier(resourceName, "drawable", getActivity().getPackageName());
  }

  private void hideActionBarIfNecessary()
  {
    try
    {
      final ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
      if (actionBar.isShowing() == true)
      {
        actionBar.hide();
      }
    }
    catch (NoClassDefFoundError exception)
    {
      final android.app.ActionBar actionBar = getActivity().getActionBar();
      if (actionBar.isShowing() == true)
      {
        actionBar.hide();
      }
    }
  }

  private void showActionBarIfNecessary()
  {
    try
    {
      final ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
      if (actionBar.isShowing() == false)
      {
        actionBar.show();
      }
    }
    catch (NoClassDefFoundError exception)
    {
      final android.app.ActionBar actionBar = getActivity().getActionBar();
      if (actionBar.isShowing() == false)
      {
        actionBar.show();
      }
    }
  }

}
