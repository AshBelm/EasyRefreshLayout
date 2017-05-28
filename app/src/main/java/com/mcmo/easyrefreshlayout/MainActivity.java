package com.mcmo.easyrefreshlayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mcmo.easyrefreshlayout.library.EasyRefreshLayout;
import com.mcmo.easyrefreshlayout.library.EasyRefreshListener;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rv;
    private EasyRefreshLayout erlayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        rv.setAdapter(new RVAdapter());
        erlayout= (EasyRefreshLayout) findViewById(R.id.easyLayout);
        erlayout.addHeaderView(new HeaderChair());
        erlayout.setEasyRefreshListener(new EasyRefreshListener() {
            @Override
            public void onRefresh(View v) {
               //上拉刷新
               // erlayout.dismissRefresh();消失刷新动画
            }

            @Override
            public void onLoadMore(View v) {
                //下拉加载
                //erlayout.dismissLoadMore();消失加载更多动画
            }
        });
    }

    private class RVAdapter extends RecyclerView.Adapter<RVHodler>{

        @Override
        public RVHodler onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RVHodler(LayoutInflater.from(MainActivity.this).inflate(R.layout.item,null));
        }

        @Override
        public void onBindViewHolder(RVHodler holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 40;
        }
    }

    private class RVHodler extends RecyclerView.ViewHolder{

        public RVHodler(View itemView) {
            super(itemView);

        }
    }
}
