/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onekeyboard.ime;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.onekeyboard.dict.Dictionary;
import com.onekeyboard.dict.UnigramStr;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private static final String TAG = "SoftKeyboard";
    private static final String UNIGRAM_ROMANIZED_MARISA_FN = "royal_best_dic";
    //private static final String UNIGRAM_THAI_FN 			= "royal_best_tfreq";	// 1-1 		map
    //private static final String MAP_ROMANIZED2THAI_FN 		= "royal_best_revmap";	// 1-many 	map

    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;

    private InputMethodManager 		mInputMethodManager;

    private LatinKeyboardView 		mInputView;
    private SplitedCandidateView 	mCandidateView;
    private CompletionInfo[] 		mCompletions;

    private StringBuilder 			mComposing = new StringBuilder();
    private StringBuilder[]         mWordsDecomposing = new StringBuilder[5];
    private List<List<String>>      mPhonetic = new ArrayList<List<String>>();
    private boolean                 isVowel = false;
    private boolean                 isFinalFilled = false;
    private boolean 				mPredictionOn;
    private boolean 				mCompletionOn;
    private int	 					mLastDisplayWidth;
    private boolean 				mCapsLock;
    private long 					mLastShiftTime;
    private long 					mMetaState;

    private LatinKeyboard 			mSymbolsKeyboard;
    private LatinKeyboard 			mSymbolsShiftedKeyboard;
    private LatinKeyboard 			mQwertyKeyboard;

    private LatinKeyboard 			mCurKeyboard;

    private String 					mWordSeparators;

    private Dictionary 				mDictionary;
    private int tmp = 1;

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        File intMemDir 	= this.getFilesDir();
        File dicFile 	= new File(intMemDir, UNIGRAM_ROMANIZED_MARISA_FN);

        /**
         * Initialize mWordDecomposing[] and mPhonetics[]
         */
        for(int i = 0; i < 3; i++) {
            mWordsDecomposing[i] = new StringBuilder();
            mPhonetic.add(new ArrayList<String>());
            //mPhonetics[i] = new StringBuilder();
        }

        /*
         * NATIVE-JNI cannot easily access file either in assets/ or res/raw.
         * 
         * For the time being, the problem is solved by copying those file to 
         * Internal Memory.
         * */
        //boolean isFirstRun = true;

        SharedPreferences settings = getSharedPreferences("PREFS_NAME", 0);
        // isFirstRun = settings.getBoolean("FIRST_RUN", true);
        //if (isFirstRun) {

        Log.d(TAG, "Copying *.dic to Internal Memory.");
        // Copy *.dic to location where NATIVE-JNI can access them EASILY.
        InputStream ins = getResources().openRawResource(R.raw.royal_best_dic);
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();

        int size = 0;
        // Read the entire resource into a local byte buffer.
        byte[] readBuffer = new byte[1024];
        try {
            while((size=ins.read(readBuffer, 0, 1024)) >= 0){
                outputStream.write(readBuffer, 0, size);
            }
            ins.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        readBuffer=outputStream.toByteArray();

        try {
            FileOutputStream fos = new FileOutputStream(dicFile);
            fos.write(readBuffer);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        settings						 	= getSharedPreferences("PREFS_NAME", 0);
        SharedPreferences.Editor editor 	= settings.edit();
        editor.putBoolean("FIRST_RUN", false);
        editor.commit();
        //}

        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators 	= getResources().getString(R.string.word_separators);

        mDictionary = new Dictionary(this, dicFile);
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);

        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = (SplitedCandidateView) getLayoutInflater().inflate(R.layout.splited_candidate_view, null);

        mCandidateView.init(this);

        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mPredictionOn = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }

        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }

    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                            int newSelStart, int newSelEnd,
                                            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            //setSuggestions(stringList, true, true);
            setSuggestions(null);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        for (List<String> l : mPhonetic) {
            l.clear();
        }
        if (isWordSeparator(primaryCode)) {
            // Handle separator
            // Toast.makeText(SoftKeyboard.this, "test" + tmp++, Toast.LENGTH_SHORT).show();
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
            for(StringBuilder i : mWordsDecomposing)
                i.delete(0, i.length());
            isVowel = false;
            isFinalFilled = false;
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
            for(int i = 0; i < mWordsDecomposing.length; i++){
                mWordsDecomposing[i] = new StringBuilder();
            }
            isVowel = false;
            isFinalFilled = false;
            for(int i = 0; i < mComposing.length(); i++) {
                wordsDecomposing(mComposing.charAt(i));
            }
            getPhonetics();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                current = mQwertyKeyboard;
            } else {
                current = mSymbolsKeyboard;
            }
            mInputView.setKeyboard(current);
            if (current == mSymbolsKeyboard) {
                current.setShifted(false);
            }
        } else {
            handleCharacter((char) primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                /*Modify the candiate list here*/
                List<List<String>> s = new LinkedList<List<String>>();
                List<String> candidateList = new LinkedList<String>();
                s = matchAll(mPhonetic);
                if(s.isEmpty()) Log.d(TAG, "matchAll is empty = =");
                for(List<String> l : s){
                    Log.d(TAG, "phonetic = " + l.toString());
                    List<String> subCandidate = mDictionary.getCandidate(l);
                    if(subCandidate != null) candidateList.addAll(subCandidate);
                }
                Log.d(TAG, "found, " + candidateList.toString());
                if(!candidateList.isEmpty())setSuggestions(candidateList);
            } else {
                setSuggestions(null);
            }
        }
    }
    public List<List<String>> matchAll(List<List<String>> L){
        List<List<String>> sum = new LinkedList<List<String>>();
        List<List<String>> subList = new LinkedList<List<String>>(L);
        subList.remove(0);
        while(!subList.isEmpty() && subList.get(0).isEmpty()){
            subList.remove(0);
        }
        for (String s : L.get(0)) {
            if(subList.isEmpty()){
                List<String> zz = new LinkedList<String>();
                zz.add(s);
                sum.add(zz);
            }
            else {
                for (List<String> l : matchAll(subList)) {
                    l.add(0, s);
                    sum.add(l);
                }
            }
        }
        return sum;
    }
    public void setSuggestions(List<String> suggestions) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }

        if (mCandidateView != null && suggestions != null) {
            mCandidateView.setSuggestions(suggestions);
        }
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }

    private void handleCharacter(char primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append(primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            wordsDecomposing(primaryCode);
            Log.d(TAG, "1 = " + mWordsDecomposing[0].toString());
            Log.d(TAG, "2 = " + mWordsDecomposing[1].toString());
            Log.d(TAG, "3 = " + mWordsDecomposing[2].toString());
            getPhonetics();
            updateCandidates();
        } else {
            getCurrentInputConnection().commitText(String.valueOf(primaryCode), 1);
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    private String getWordSeparators() {
        return mWordSeparators;
    }

    /**
     * Check the character whether it is consonant or not.
     */
    private boolean isConsonant(char code) {
        if (code =='a' || code == 'e' || code == 'i' || code == 'o' || code == 'u')
            return false;
        return true;
    }

    /**
     * Composes the words into initial consonants, vowel, and final consonants.
     */
    private void wordsDecomposing(char code) {
        if (code == 'r') {
            if (isVowel) {                      //-ar, -or
                mWordsDecomposing[1].append("" + code);      //'r' is vowel.
            } else {
                mWordsDecomposing[0].append("" + code);      //'r' is initial consonant.
            }
        }
        else if (isConsonant(code)) {               //Consonant cases
            if (isVowel) {
                mWordsDecomposing[2].append("" + code);      //Final
            } else {
                mWordsDecomposing[0].append("" + code);      //Initial
            }
        } else {                                  //Vowel cases
            mWordsDecomposing[1].append("" + code);
            isVowel = true;                     //Mark that vowel is found.
        }
    }

    /**
     * Compare two Strings
     */
    private boolean isEqual(int type,String secondKey) {
        return mWordsDecomposing[type].toString().equals(secondKey);
    }

    /**
     * Convert from words to phonetics
     */
    private void getPhonetics() {
        //Initial consonant
        if (mWordsDecomposing[0].length() == 0) {
            mPhonetic.get(0).add("z");
        } else if (isEqual(0, "y")) {
            mPhonetic.get(0).add("j");
        } else {
            mPhonetic.get(0).add(mWordsDecomposing[0].toString());
        }
        //Vowel
        if (isEqual(1, "a")) {              //-a
            mPhonetic.get(1).add("a");
        } else if (isEqual(1, "aa")) {      //-aa
            mPhonetic.get(1).add("aa");
        } else if (isEqual(1, "e")) {       //short & long -e
            mPhonetic.get(1).add("e");
            mPhonetic.get(1).add("ee");
        } else if (isEqual(1, "i")) {       //short -i
            mPhonetic.get(1).add("i");
        } else if (isEqual(1, "ee")) {      //-ee -> -ii
            mPhonetic.get(1).add("ii");
        } else if (isEqual(1, "oo")) {      //-oo
            mPhonetic.get(1).add("oo");
        } else if (isEqual(1, "o")) {       //-o
            mPhonetic.get(1).add("o");
        } else if (isEqual(1, "u")) {       //-u
            mPhonetic.get(1).add("u");
        } else if (isEqual(1, "uu")) {      //-uu
            mPhonetic.get(1).add("uu");
        } else if (isEqual(1, "oe")) {      //short &long -oe
            mPhonetic.get(1).add("q");
            mPhonetic.get(1).add("qq");
        } else if (isEqual(1, "ae")) {      //short & long -ae
            mPhonetic.get(1).add("x");
            mPhonetic.get(1).add("xx");
        } else if (isEqual(1, "ue")) {      //short & long -ue
            mPhonetic.get(1).add("v");
            mPhonetic.get(1).add("vv");
        } else if (isEqual(1, "or")) {      //short & long -or
            mPhonetic.get(1).add("@");
            mPhonetic.get(1).add("@@");
        } else if (isEqual(1, "uea")) {     //-uea -> -vva
            mPhonetic.get(1).add("vva");
        } else if (isEqual(1, "ua")) {      //-ua -> -uua
            mPhonetic.get(1).add("uua");
        } else if (isEqual(1, "ia")) {      //short & long -ia
            mPhonetic.get(1).add("ia");
            mPhonetic.get(1).add("iia");
        } else if (isEqual(1, "iao")) {     //-iao -> -iia + w
            mPhonetic.get(1).add("iia");
            mPhonetic.get(2).add("w");
            isFinalFilled = true;
        } else if (isEqual(1, "ai")) {      //-ai -> -a + j
            mPhonetic.get(1).add("a");
            mPhonetic.get(2).add("j");
            isFinalFilled = true;
        } else if (isEqual(1, "oi")) {      //-oi -> -@@ + j
            mPhonetic.get(1).add("@@");
            mPhonetic.get(2).add("j");
            isFinalFilled = true;
        } else if (isEqual(1, "ui")) {      //-ui -> -u + j
            mPhonetic.get(1).add("u");
            mPhonetic.get(2).add("j");
            isFinalFilled = true;
        } else if (isEqual(1, "ao")) {      //-ao -> -a + w
            mPhonetic.get(1).add("a");
            mPhonetic.get(2).add("w");
            isFinalFilled = true;
        } else if (isEqual(1, "ueai")) {    //-ueai -> -vva + j
            mPhonetic.get(1).add("vva");
            mPhonetic.get(2).add("j");
            isFinalFilled = true;
        } else if (isEqual(1, "aeo")) {     //-aeo -> -xx + w
            mPhonetic.get(1).add("xx");
            mPhonetic.get(2).add("w");
            isFinalFilled = true;
        } else if (isEqual(1, "oei")) {     //-oei -> -qq + j
            mPhonetic.get(1).add("qq");
            mPhonetic.get(2).add("j");
            isFinalFilled = true;
        } else if (isEqual(1, "uai")) {     //-uai -> -uua + j
            mPhonetic.get(1).add("uua");
            mPhonetic.get(2).add("j");
            isFinalFilled = true;
        }
        //Final consonants
        if (isFinalFilled)          //If final consonants are filled, do nothing.
            return ;
        if (mWordsDecomposing[2].length() == 0) {
            mPhonetic.get(2).add("z");
        } else {
            mPhonetic.get(2).add(mWordsDecomposing[2].toString());
        }
        Log.d(TAG, mPhonetic.get(0).toString() + mPhonetic.get(1).toString() + mPhonetic.get(2).toString());
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    public void commitSuggestion(String chosen_string){
        InputConnection ic = getCurrentInputConnection();
        ic.commitText(chosen_string, 1);
        mComposing.setLength(0);
        updateCandidates();
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                //mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            /*
        	// If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        	*/

            InputConnection ic = getCurrentInputConnection();
            //ic.commitText(mCandidateView.getSuggestions(index), 1);
            mComposing.setLength(0);
            updateCandidates();

        }
    }

    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }

    public void onPress(int primaryCode) {
    }

    public void onRelease(int primaryCode) {
    }
}
