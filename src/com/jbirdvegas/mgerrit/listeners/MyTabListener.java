package com.jbirdvegas.mgerrit.listeners;


import android.app.ActionBar;
import android.content.Context;

import android.support.v4.view.ViewPager;
import android.app.FragmentTransaction;

public class MyTabListener implements ActionBar.TabListener
{
    private ViewPager mViewPager;
    private Context mContext;

    public MyTabListener(ViewPager viewPager, Context context)
    {
        this.mViewPager = viewPager;
        this.mContext = context;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab,
            FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab,
            FragmentTransaction fragmentTransaction) {
        // Not used
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab,
            FragmentTransaction fragmentTransaction) {
        // Not used
    }
}
