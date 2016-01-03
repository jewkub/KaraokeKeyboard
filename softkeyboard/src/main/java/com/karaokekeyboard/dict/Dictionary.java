package com.karaokekeyboard.dict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;

import com.karaokekeyboard.ime.CandidateView;
import com.karaokekeyboard.ime.R;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.karaokekeyboard.ime.SoftKeyboard;
import com.karaokekeyboard.ime.trie.*;

public class Dictionary{
	private static final String TAG = "Dictionary";
	//private static String[] composing;
	private Context mContext;
	private Trie mTrie;
	private Handler mHandler = new Handler();
	private int mProgress = 0;
	public boolean finished = false;

	/** Constructor */
	public Dictionary(Context context, final SoftKeyboard s){
		finished = false;
		mContext = context;
		mTrie = new Trie();
		new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream input = mContext.getResources().openRawResource(R.raw.packed_dict);
				Log.d(TAG, "BEGIN PARSE DICT");
				try {
					BufferedReader buffTUnigram = new BufferedReader(new InputStreamReader(input, "UTF-8"));
					String dLine;
					while ((dLine = buffTUnigram.readLine()) != null) {
						String[] line_split = dLine.split("\\t");
						if(line_split.length == 1) Log.d(TAG, dLine);
						String[] phonetic = line_split[1].split("-| |\\|");
						mTrie.addPhonetic(Arrays.asList(phonetic), line_split[0]);
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								mProgress++;
								//progress.setProgress(mProgress);
							}
						});
					}
					mHandler.post(new Runnable(){
						public void run(){
							finished = true;
							if(s != null) s.closeProgressBar();
						}
					});
					Log.d(TAG, "END PARSE DICT");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (NullPointerException e){
					e.printStackTrace();
				}
				String test[] = {"k", "o", "t"};
				Log.d(TAG, "kot found, " + getCandidate(Arrays.asList(test)).toString());
			}
		}).start();
	}
	public List<String> getCandidate(List<String> composing){
		return mTrie.getWord(composing);
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
