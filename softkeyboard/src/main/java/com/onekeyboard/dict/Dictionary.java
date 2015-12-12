package com.onekeyboard.dict;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;

import com.onekeyboard.ime.R;
import android.content.Context;
import android.util.Log;
import com.onekeyboard.ime.trie.*;

public class Dictionary {
	private static final String TAG = "Dictionary";
	//private static String[] composing;
	private Context mContext;
	private Trie mTrie;

	/** Constructor */
	public Dictionary(Context context, File dicPath){
		mContext = context;
		mTrie = new Trie();
		new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream inptTUnigram = mContext.getResources().openRawResource(R.raw.dict_part);
				Log.d(TAG, "BEGIN PARSE DICT");
				try {
					BufferedReader buffTUnigram = new BufferedReader(new InputStreamReader(inptTUnigram, "UTF-8"));
					String dLine;
					while ((dLine = buffTUnigram.readLine()) != null) {
						String[] line_split = dLine.split("\\t");
						if(line_split.length == 1) Log.d(TAG, dLine);
						String[] phonetic = line_split[1].split("-| |\\|");
				/*for(int i = 0; i < phonetic.length; i++){
					phonetic[i] = phonetic[i].replace("^", "");
				}*/
						//Log.d("Dictionary", "adding " + phonetic[0]);
						mTrie.addPhonetic(Arrays.asList(phonetic), line_split[0]);
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Log.d(TAG, "END PARSE DICT");
				String test[] = {"k", "o", "t"};
				Log.d(TAG, "kot found, " + getCandidate(Arrays.asList(test)).toString());
			}
		}).start();
	}
	public List<String> getCandidate(List<String> composing){
		return mTrie.getWord(composing);
	}
	/*public LinkedList<UnigramStr> getCandidates(String composing){
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
	}*/
	 
	
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
