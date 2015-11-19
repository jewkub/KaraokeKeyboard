package com.onekeyboard.dict;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;

import com.onekeyboard.ime.R;
import android.content.Context;
import android.util.Log;
import jni.marisa.swig.Agent;
import jni.marisa.swig.Keyset;
import jni.marisa.swig.Trie;

public class Dictionary {
	private static final String TAG = "Dictionary";
	
	private Context mContext;
	private Trie mTrie;

	private Hashtable<String, Integer> 				mUnigramThai;
	private Hashtable<String, LinkedList<UnigramStr>> 	mMapRomanized2Thai;

	/** Constructor */
	public Dictionary(Context context, File dicPath){	
		mContext = context;
		
		// Load Romanized Unigram (Marisa Tree)
		mTrie = new Trie();
		Log.d(TAG, "dicPath : " + dicPath.getAbsolutePath());
		mTrie.load(dicPath.getAbsolutePath());
		
		// Load Thai Unigram (Text File)		# 1-1
		mUnigramThai  =  new Hashtable<String, Integer>();
		InputStream inptTUnigram = mContext.getResources().openRawResource(R.raw.royal_best_tfreq);
		Log.d(TAG, "BEGIN PARSE TFREQ");
		try {
			BufferedReader buffTUnigram = new BufferedReader(new InputStreamReader(inptTUnigram, "UTF-8"));
	        String dLine;
			while ((dLine = buffTUnigram.readLine()) != null) {	 
				String[] line_split = dLine.split("\\|");
				assert(line_split.length == 2);
				mUnigramThai.put(line_split[0], new Integer(line_split[1]));
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "END PARSE TFREQ");
		
		// Load Romanized2Thai Map (Text File)  # 1-many
		mMapRomanized2Thai = new Hashtable<String, LinkedList<UnigramStr>>();
		InputStream inptR2TMap = mContext.getResources().openRawResource(R.raw.royal_best_revmap);
		Log.d(TAG, "BEGIN PARSE R2TMap");
		try {
			BufferedReader buffR2TMap = new BufferedReader(new InputStreamReader(inptR2TMap, "UTF-8"));
	        String dLine;
			while ((dLine = buffR2TMap.readLine()) != null) {	 
				String[] line_split = dLine.split("\\|");
				assert(line_split.length == 2);
				
				String[] thai_script = line_split[1].split(",");
				LinkedList<UnigramStr> tscript_unigram = new LinkedList<UnigramStr>();
				for(int i = 0; i < thai_script.length; i++){
					tscript_unigram.add(new UnigramStr(thai_script[i], mUnigramThai.get(thai_script[i])));
				}
				mMapRomanized2Thai.put(line_split[0], tscript_unigram);	
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "END PARSE R2TMap");
	}
	
	public LinkedList<UnigramStr> getCandidates(String composing){
		LinkedList<UnigramStr> candidateStr = new LinkedList<UnigramStr>();

		Agent  agent = new Agent();

		Keyset keyset = new Keyset();
		agent.set_query(composing.toLowerCase().getBytes());
		while(mTrie.predictive_search(agent)){
			keyset.push_back(agent.key().str().getBytes());
		}
		
		for(int i = 0; i < keyset.size(); i++){	
			candidateStr.addAll(candidateStr.size(), mMapRomanized2Thai.get(keyset.key_str(i)));
		}
		Collections.sort(candidateStr, Collections.reverseOrder());
		Log.d(TAG, "candidateStr.size() = "+ candidateStr.size());
		
		return candidateStr;
	}
	 
	
    /** static constructor */
    static {
    	try {
    		System.loadLibrary("marisa");
    		System.loadLibrary("marisa-swig_java");
    	} catch (UnsatisfiedLinkError ule) {
    		Log.e(TAG, "******** Could not load native library nativeim ********");
    		Log.e(TAG, "******** Could not load native library nativeim ********", ule);
    		Log.e(TAG, "******** Could not load native library nativeim ********");
    	} catch (Throwable t) {
    		Log.e(TAG, "******** Failed to load native dictionary library ********");
    		Log.e(TAG, "******** Failed to load native dictionary library *******", t);
    		Log.e(TAG, "******** Failed to load native dictionary library ********");
    	}
    }
}
