package com.karaokekeyboard.dict;

import java.util.Comparator;

public class UnigramStr implements Comparable<UnigramStr>{
	
	private static final int FIRST_NON_ALPHABET_THAI = 0x0E2F;
	private static final int LAST_NON_ALPHABET_THAI  = 0x0E5B;
	
	private String mStr;
	private int mFreq;
	
	public UnigramStr(String str, int freq){
		setString(str);
		setFrequency(freq);
	}

	@Override
	public String toString() {
		return "" + this.mStr + " (" + this.mFreq + ")";
		//return "" + mGroupInitial;
	}
	

	public int getFrequency() {
		return mFreq;
	}

	public void setFrequency(int mFreq) {
		this.mFreq = mFreq;
	}

	public String getString() {
		return mStr;
	}

	public void setString(String mStr) {
		this.mStr = mStr;
	}
	
	public char getStringInitial(){
		Character initial_c = null;
		for(int i = 0; i < this.mStr.length(); i++){
			char cur_c = this.mStr.charAt(i);
			if((int)cur_c < FIRST_NON_ALPHABET_THAI || (int)cur_c > LAST_NON_ALPHABET_THAI){
				initial_c = new Character(cur_c);
				break;
			}
		}
		return (initial_c == null)? this.mStr.charAt(0): initial_c;
	}

	@Override
	public int compareTo(UnigramStr ustr) {
		if(this.mFreq > ustr.mFreq){
			return 1;
		}else if (this.mFreq == ustr.mFreq){
			return 0;
		}else {
			return -1;
		}
	}
	
	public static class UnigramStrComparatorByString implements Comparator<UnigramStr>{

		@Override
		public int compare(UnigramStr arg0, UnigramStr arg1) {
			return arg0.getString().compareTo(arg1.getString());
		}
	}
};
