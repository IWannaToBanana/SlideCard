package com.ljc.slidecard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class MyAdapter extends PagerAdapter {

    private ImageView ivCard;
    private TextView tvCard;

    private LinkedList<Order> list;
    private Activity ctx;
    private View mCurrentView;


    public MyAdapter(Activity context, LinkedList<Order> linkedList) {
        list = linkedList;
        ctx = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @SuppressLint({"CutPasteId", "SetTextI18n"})
    @Override
    public Object instantiateItem(ViewGroup container, int position) {//必须实现，实例化
        View view = LayoutInflater.from(ctx).inflate(R.layout.item, null);
        Order crtOrder = list.get(position);

        ivCard = view.findViewById(R.id.ivCard);
        tvCard = view.findViewById(R.id.tvCard);

        ivCard.setImageResource(crtOrder.getImg_url());
        tvCard.setText(crtOrder.getTilte_text());

        container.addView(view);
        return view;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        mCurrentView = (View) object;
    }

    public View getPrimaryItem() {
        return mCurrentView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {//必须实现，销毁
        container.removeView((View) object);
    }

    public Order getItem(int i) {
        return list.get(i);
    }

    public LinkedList<Order> getList() {
        return list;
    }

    public void add(Order viewPagerItemBean) {
        list.add(viewPagerItemBean);
    }

    public void remove(int id) {
        for (Order order : list) {
            if (order.getId() == id) {
                list.remove(order);
                return;
            }
        }
    }
}
