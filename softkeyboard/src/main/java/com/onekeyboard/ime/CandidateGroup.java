package com.onekeyboard.ime;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import com.onekeyboard.dict.UnigramStr;

@SuppressLint("Assert")
public class CandidateGroup implements Comparable<CandidateGroup>{
	private char 					mGroupInitial;
	private int 					mTotalFreq  	= 0;
	private LinkedList<UnigramStr>  mCandidate		= new LinkedList<UnigramStr>();

	public CandidateGroup(UnigramStr unigram_str){
		assert(!unigram_str.getString().isEmpty());

		mGroupInitial = unigram_str.getStringInitial();
		this.add(unigram_str, false);
	}
	
	@Override
	public String toString() {
		return "" + mCandidate.get(0).getString() + " (" + mCandidate.size() + ")";
		//return "" + mGroupInitial;
	}
	
	public boolean shouldInclude(UnigramStr unigram_str){
		assert(!unigram_str.getString().isEmpty());

		return mGroupInitial == unigram_str.getStringInitial();
	}
	
	/*
	 * Sort of UnigramStr in mCandidate should be from high->low
	 * */
	public boolean add(UnigramStr unigram_str, boolean insert_sort){
		if(!shouldInclude(unigram_str)){
			return false;
		}else{
			int insert_idx = this.mCandidate.size();

			if(insert_sort){
				for(insert_idx = 0; insert_idx < this.mCandidate.size(); insert_idx++){
					if(unigram_str.compareTo(this.mCandidate.get(insert_idx)) > 0){
						break;
					}
				}
			}
			
			mCandidate.add(insert_idx, unigram_str);
			this.mTotalFreq += unigram_str.getFrequency();
			return true;
		}
	}

	@Override
	public int compareTo(CandidateGroup cg) {
		if(this.mTotalFreq > cg.mTotalFreq){
			return 1;
		}else if (this.mTotalFreq == cg.mTotalFreq){
			return 0;
		}else {
			return -1;
		}
	}
	
	public LinkedList<UnigramStr> getAllCandidates(){
		return this.mCandidate;
	}

}
