package com.smartnsoft.webviewfragment;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.smartnsoft.droid4me.LifeCycle.BusinessObjectsRetrievalAsynchronousPolicyAnnotation;
import com.smartnsoft.droid4me.support.v4.app.SmartFragment;

/**
 * @author Ludovic Roland
 * @since 2016.06.03
 */
@BusinessObjectsRetrievalAsynchronousPolicyAnnotation
public class WebViewFragment<AggregateClass>
    extends SmartFragment<AggregateClass>
    implements OnClickListener
{

  public static final String PAGE_URL_EXTRA = "pageUrlExtra";

  public static final String SCREEN_TITLE_EXTRA = "screenTitleExtra";

  //Views
  protected View loadingErrorAndRetry;

  protected View errorAndRetry;

  protected WebView webView;

  protected Button retry;

  //webview state
  protected boolean webViewRestored = false;

  protected boolean errorWhenLoadingPage = false;

  protected String url;

  //network state
  protected boolean hasConnectivity = true;

  protected final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      hasConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) == false;
    }
  };

  protected NetworkCallback networkCallback;

  protected Map<String, Boolean> networkStatus = new HashMap<>();

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    final NetworkInfo activeNetworkInfo = getActiveNetworkInfo();
    if (activeNetworkInfo == null || activeNetworkInfo.isConnected() == false)
    {
      hasConnectivity = false;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
    {
      registerBroadcastListenerOnCreate();
    }
    else
    {
      registerBroadcastListenerOnCreateLollipopAndAbove();
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    final View rootView = inflater.inflate(R.layout.web_view, container, false);

    setHasOptionsMenu(true);

    webView = rootView.findViewById(R.id.webview);
    loadingErrorAndRetry = rootView.findViewById(R.id.loadingErrorAndRetry);
    errorAndRetry = rootView.findViewById(R.id.errorAndRetry);
    retry = rootView.findViewById(R.id.retry);

    retry.setOnClickListener(this);

    // Cookies management
    CookieManager.setAcceptFileSchemeCookies(true);
    CookieManager.getInstance().setAcceptCookie(true);

    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
    {
      CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
      CookieManager.getInstance().flush();
    }

    final WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);

    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN)
    {
      webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
      webView.getSettings().setAllowFileAccessFromFileURLs(true);
    }

    // Cache management
    webView.getSettings().setAppCachePath(getActivity().getApplicationContext().getCacheDir().getAbsolutePath());
    webView.getSettings().setAllowFileAccess(true);
    webView.getSettings().setAppCacheEnabled(true);
    webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

    if (savedInstanceState != null)
    {
      webView.restoreState(savedInstanceState);
      webViewRestored = true;
    }
    else
    {
      webViewRestored = false;
    }

    return rootView;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
  {
    super.onCreateOptionsMenu(menu, menuInflater);

    {
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.WebView_back)
          .setIcon(webView.canGoBack() == true ? R.drawable.ic_web_view_bar_previous_default : R.drawable.ic_web_view_bar_previous_disabled)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
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
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.WebView_refresh)
          .setIcon(R.drawable.ic_bar_refresh)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
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
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.WebView_forward)
          .setIcon(webView.canGoForward() == true ? R.drawable.ic_web_view_bar_next_default : R.drawable.ic_web_view_bar_next_disabled)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
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
      final MenuItem menuItem = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.WebView_openBrowser)
          .setIcon(R.drawable.ic_bar_open_browser)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener()
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
  public void onRetrieveDisplayObjects()
  {

  }

  @Override
  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    url = getActivity().getIntent().getStringExtra(WebViewFragment.PAGE_URL_EXTRA);
  }

  @Override
  public void onFulfillDisplayObjects()
  {
    if (getActivity().getIntent().hasExtra(WebViewFragment.SCREEN_TITLE_EXTRA) == false)
    {
      try
      {
        if (getActivity() instanceof AppCompatActivity)
        {
          ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(webView.getTitle());
        }
      }
      catch (Exception exception)
      {
        if (log.isWarnEnabled() == true)
        {
          log.warn("Cannot set the title", exception);
        }
      }
    }

    configureWebView();
  }

  @Override
  public void onSynchronizeDisplayObjects()
  {

  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
    {
      unregisterBroadcastListenerOnDestroy();
    }
    else
    {
      unregisterBroadcastListenerLollipopAndAbove();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    webView.saveState(outState);
  }

  @Override
  public void onClick(View view)
  {
    if (view.equals(retry) == true)
    {
      refresh();
    }
  }

  protected void showLoadingScreen(boolean visible)
  {
    if (visible == true)
    {
      errorAndRetry.setVisibility(View.INVISIBLE);
      loadingErrorAndRetry.setVisibility(View.VISIBLE);
    }
    else
    {
      if (getActivity() != null && getActivity().isFinishing() == false)
      {
        final Animation animation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        animation.setAnimationListener(new AnimationListener()
        {
          @Override
          public void onAnimationStart(Animation animation)
          {
          }

          @Override
          public void onAnimationEnd(Animation animation)
          {
            loadingErrorAndRetry.setVisibility(View.INVISIBLE);
          }

          @Override
          public void onAnimationRepeat(Animation animation)
          {
          }

        });

        loadingErrorAndRetry.startAnimation(animation);
      }
      else
      {
        errorAndRetry.setVisibility(View.INVISIBLE);
        loadingErrorAndRetry.setVisibility(View.INVISIBLE);
      }
    }
  }

  protected void showErrorScreen()
  {
    errorAndRetry.setVisibility(View.VISIBLE);
    loadingErrorAndRetry.setVisibility(View.VISIBLE);
  }

  protected void refresh()
  {
    if (hasConnectivity == true)
    {
      if (webView.getUrl() != null)
      {
        webView.reload();
      }
      else
      {
        webView.loadUrl(url);
      }
      errorWhenLoadingPage = false;
    }
    else
    {
      showErrorScreen();
    }
  }

  private void registerBroadcastListenerOnCreate()
  {
    final IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    getContext().registerReceiver(broadcastReceiver, intentFilter);
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  private void registerBroadcastListenerOnCreateLollipopAndAbove()
  {
    if (networkCallback == null)
    {
      final NetworkRequest.Builder builder = new NetworkRequest.Builder();
      networkCallback = new ConnectivityManager.NetworkCallback()
      {
        @Override
        public void onAvailable(Network network)
        {
          networkStatus.put(network.toString(), true);
          hasConnectivity = networkStatus.containsValue(true);
        }

        @Override
        public void onLost(Network network)
        {
          networkStatus.remove(network.toString());
          hasConnectivity = networkStatus.containsValue(true);
        }
      };

      getConnectivityManager().registerNetworkCallback(builder.build(), networkCallback);
    }
  }

  private void unregisterBroadcastListenerOnDestroy()
  {
    getContext().unregisterReceiver(broadcastReceiver);
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  private void unregisterBroadcastListenerLollipopAndAbove()
  {
    // We listen to the network connection potential issues: we do not want child activities to also register for the connectivity change events
    if (networkCallback != null)
    {
      getConnectivityManager().unregisterNetworkCallback(networkCallback);
      networkCallback = null;
    }
  }

  private NetworkInfo getActiveNetworkInfo()
  {
    return getConnectivityManager().getActiveNetworkInfo();
  }

  private ConnectivityManager getConnectivityManager()
  {
    return ((ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE));
  }

  private void configureWebView()
  {
    final WebViewClient webViewClient = new WebViewClient()
    {

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon)
      {
        super.onPageStarted(view, url, favicon);
        showLoadingScreen(true);
      }

      @Override
      public void onPageFinished(WebView view, String currentURL)
      {
        super.onPageFinished(view, currentURL);

        if (errorWhenLoadingPage == false && hasConnectivity == true)
        {
          showLoadingScreen(false);
        }
        else
        {
          showErrorScreen();
        }
      }

      @Override
      public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
      {
        super.onReceivedError(view, errorCode, description, failingUrl);
        errorWhenLoadingPage = true;
        showErrorScreen();
      }

      @Override
      public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
      {
        super.onReceivedError(view, request, error);
        errorWhenLoadingPage = true;
        showErrorScreen();
      }
    };

    webView.setWebViewClient(webViewClient);

    if (hasConnectivity == true)
    {
      if (webViewRestored == false)
      {
        webView.loadUrl(url);
      }
    }
    else
    {
      showErrorScreen();
    }
  }

}
