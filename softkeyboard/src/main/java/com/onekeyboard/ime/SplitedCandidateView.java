package com.onekeyboard.ime;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.meetme.android.horizontallistview.HorizontalListView;
import com.onekeyboard.dict.UnigramStr;

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
    private ArrayAdapter<UnigramStr>		mSelectedCandidateAdapter;
    
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
        mSelectedCandidateAdapter = new ArrayAdapter<UnigramStr>(mContext, R.layout.initial_candidate_textview, R.id.initial_text);
        
        mSelectedInitialGridView.setAdapter(mSelectedCandidateAdapter);
        
        mSelectedInitialGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
				UnigramStr unigram = (UnigramStr) adapter.getItemAtPosition(position);
				
				mParent.commitSuggestion(unigram.getString());
				//Log.d(TAG, unigram.getString());
			}
		});

        mSelectedInitialGridView.setVisibility(View.INVISIBLE);
	}
	

	public void setSuggestions(List<UnigramStr> suggestions){
		setSuggestions(suggestions, true);
	}

	/*
	 * isSorted indicated whether suggestions has been sorted from high->low.
	 * */
	public void setSuggestions(List<UnigramStr> suggestions, boolean isSorted){
		mInitialCandidateAdapter.clear();
		mSelectedInitialGridView.setVisibility(View.INVISIBLE);

		
		LinkedList<CandidateGroup> currentGroups = new LinkedList<CandidateGroup>();
		
		for(int i = 0; i < suggestions.size(); i++){
			UnigramStr cur_suggestion = suggestions.get(i);

			// Make sure to SKIP empty candidate !!!!
			if(cur_suggestion.getString().isEmpty()){
				continue;
			}

			// Find a Candidate Group
			CandidateGroup cg = null;
			for(int j = 0; j < currentGroups.size(); j++){
				if(currentGroups.get(j).shouldInclude(cur_suggestion)){
					cg = currentGroups.get(j);
					break;
				}
			}
			
			// If no group, create a new one
			// else add them to the related group
			if(cg == null){
				currentGroups.add(new CandidateGroup(cur_suggestion));
			}else{
				cg.add(cur_suggestion, !isSorted);
			}
		}
		
		for(int i = 0; i < currentGroups.size(); i++){
			mInitialCandidateAdapter.add(currentGroups.get(i));
		}
	}

}
