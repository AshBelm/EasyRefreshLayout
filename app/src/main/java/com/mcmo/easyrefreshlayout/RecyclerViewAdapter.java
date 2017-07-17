package com.mcmo.easyrefreshlayout;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by ZhangWei on 2017/5/28.
 */

public abstract class RecyclerViewAdapter<VH extends RecyclerView.ViewHolder> {
    private final int VIEWTYPE_LOADMORE = 1213;
    private LoadMoreScrollListener listener = null;
    private View loadMoreView;
    private TextView tv;
    private OnLoadMoreListener mListener;

    public void setOnLoadMoreListener(OnLoadMoreListener mListener) {
        this.mListener = mListener;
    }
    public void loadMoreOver(){
        listener.isLoading=false;
    }
    public void notifyDataSetChanged(){
        adapter.notifyDataSetChanged();
    }
    private RecyclerView.Adapter<LoadMoreHolder> adapter = new RecyclerView.Adapter<LoadMoreHolder>() {
        @Override
        public LoadMoreHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (listener == null && parent instanceof RecyclerView) {
                listener = new LoadMoreScrollListener();
                ((RecyclerView) parent).addOnScrollListener(listener);
            }
            LoadMoreHolder holder ;
            if(viewType==VIEWTYPE_LOADMORE){
                loadMoreView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_loadmore,parent,false);
                holder =new LoadMoreHolder(loadMoreView);
                tv= (TextView) loadMoreView.findViewById(R.id.tv_loadmore);
            }else{
                VH vh = RecyclerViewAdapter.this.onCreateViewHolder(parent, viewType);
                holder = new LoadMoreHolder(vh.itemView);
                holder.setVh(vh);
            }
            return holder;
        }

        @Override
        public void onBindViewHolder(LoadMoreHolder holder, int position) {
            if(position==getItemCount()-1){

            }else{
                RecyclerViewAdapter.this.onBindViewHolder(holder.vh,position);
            }
        }

        @Override
        public int getItemCount() {
            int item = RecyclerViewAdapter.this.getItemCount();
            return item == 0 ? 0 : item + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) {
                return VIEWTYPE_LOADMORE;
            } else {
                int type= RecyclerViewAdapter.this.getItemViewType(position);
                if(type==VIEWTYPE_LOADMORE){
                    throw new IllegalArgumentException("view type "+VIEWTYPE_LOADMORE+" cannot use");
                }
                return type;
            }
        }
    };

    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    public abstract void onBindViewHolder(VH holder, int position);

    public abstract int getItemCount();

    public int getItemViewType(int position) {
        return 0;
    }
    public RecyclerView.Adapter<LoadMoreHolder> adapter(){
        return adapter;
    }

    private class LoadMoreScrollListener extends RecyclerView.OnScrollListener {
        private int prevState = RecyclerView.SCROLL_STATE_IDLE;
        private boolean isLoading;
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            switch (newState){
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    break;
                case RecyclerView.SCROLL_STATE_SETTLING:
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                    if(!isLoading&&mListener!=null){
                        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                        if(layoutManager instanceof LinearLayoutManager){
                            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                            int lastPosition = linearLayoutManager.findLastVisibleItemPosition();
                            int firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
                            int lastCompletePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                            if(lastPosition==adapter.getItemCount()-1){
                                isLoading=true;
                                mListener.onLoadMore();
                            }
                        }
                    }
                    break;
            }
            prevState = newState;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

        }
    }
    private class LoadMoreHolder extends RecyclerView.ViewHolder{
        private VH vh;

        public void setVh(VH vh) {
            this.vh = vh;
        }

        public LoadMoreHolder(View itemView) {
            super(itemView);
        }
    }
    public interface OnLoadMoreListener{
        void onLoadMore();
    }
}
