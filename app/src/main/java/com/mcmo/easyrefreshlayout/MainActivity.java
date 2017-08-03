package com.mcmo.easyrefreshlayout;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mcmo.easyrefreshlayout.library.EasyRefreshLayout;
import com.mcmo.easyrefreshlayout.library.impl.EasyRefreshListener;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rv;
    private EasyRefreshLayout erlayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rv = (RecyclerView) findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        final RVAdapter adapter =new RVAdapter();
//        adapter.setOnLoadMoreListener(new RecyclerViewAdapter.OnLoadMoreListener() {
//            @Override
//            public void onLoadMore() {
//                count+=15;
//                Log.e("aaaaa", "onLoadMore "+count);
//                adapter.notifyDataSetChanged();
//                adapter.loadMoreOver();
//            }
//        });
        rv.setAdapter(adapter);
        erlayout= (EasyRefreshLayout) findViewById(R.id.easyLayout);
        erlayout.addHeaderView(new HeaderChair());
        erlayout.addFooterView(new FooterText());
//        erlayout.setLoadMoreEnable(true);
//        erlayout.setRefreshEnable(true);
        erlayout.setEasyRefreshListener(new EasyRefreshListener() {
            @Override
            public void onRefresh(View v) {
               //上拉刷新
               // erlayout.dismissRefresh();消失刷新动画
                Log.e("aa", "onRefresh");
                erlayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        erlayout.dismissRefresh();
                    }
                },3000);
            }

            @Override
            public void onLoadMore(View v) {
                //下拉加载
                //erlayout.dismissLoadMore();消失加载更多动画
                Log.e("aa", "onLoadMore");
                erlayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        erlayout.dismissLoadMore();
                        count += 10;
                        adapter.notifyDataSetChanged();
                    }
                },3000);
            }
        });
//        findViewById(R.id.tv_click).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(MainActivity.this, "click", Toast.LENGTH_SHORT).show();
//            }
//        });
    }
    private int count =65;
    private class RVAdapter extends RecyclerView.Adapter<RVHodler>{

        @Override
        public RVHodler onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RVHodler(LayoutInflater.from(MainActivity.this).inflate(R.layout.item,null));
        }

        @Override
        public void onBindViewHolder(RVHodler holder, int position) {
//            holder.itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    ViewGroup vp = (ViewGroup) findViewById(R.id.llay);
//                    TextView tv = new TextView(MainActivity.this);
//                    tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//                    vp.addView(tv);
//                }
//            });
            if(position==getItemCount()-1){
                holder.tv.setText("end");
            }else{
                holder.tv.setText(""+position);
            }
        }

        @Override
        public int getItemCount() {
            return count;
        }
    }

    private class RVHodler extends RecyclerView.ViewHolder{
        private TextView tv;
        public RVHodler(View itemView) {
            super(itemView);
            tv= (TextView) itemView.findViewById(R.id.tv);

        }
    }

}
