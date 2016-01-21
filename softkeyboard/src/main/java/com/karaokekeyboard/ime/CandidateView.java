package com.karaokekeyboard.ime;

import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class CandidateView extends LinearLayout{
    private static final String		TAG = "CandidateView";
	private ProgressBar				mProgressBar;
    private Context					mContext;
	private ArrayAdapter<String>	mGridViewAdapter;
    private SoftKeyboard			mParent;
	private GridView				mGridView;

	public CandidateView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public void init(SoftKeyboard parent){
		mParent = parent;

		// Get references to UI widgets
		mGridView = (GridView) findViewById(R.id.candidate_list);

        // Make an array adapter
		mGridViewAdapter = new ArrayAdapter<String>(mContext, R.layout.textview, R.id.text);

        mGridView.setAdapter(mGridViewAdapter);
        mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
				mParent.commitSuggestion((String) adapter.getItemAtPosition(position));
				mGridViewAdapter.clear();
			}
		});
        mGridView.setVisibility(View.VISIBLE);

		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		if(!parent.loadFinished) mProgressBar.setVisibility(View.VISIBLE);
	}

	public void setSuggestions(List<String> suggestions){
		mGridViewAdapter.clear();
		mGridViewAdapter.addAll(suggestions);
	}

	public void closeProgressBar(){
		mProgressBar.setVisibility(View.GONE);
		//mGridView.setLayoutParams(new LayoutParams(0, R.dimen.candidate_height, 1.5f));
	}
}
