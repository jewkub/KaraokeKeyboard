package com.karaokekeyboard.dict;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.karaokekeyboard.database.DatabaseContract;
import com.karaokekeyboard.database.DbHelper;
import com.karaokekeyboard.ime.R;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.util.Log;

import com.karaokekeyboard.ime.SoftKeyboard;
import com.karaokekeyboard.trie.*;

public class Dictionary{
	private static final String TAG = "Dictionary";
	//private static String[] composing;
	private Context mContext;
	private Trie mTrie;
	private Handler mHandler;

	/** Constructor */
	public Dictionary(final Context context, final SoftKeyboard s){
		mContext = context;
		mTrie = new Trie();
		mHandler = new Handler();
		new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream input = mContext.getResources().openRawResource(R.raw.packed_dict);
				Log.d(TAG, "BEGIN PARSE DICT");
				try {
					BufferedReader buffTUnigram = new BufferedReader(new InputStreamReader(input, "UTF-8"));
					String in1, in2;
					while ((in1 = buffTUnigram.readLine()) != null && (in2 = buffTUnigram.readLine()) != null) {
						//String[] line_split = dLine.split("\\t");
						//if(line_split.length == 1) Log.d(TAG, dLine);
						String[] phonetic = in2.split("\\|");
						mTrie.addPhonetic(Arrays.asList(phonetic), in1);
					}
				} catch (Exception e) {
					Log.w(TAG, e.toString());
					e.printStackTrace();
				}
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						if (s != null) s.closeProgressBar();
					}
				});
				Log.d(TAG, "END PARSE DICT");
				String test[] = {"k", "o", "t"};
				Log.d(TAG, "kot found, " + getCandidate(Arrays.asList(test)).toString());
			}
		}).start();
	}
	public List<String> getCandidate(List<String> composing){
		return mTrie.getWord(composing);
	}
}
