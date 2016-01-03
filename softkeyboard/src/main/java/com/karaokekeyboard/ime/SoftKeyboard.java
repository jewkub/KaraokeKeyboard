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

package com.karaokekeyboard.ime;

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
import android.widget.ProgressBar;

import com.karaokekeyboard.dict.Dictionary;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private CandidateView           mCandidateView;
    public void closeProgressBar(){
        if(mCandidateView != null){
            //setCandidatesViewShown(false);
            mCandidateView.closeProgressBar();
            //setCandidatesViewShown(true);
        }
    }
    private CompletionInfo[] 		mCompletions;

    private StringBuilder 			mComposing = new StringBuilder();
    private class KaraokeWord {
        StringBuilder consonant, vowel, terminal;
        public boolean isVowel, isFinalFilled;
        public KaraokeWord(){
            consonant = new StringBuilder();
            vowel = new StringBuilder();
            terminal = new StringBuilder();
            isVowel = false;
            isFinalFilled = false;
        }
        @Override public String toString() {
            return consonant.toString() + " + " + vowel.toString() + " + " + terminal.toString();
        }
    }
    private List<KaraokeWord>       mWordsDecomposing = new ArrayList<KaraokeWord>(){
        @Override public KaraokeWord get(int location){
            if(location == -1) return super.get(size() - 1);
            return super.get(location);
        }
        public KaraokeWord getLast(){
            return get(size()-1);
        }
    };
    private List<List<String>>      mPhonetic = new ArrayList<List<String>>(12);
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

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        Log.d("CallBack", "onCreate!");
        super.onCreate();

        /**
         * Initialize mWordDecomposing and mPhonetic
         */
        mWordsDecomposing.add(new KaraokeWord());
        for(int i = 0; i < 12; i++) {
            mPhonetic.add(new ArrayList<String>());
        }

        /*
         * NATIVE-JNI cannot easily access file either in assets/ or res/raw.
         * 
         * For the time being, the problem is solved by copying those file to 
         * Internal Memory.
         * */

        mCandidateView = null;
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators 	= getResources().getString(R.string.word_separators);
        mDictionary = new Dictionary(this, this);
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        Log.d("CallBack", "onInitializeInterface!");
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
        Log.d("CallBack", "onCreateInputView!");
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        Log.d("CallBack", "onCreateCandidateView!");
        mCandidateView = (CandidateView) getLayoutInflater().inflate(R.layout.candidate_view, null);
        mCandidateView.init(this);
        Log.d("CallBack", "ended");
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        Log.d("CallBack", "onStartInput! with restarting = " + restarting);
        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        refresh();

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
        Log.d("CallBack", "onFinishInput!");
        super.onFinishInput();

        // Clear current composing text and candidates.
        refresh();

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
        Log.d("CallBack", "onStartInputView!");
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        //mInputView.closing();
        setCandidatesViewShown(false);
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
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
            refresh();
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    /*@Override public void onDisplayCompletions(CompletionInfo[] completions) {
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
    }*/

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
        Log.d("CallBack", "onKeyDown!");
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
        Log.d("CallBack", "onKeyUp!");
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        if(keyCode == KeyEvent.KEYCODE_BACK) handleClose();
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
        Log.d("CallBack", "onKey!");
        for (List<String> l : mPhonetic) {
            l.clear();
        }
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
            mWordsDecomposing.clear();
            mWordsDecomposing.add(new KaraokeWord());
            for(int i = 0; i < mComposing.length(); i++) {
                if(!isSyllableSeparator(mComposing.charAt(i))) wordsDecomposing(mComposing.charAt(i));
                else mWordsDecomposing.add(new KaraokeWord());
            }
            getPhonetics(0);
            updateCandidates();
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
        Log.d("CallBack", "onText!");
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
                List<List<String>> s = matchAll(mPhonetic);
                Log.d(TAG, "mPhonetic = " + mPhonetic.toString());
                List<String> candidateList = new LinkedList<String>();
                for(List<String> l : s){
                    Log.d(TAG, "phonetic = " + l.toString());
                    List<String> subCandidate = mDictionary.getCandidate(l);
                    if(subCandidate != null) candidateList.addAll(subCandidate);
                    else Log.d(TAG, "zzz");
                }
                Log.d(TAG, "found, " + candidateList.toString());
                setSuggestions(candidateList);
            } else {
                setSuggestions(new LinkedList<String>());
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
        if(suggestions == null) suggestions = new LinkedList<String>();
        //Log.d(TAG, "size = " + suggestions.size());
        if (!suggestions.isEmpty() || true) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            Log.d(TAG, "fullscreen = " + isFullscreenMode());
            setCandidatesViewShown(true);
        } else setCandidatesViewShown(false);

        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions);
        }
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            //updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            //updateCandidates();
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
        if (Character.isLetter(primaryCode) && mPredictionOn) {
            mComposing.append(primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            wordsDecomposing(primaryCode);
            Log.d(TAG, "mworddecomposing = " + mWordsDecomposing.toString());
            getPhonetics(0);
            updateCandidates();
        } else if(isSyllableSeparator(primaryCode) && mPredictionOn) {
            mComposing.append(primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            mWordsDecomposing.add(new KaraokeWord());
        } else {
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            refresh();
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
            //getCurrentInputConnection().commitText(String.valueOf(primaryCode), 1);
        }
    }

    private void handleClose() {
        refresh();
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        setCandidatesViewShown(false);
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

    /**
     * Check the character whether it is consonant or not.
     */
    private boolean isConsonant(char code) {
        return !(code == 'a' || code == 'e' || code == 'i' || code == 'o' || code == 'u');
    }

    /**
     * Composes the words into initial consonants, vowel, and final consonants.
     */
    private void wordsDecomposing(char code) {
        code = Character.toLowerCase(code);
        if (code == 'r') {
            if (mWordsDecomposing.get(-1).isVowel) {                      //-ar, -or
                mWordsDecomposing.get(-1).vowel.append(code);      //'r' is vowel.
            } else {
                mWordsDecomposing.get(-1).consonant.append(code);      //'r' is initial consonant.
            }
        }
        else if (isConsonant(code)) {               //Consonant cases
            if (mWordsDecomposing.get(-1).isVowel) {
                mWordsDecomposing.get(-1).terminal.append(code);      //Final
            } else {
                mWordsDecomposing.get(-1).consonant.append(code);      //Initial
            }
        } else {                                  //Vowel cases
            if(mWordsDecomposing.get(-1).isVowel && mWordsDecomposing.get(-1).terminal.length() > 0) throw new UnsupportedOperationException("Vowel after consonant is not allowed right now.");
            mWordsDecomposing.get(-1).vowel.append(code);
            mWordsDecomposing.get(-1).isVowel = true;                     //Mark that vowel is found.
        }
    }

    /**
     * Compare two Strings
     */
    private boolean isEqual(int syllable, int type, String secondKey) {
        KaraokeWord z = mWordsDecomposing.get(syllable);
        switch(type){
            case 0 : return z.consonant.toString().equals(secondKey);
            case 1 : return z.vowel.toString().equals(secondKey);
            case 2 : return z.terminal.toString().equals(secondKey);
        }
        throw new IllegalArgumentException("type cannot be others except 1, 2 and 3");
    }

    /**
     * Convert from words to phonetics
     */
    private void getPhonetics(int syllable) {
        //Initial consonant
        if (mWordsDecomposing.get(syllable).consonant.length() == 0) {
            mPhonetic.get(3 * syllable + 0).add("z");
        } else if (isEqual(syllable, 0, "y")) {
            mPhonetic.get(3 * syllable + 0).add("j");
        } else if (isEqual(syllable, 0, "j")) {
            mPhonetic.get(3 * syllable + 0).add("c");
        } else if (isEqual(syllable, 0, "kr")) {
            mPhonetic.get(3 * syllable + 0).add("kr");
            mPhonetic.get(3 * syllable + 0).add("khr");
        } else if (isEqual(syllable, 0, "t") || isEqual(syllable, 0, "th")) {
            mPhonetic.get(3 * syllable + 0).add("th");
            mPhonetic.get(3 * syllable + 0).add("t");
        } else if (isEqual(syllable, 0, "p") || isEqual(syllable, 0, "ph")) {
            mPhonetic.get(3 * syllable + 0).add("p");
            mPhonetic.get(3 * syllable + 0).add("ph");
        } else if (isEqual(syllable, 0, "pr")) {
            mPhonetic.get(3 * syllable + 0).add("pr");
            mPhonetic.get(3 * syllable + 0).add("phr");
        } else if (isEqual(syllable, 0, "k")){
            mPhonetic.get(3 * syllable + 0).add("k");
            mPhonetic.get(3 * syllable + 0).add("kh");
        } else if (isEqual(syllable, 0, "kh")){
            mPhonetic.get(3 * syllable + 0).add("k");
            mPhonetic.get(3 * syllable + 0).add("kh");
        } else {
            mPhonetic.get(3*syllable + 0).add(mWordsDecomposing.get(syllable).consonant.toString());
        }
        //Vowel
        if (isEqual(syllable, 1, "a")) {              //-a
            mPhonetic.get(3*syllable + 1).add("a");
        } else if (isEqual(syllable, 1, "aa")) {      //-aa
            mPhonetic.get(3*syllable + 1).add("aa");
        } else if (isEqual(syllable, 1, "e")) {       //short & long -e
            mPhonetic.get(3*syllable + 1).add("e");
            mPhonetic.get(3*syllable + 1).add("ee");
        } else if (isEqual(syllable, 1, "i")) {       //short -i
            mPhonetic.get(3*syllable + 1).add("i");
        } else if (isEqual(syllable, 1, "ee")) {      //-ee -> -ii
            mPhonetic.get(3*syllable + 1).add("ii");
        } else if (isEqual(syllable, 1, "oo")) {      //-oo, -uu
            mPhonetic.get(3*syllable + 1).add("oo");
            mPhonetic.get(3*syllable + 1).add("uu");
        } else if (isEqual(syllable, 1, "o")) {       //-o
            mPhonetic.get(3*syllable + 1).add("o");
        } else if (isEqual(syllable, 1, "u")) {       //-u
            mPhonetic.get(3*syllable + 1).add("u");
        } else if (isEqual(syllable, 1, "uu")) {      //-uu
            mPhonetic.get(3*syllable + 1).add("uu");
        } else if (isEqual(syllable, 1, "oe")) {      //short &long -oe
            mPhonetic.get(3*syllable + 1).add("q");
            mPhonetic.get(3*syllable + 2).add("qq");
        } else if (isEqual(syllable, 2, "ur")) {      //long -oe
            mPhonetic.get(3*syllable + 2).add("qq");
        } else if (isEqual(syllable, 2, "er")) {      //long -oe
            mPhonetic.get(3*syllable + 2).add("qq");
        } else if (isEqual(syllable, 1, "ae")) {      //short & long -ae
            mPhonetic.get(3*syllable + 1).add("x");
            mPhonetic.get(3*syllable + 1).add("xx");
        } else if (isEqual(syllable, 1, "ue")) {      //short & long -ue
            mPhonetic.get(3*syllable + 1).add("v");
            mPhonetic.get(3*syllable + 1).add("vv");
        } else if (isEqual(syllable, 1, "or")) {      //short & long -or
            mPhonetic.get(3*syllable + 1).add("@");
            mPhonetic.get(3*syllable + 1).add("@@");
        } else if (isEqual(syllable, 1, "uea")) {     //-uea -> -vva
            mPhonetic.get(3*syllable + 1).add("vva");
        } else if (isEqual(syllable, 1, "ua")) {      //-ua -> -uua
            mPhonetic.get(3*syllable + 1).add("ua");
            mPhonetic.get(3*syllable + 1).add("uua");
        } else if (isEqual(syllable, 1, "ia")) {      //short & long -ia
            mPhonetic.get(3*syllable + 1).add("ia");
            mPhonetic.get(3*syllable + 1).add("iia");
        } else if (isEqual(syllable, 1, "iao")) {     //-iao -> -iia + w
            mPhonetic.get(3*syllable + 1).add("iia");
            mPhonetic.get(3*syllable + 2).add("w");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "ai")) {      //-ai -> -a + j
            mPhonetic.get(3*syllable + 1).add("a");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "oi")) {      //-oi -> -@ + j
            mPhonetic.get(3*syllable + 1).add("@");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "oy")) {      //-oy -> -@@ + j
            mPhonetic.get(3*syllable + 1).add("@@");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "ui")) {      //-ui -> -u + j
            mPhonetic.get(3*syllable + 1).add("u");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "ao")) {      //-ao -> -a + w
            mPhonetic.get(3*syllable + 1).add("a");
            mPhonetic.get(3*syllable + 2).add("w");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "ueai")) {    //-ueai -> -vva + j
            mPhonetic.get(3*syllable + 1).add("vva");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "aeo")) {     //-aeo -> -xx + w
            mPhonetic.get(3*syllable + 1).add("xx");
            mPhonetic.get(3*syllable + 2).add("w");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "oei")) {     //-oei -> -qq + j
            mPhonetic.get(3*syllable + 1).add("qq");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 1, "uai")) {     //-uai -> -uua + j
            mPhonetic.get(3*syllable + 1).add("uua");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        } else if (isEqual(syllable, 2, "uay")) {      //long -oe
            mPhonetic.get(3*syllable + 2).add("uua");
            mPhonetic.get(3*syllable + 2).add("j");
            mWordsDecomposing.get(syllable).isFinalFilled = true;
        }
        //Final consonants
        if (mWordsDecomposing.get(syllable).isFinalFilled)          //If final consonants are filled, do nothing.
            return ;
        if (mWordsDecomposing.get(syllable).terminal.length() == 0) {
            mPhonetic.get(3*syllable + 2).add("z");
        } else if(isEqual(syllable, 2, "y")) {
            mPhonetic.get(3*syllable + 2).add("j");
        } else if(isEqual(syllable, 2, "d")) {
            mPhonetic.get(3*syllable + 2).add("t");
        } else if(isEqual(syllable, 2, "b")) {
            mPhonetic.get(3*syllable + 2).add("p");
            mPhonetic.get(3*syllable + 2).add("f");
        } else if(isEqual(syllable, 2, "f")) {
            mPhonetic.get(3*syllable + 2).add("b");
            mPhonetic.get(3*syllable + 2).add("f");
        } else if(isEqual(syllable, 2, "p")) {
            mPhonetic.get(3*syllable + 2).add("b");
            mPhonetic.get(3*syllable + 2).add("f");
        } else if(isEqual(syllable, 2, "g")) {
            mPhonetic.get(3*syllable + 2).add("k");
        } else if(isEqual(syllable, 2, "d")) {
            mPhonetic.get(3*syllable + 2).add("t");
        } else {
            mPhonetic.get(3*syllable + 2).add(mWordsDecomposing.get(syllable).terminal.toString());
        }
        //Log.d(TAG, mPhonetic.get(0).toString() + mPhonetic.get(1).toString() + mPhonetic.get(2).toString());
        if(syllable+1 < mWordsDecomposing.size()) getPhonetics(syllable+1);
    }

    public boolean isSyllableSeparator(char code) {
        return mWordSeparators.contains(String.valueOf((char)code));
    }

    public void commitSuggestion(String chosen_string){
        InputConnection ic = getCurrentInputConnection();
        ic.commitText(chosen_string, 1);
        refresh();
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

    private void refresh(){
        mComposing.setLength(0);
        mWordsDecomposing.clear();
        mWordsDecomposing.add(new KaraokeWord());
        InputConnection ic = getCurrentInputConnection();
        if(ic != null) ic.finishComposingText();
        //updateCandidates();
    }

    public void swipeRight() {
        /*if (mCompletionOn) {
            pickDefaultCandidate();
        }*/
    }

    public void swipeLeft() {
        //handleBackspace();
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
