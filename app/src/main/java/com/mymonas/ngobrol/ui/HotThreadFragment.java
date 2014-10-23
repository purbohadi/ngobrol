package com.mymonas.ngobrol.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.mymonas.ngobrol.R;
import com.mymonas.ngobrol.io.RestClient;
import com.mymonas.ngobrol.io.model.ThreadCallback;
import com.mymonas.ngobrol.model.ThreadItem;
import com.mymonas.ngobrol.ui.adapter.ThreadAdapter;
import com.mymonas.ngobrol.util.Clog;

import java.util.ArrayList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class HotThreadFragment extends Fragment {
    private static final String ACTION_GET_ALL_THREADS = "get_all_threads";
    private Context mContext;
    private ArrayList<ThreadItem> mThreadList;
    private ThreadAdapter mThreadAdapter;

    public HotThreadFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mContext = getActivity();
        View view = inflater.inflate(R.layout.fragment_hot_thread, container, false);

        mThreadList = new ArrayList<ThreadItem>();
        mThreadAdapter = new ThreadAdapter(mContext, mThreadList);

        final ProgressBar pBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        pBar.setVisibility(View.VISIBLE);
        RestClient.get().getThreads(ACTION_GET_ALL_THREADS, new Callback<ThreadCallback>() {
            @Override
            public void success(ThreadCallback threadCallback, Response response) {
                pBar.setVisibility(View.GONE);
                mThreadList.addAll(threadCallback.getData());
                mThreadAdapter.notifyDataSetChanged();
            }

            @Override
            public void failure(RetrofitError error) {
                pBar.setVisibility(View.GONE);
                Clog.d(error.getCause().toString());
            }
        });

        ListView threadLv = (ListView) view.findViewById(R.id.thread_list);
        threadLv.setAdapter(mThreadAdapter);
        threadLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bundle args = new Bundle();
                args.putString("threadId", mThreadList.get(i).getId());

                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtras(args);
                startActivity(intent);
            }
        });

        return view;
    }
}