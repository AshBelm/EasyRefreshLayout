package com.mcmo.easyrefreshlayout;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mcmo.easyrefreshlayout.library.EasyRefreshLayout;
import com.mcmo.easyrefreshlayout.library.impl.EasyRefreshListener;

public class ScrollingActivity extends AppCompatActivity {
    private static final String TAG = "ScrollingActivity";
    private RecyclerView rv;
    private MAdapter mAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MAdapter();
        rv.setAdapter(mAdapter);

        initRefresh();
    }

    private void initRefresh() {
        final EasyRefreshLayout erlayout= (EasyRefreshLayout) findViewById(R.id.easyLayout);
        erlayout.addHeaderView(new HeaderChair());
        erlayout.addFooterView(new FooterText());
        erlayout.setEasyRefreshListener(new EasyRefreshListener() {
            @Override
            public void onRefresh(View v) {
                Log.e(TAG, "onRefresh");
                erlayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        erlayout.dismissRefresh();
                    }
                },1000);
            }

            @Override
            public void onLoadMore(View v) {
                Log.e(TAG, "onLoadMore");
                erlayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        erlayout.dismissLoadMore();
                        mAdapter.data+=5;
                        mAdapter.notifyDataSetChanged();
                    }
                },3000);
            }
        });
//        erlayout.setRefreshJustSpring(true);
//        erlayout.setLoadMoreJustSpring(true);
//        erlayout.setRefreshEnable(false);

    }

    private class MAdapter extends RecyclerView.Adapter<MHolder>{


        @Override
        public MHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(ScrollingActivity.this);
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new MHolder(textView);
        }

        @Override
        public void onBindViewHolder(MHolder holder, int position) {
            Log.e(TAG, "onBindViewHolder + "+position);
            holder.tv.setText(position+"");
        }
        public int data = 120;
        @Override
        public int getItemCount() {
            return data;
        }
    }
    private class MHolder extends RecyclerView.ViewHolder{
        TextView tv;
        public MHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView;
        }
    }
}

