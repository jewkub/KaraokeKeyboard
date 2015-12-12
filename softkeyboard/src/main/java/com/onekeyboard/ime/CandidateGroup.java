package com.onekeyboard.ime;

import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import com.onekeyboard.dict.UnigramStr;

@SuppressLint("Assert")
public class CandidateGroup implements Comparable<CandidateGroup>{
	private char 					mGroupInitial;
	private int						mScore = 0;
	//private int 					mTotalFreq  	= 0;
	private LinkedList<String>  	mCandidate		= new LinkedList<String>();

	public CandidateGroup(String str){
		assert(!str.isEmpty());

		mGroupInitial = str.charAt(0);
		this.add(str);
	}
	
	/*@Override
	public String toString() {
		return "" + mCandidate.get(0).getString() + " (" + mCandidate.size() + ")";
		//return "" + mGroupInitial;
	}*/
	
	public boolean shouldInclude(String str){
		assert(!str.isEmpty());

		return mGroupInitial == str.charAt(0);
	}
	
	/*
	 * Sort of String in mCandidate should be from high->low
	 * */
	public boolean add(String str){
		//if(!shouldInclude(str)){
		//	return false;
		//}else{
			int insert_idx = this.mCandidate.size();

			/*if(insert_sort){
				for(insert_idx = 0; insert_idx < this.mCandidate.size(); insert_idx++){
					if(str.compareTo(this.mCandidate.get(insert_idx)) > 0){
						break;
					}
				}
			}*/
			
			mCandidate.add(insert_idx, str);
			//this.mTotalFreq += unigram_str.getFrequency();
			return true;
		//}
	}
	@Override
	public int compareTo(CandidateGroup cg) {
		if(this.mScore > cg.mScore){
			return 1;
		}else if (this.mScore == cg.mScore){
			return 0;
		}else {
			return -1;
		}
	}
	
	public LinkedList<String> getAllCandidates(){
		return this.mCandidate;
	}

}
