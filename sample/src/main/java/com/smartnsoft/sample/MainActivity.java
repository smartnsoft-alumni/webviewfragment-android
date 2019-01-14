package com.smartnsoft.sample;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

/**
 * @author Ludovic Roland
 * @since 2019.01.14
 */
public class MainActivity
    extends AppCompatActivity
{

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    final Bundle bundle = new Bundle();
    bundle.putString(MyWebviewFragment.PAGE_URL_EXTRA, "https://www.hagergroup.com/");
    bundle.putString(MyWebviewFragment.DEFAULT_ERROR_MESSAGE_EXTRA, "Something wrong happened");

    final MyWebviewFragment myWebviewFragment = new MyWebviewFragment();
    myWebviewFragment.setArguments(bundle);

    final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    fragmentTransaction.replace(R.id.fragmentContainer, myWebviewFragment);
    fragmentTransaction.commitAllowingStateLoss();
  }

}
