package com.onekeyboard.ime;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.meetme.android.horizontallistview.HorizontalListView;
//import com.onekeyboard.dict.UnigramStr;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;

public class SplitedCandidateView extends LinearLayout{
    private static final String 	TAG 						= "SplitedCandidateView";
    private Context			  		mContext;

    private HorizontalListView  		  	mInitialCandidateList;
    private ArrayAdapter<CandidateGroup>	mInitialCandidateAdapter;

    private GridView						mSelectedInitialGridView;
    private ArrayAdapter<String>			mSelectedCandidateAdapter;
    
    private SoftKeyboard					mParent;
	
	public SplitedCandidateView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public void init(SoftKeyboard parent){
		mParent = parent;

		// Get references to UI widgets
        mInitialCandidateList = (HorizontalListView) findViewById(R.id.initial_candidate_list);
        
        mSelectedInitialGridView = (GridView) findViewById(R.id.selected_initial_candidate_list);
        
		
        // Make an array adapter
        mInitialCandidateAdapter = new ArrayAdapter<CandidateGroup>(mContext, R.layout.initial_candidate_textview, R.id.initial_text);
        
        mInitialCandidateList.setAdapter(mInitialCandidateAdapter);
        
        mInitialCandidateList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
				CandidateGroup group = (CandidateGroup) adapter.getItemAtPosition(position); 
				
				mSelectedCandidateAdapter.clear();
				
				mSelectedCandidateAdapter.addAll(group.getAllCandidates());

				mSelectedInitialGridView.setVisibility(View.VISIBLE);
			}
        	
        });
        
        // Make an array adapter
        mSelectedCandidateAdapter = new ArrayAdapter<String>(mContext, R.layout.initial_candidate_textview, R.id.initial_text);
        
        mSelectedInitialGridView.setAdapter(mSelectedCandidateAdapter);
        
        mSelectedInitialGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
				mParent.commitSuggestion((String) adapter.getItemAtPosition(position));
				//Log.d(TAG, unigram.getString());
			}
		});

        mSelectedInitialGridView.setVisibility(View.INVISIBLE);
	}

	public void setSuggestions(List<String> suggestions){
		mInitialCandidateAdapter.clear();
		mSelectedInitialGridView.setVisibility(View.INVISIBLE);
		
		//LinkedList<CandidateGroup> currentGroups = new LinkedList<CandidateGroup>();
		CandidateGroup currentGroups = null;
		
		for(String i : suggestions){
			// Make sure to SKIP empty candidate !!!!
			if(i.isEmpty()){
				continue;
			}

			// Find a Candidate Group
			/*CandidateGroup cg = null;
			for(int j = 0; j < currentGroups.size(); j++){
				if(currentGroups.get(j).shouldInclude(i)){
					cg = currentGroups.get(j);
					break;
				}
			}
			// If no group, create a new one
			// else add them to the related group
			*/
			if(currentGroups == null){
				currentGroups = new CandidateGroup(i);
			}else{
				currentGroups.add(i);
			}
		}
		mInitialCandidateAdapter.add(currentGroups);
	}

}
