package com.ljc.slidecard;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewConfigurationCompat;
import androidx.viewpager.widget.ViewPager;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends Activity {
    private int[] mData = {R.drawable.img0, R.drawable.img1, R.drawable.img2, R.drawable.img3, R.drawable.img4, R.drawable.img0, R.drawable.img1, R.drawable.img2, R.drawable.img3, R.drawable.img4};

    private TextView page;
    private ProgressBar progressBar;
    private ViewPager viewpager;
    private Button btn;

    private MyAdapter adapter;
    private LinkedList<Order> linkedList = new LinkedList<>();

    private int Current = 0;
    private Handler handler;
    private Runnable runnable;
    //进度条计时器
    private Timer progressBarTimer;
    private TimerTask timerTask;
    private Runnable runnable3;
    private Handler progressBarHandler;
    //循环播放的固定时间
    private long LOOP_TIME = 1000 * 15;
    //当前进度
    private int progressNow;
    private int progressPlayOver;
    //是用户滑动还是自动轮播
    boolean isTouch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        page = findViewById(R.id.page);
        progressBar = findViewById(R.id.progress_bar);
        viewpager = findViewById(R.id.viewpager);
        btn = findViewById(R.id.btn);
        initProgressBarHandler();
        initHandler();
        initView();
        initdata();
        initEvent();
        add();
        setPage();
    }


    @SuppressLint("HandlerLeak")
    private void initProgressBarHandler() {
        progressBarHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        if (progressNow > 0) {
                            progressBar.setProgress(progressNow);
                        }
                        break;
                }
            }
        };
        runnable3 = new Runnable() {
            @Override
            public void run() {
                if (progressNow > 0) {
                    progressBarHandler.sendMessage(progressBarHandler.obtainMessage(0));
                }
                progressBarHandler.postDelayed(this, 15);
            }
        };
        new Thread(runnable3).start();
    }

    @SuppressLint("SetTextI18n")
    private void setPage() {
        //设置一下页码
        String s = String.valueOf(Current + 1);
        String s1 = String.valueOf(adapter.getList().size());
        page.setText(s + "/" + s1);
        String zero = "";
        switch (s.length()) {
            case 1:
                zero = "00";
                break;
            case 2:
                zero = "0";
                break;
            case 3:
                zero = "";
                break;
        }
        switch (s1.length()) {
            case 1:
                page.setText(zero + s + "/" + "00" + s1);
                break;
            case 2:
                page.setText(zero + s + "/" + "0" + s1);
                break;
            case 3:
                page.setText(zero + s + "/" + "" + s1);
                break;
        }
    }

    private void add() {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Order o = new Order(adapter.getList().size(), R.drawable.img0, "订单：" + adapter.getList().size());
                synchronized (adapter.getList()) {
                    adapter.getList().add(adapter.getList().size(), o);
                }
                adapter.notifyDataSetChanged();
                setPage();
            }
        });
    }

    private void initdata() {
        for (int i = 0; i < mData.length; i++) {
            Order o = new Order(i, mData[i], "订单：" + i);
            adapter.add(o);
            adapter.notifyDataSetChanged();
        }
        playOrder();
    }

    @SuppressLint("HandlerLeak")
    private void initHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0://关闭自动循环播放
                        handler.removeCallbacks(runnable);
                        break;
                    case 1://打开自动循环播放
                        handler.postDelayed(runnable, LOOP_TIME);
                        break;
                }
                super.handleMessage(msg);
            }
        };
        runnable = new Runnable() {
            @Override
            public void run() {
                if (adapter.getList().size() - 1 == Current) {
                    viewpager.setCurrentItem(0);
                } else {
                    viewpager.setCurrentItem(Current + 1);
                }
                handler.postDelayed(this, LOOP_TIME);
            }
        };
        handler.postDelayed(runnable, LOOP_TIME);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {
        ViewConfiguration configuration = ViewConfiguration.get(this);
        final int mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        viewpager.setOnTouchListener(new View.OnTouchListener() {
            private long TouchTime;//记录点击时间
            private ObjectAnimator mObjectAnimatorY;
            int touchFlag = 0; //通知UP事件类型,左右滑动禁止上下滑动，反之上下滑动禁止左右滑动
            float x = 0, y = 0;//父容器坐标轴
            float rawX = 0, rawY = 0;//系统页面坐标轴
            float lastX = 0, lastY = 0;//记录上次坐标
            float xDiff = 0, yDiff = 0;//偏移量
            float nextY = 0;
            boolean isSlideDel = false;
            View childAt;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i("MotionEvent", "ACTION_DOWN");
                        handler.sendMessage(handler.obtainMessage(0));
                        TouchTime = System.currentTimeMillis();
                        rawX = event.getRawX();
                        rawY = event.getRawY();
                        touchFlag = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.i("MotionEvent", "ACTION_MOVE");
                        synchronized (MainActivity.class) {
                            childAt = adapter.getPrimaryItem();
                        }
                        if (childAt != null) {
                            //每次移动的距离
                            float distanceX = 0;
                            float distanceY = 0;
                            if (lastY != 0 && lastX != 0) {
                                distanceX = event.getRawX() - lastX;
                                distanceY = event.getRawY() - lastY;

                                nextY = 0;

                                //通知ACTION_UP本次是什么事件
                                xDiff = Math.abs(event.getRawX() - rawX);
                                yDiff = Math.abs(event.getRawY() - rawY);
                                if (xDiff < mTouchSlop && yDiff < mTouchSlop) {
                                    Log.i("MotionEvent", "点击事件");
                                    touchFlag = 0;
                                } else if (xDiff > mTouchSlop && xDiff > yDiff) {
                                    if (touchFlag != 2) {
                                        touchFlag = 1;
                                        Log.i("MotionEvent", "左右滑动事件");

                                        if (Math.abs(distanceX) > 100) {
                                            //左右滑动交给viewpager 这里不做处理,只记录坐标
                                            Log.i("MotionEvent", "fragment" + Current + "左右滑动");
                                            Log.i("MotionEvent", "fragment" + Current + "左右滑动距离 = " + Math.abs(distanceX));
                                            lastX = event.getRawX();
                                            lastY = event.getRawY();
                                        }
                                        break;
                                    }
                                } else if (yDiff > mTouchSlop && yDiff > xDiff) {
                                    if (touchFlag != 1) {
                                        touchFlag = 2;
                                        Log.i("MotionEvent", "上下滑动事件");

                                        if (event.getRawY() < lastY) {
                                            //上滑distanceY为负
                                            if (distanceY < -250) {
                                                //滑出页面
                                                nextY = childAt.getY() + distanceY;
                                                isSlideDel = true;
                                                Log.i("MotionEvent", "fragment" + Current + "上滑——滑出页面");
                                            } else {
                                                Log.i("MotionEvent", "fragment" + Current + "上滑——弹回原位");
                                                nextY = childAt.getY() + distanceY;
                                                isSlideDel = false;
                                            }
                                            slide();
                                        } else if (event.getRawY() > lastY) {
                                            //下滑distanceY为正
                                            nextY = childAt.getY() + distanceY;
                                            if (nextY > 200) {
                                                nextY = 200;
                                            }
                                            isSlideDel = false;
                                            Log.i("MotionEvent", "fragment" + Current + "下滑——弹回原位");
                                            slide();
                                        }
                                        Log.i("MotionEvent", "fragment" + Current + "上下滑动距离 = " + distanceY);
                                    }
                                }
                            }
                            //移动完之后记录当前坐标
                            lastX = event.getRawX();
                            lastY = event.getRawY();
                            break;
                        }
                    case MotionEvent.ACTION_UP:
                        Log.i("MotionEvent", "ACTION_UP");
                        Log.i("MotionEvent", "touchFlag =" + touchFlag);
                        handler.sendMessage(handler.obtainMessage(1));

                        if (touchFlag == 0) {//单击事件
                            if (System.currentTimeMillis() - TouchTime > 1000) {//长按事件
                                Toast.makeText(MainActivity.this, "长按了一次", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "单击了一次", Toast.LENGTH_SHORT).show();
                            }
                        } else if (touchFlag == 1) {//左右滑动
                            //TODO 把旁边的view位置归零
                            View leftView;
                            View rightView;
                            if (Current - 1 > 0) {
                                leftView = viewpager.getChildAt(Current - 1);
                                back(leftView);
                            }
                            if (Current + 1 < adapter.getList().size()) {
                                rightView = viewpager.getChildAt(Current + 1);
                                back(rightView);
                            }
                            isTouch = true;
                        } else if (touchFlag == 2) {//上下滑动
                            Log.i("MotionEvent", "nextY = " + nextY);
                            if (isSlideDel || nextY < -500) {//通过删除滑动距离和最后抬手的位置确定用户是否想删除
                                //TODO 应该在手指离开后确定执行删除动画
                                synchronized (MainActivity.class) {
                                    //滑出了
                                    slideDel();
                                    delete();
                                }
                            } else {
                                back(childAt);
                            }
                        }
                }
                return false;
            }

            /**
             * 实时滑动
             */
            private void slide() {
                Log.i("MotionEvent", "fragment" + Current + "slide = " + nextY);
                mObjectAnimatorY = ObjectAnimator.ofFloat(childAt, "y", childAt.getY(), nextY);
                AnimatorSet mAnimatorSet = new AnimatorSet();
                mAnimatorSet.playTogether(mObjectAnimatorY);//只允许上下滑动
                mAnimatorSet.setDuration(0);
                mAnimatorSet.start();
            }

            private void slideDel() {
                Log.i("MotionEvent", "fragment" + Current + "slideDel = " + nextY);
                mObjectAnimatorY = ObjectAnimator.ofFloat(childAt, "y", childAt.getY(), -2500f);
                AnimatorSet mAnimatorSet = new AnimatorSet();
                mAnimatorSet.playTogether(mObjectAnimatorY);//只允许上下滑动
                mAnimatorSet.setDuration(300);
                mAnimatorSet.start();
            }

            /**
             * 未滑出，弹回原位
             */
            private void back(View view) {
                mObjectAnimatorY = ObjectAnimator.ofFloat(view, "y", 0);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(mObjectAnimatorY);//只需要上下滑动
                animatorSet.setDuration(300);
                animatorSet.start();
                Log.i("MotionEvent", "fragment" + Current + "弹回原位");
            }
        });
    }


    /**
     * 主动设置页面并不会回调onPageSelected方法
     * Current和PlayOrder()需要手动处理
     */
    private void delete() {
        int position = 0;
        try {
            if (adapter.getList().size() > 1) { //还剩两张以上
                Order order = adapter.getList().get(Current);
                adapter.remove(order.getId());
                adapter.notifyDataSetChanged();

                if (adapter.getList().size() == 0) {
                    finish();
                    return;
                } else if (adapter.getList().size() == 1) {//删完只剩一个了
                    Current = 0;
                    position = Current;
                } else if (Current >= (adapter.getList().size() - 1)) {//删的是最后一个
                    Current = adapter.getList().size() - 1;
                    position = Current;
                } else {//后面还有
                    position = Current;
                }
                final int finalPosition = position;
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        viewpager.setAdapter(adapter);
                        viewpager.setCurrentItem(finalPosition);
                    }
                }, 300);
            } else if (adapter.getList().size() == 1) {//只剩一个可以删
                Order order = adapter.getList().get(position);
                adapter.remove(order.getId());
                adapter.notifyDataSetChanged();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        viewpager.setAdapter(adapter);
                        finish();
                    }
                }, 300);
            }
            setPage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        adapter = new MyAdapter(this, linkedList);
        viewpager = findViewById(R.id.viewpager);
        viewpager.setOffscreenPageLimit(3);
        viewpager.setAdapter(adapter);
        viewpager.setCurrentItem(0);
        viewpager.setPageMargin(30);
        viewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                //有上滑删除得自己维护Current,页面切换的时候要重置底部Button
                Current = i;
                playOrder();
                setPage();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    private void playOrder() {
        stopGrogressBar();
        progressNow = 0;
        progressBar.setMax((int) LOOP_TIME);
        progressBarTimer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressNow > 15000){
                            progressNow = 15000;
                            Log.i("TAG", progressNow+"");
                        }else {
                            progressNow = progressNow + 15;
                            Log.i("TAG", progressNow+"");
                        }
                    }
                });
            }
        };
        progressBarTimer.schedule(timerTask, 15, 15);
    }

    //停止进度条
    private void stopGrogressBar() {
        try {
            if (progressBarTimer != null) {
                progressBarTimer.cancel();
            }
            if (timerTask != null) {
                timerTask.cancel();
            }
        } catch (Exception e) {

        }
    }
}
