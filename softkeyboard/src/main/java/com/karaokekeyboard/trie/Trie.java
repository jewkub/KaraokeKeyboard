package com.karaokekeyboard.trie;

/**
 * This file was created by Jew on 11/30/2015.
 */

import android.util.Log;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Trie implements Serializable {
    public static final int SIZE = 39;
    public static final String TAG = "Dictionary";
    protected static final String COMMA = ",";
    private TrieNode root;

    /**
     * Constructor
     */
    public Trie()
    {
        root = new TrieNode();
    }

    /**
     * map the phoneme to integer
     * @param s the phoneme
     * @return that integer
     */
    protected static int map(String s){
        switch(s){
            case "k": case "@@": return 0;
            case "z": case "o": return 1;
            case "j": case "uua": return 2;
            case "t": case "uu": return 3;
            case "n": case "aa": return 4;
            case "ng": case "a": return 5;
            case "p": case "u": return 6;
            case "m": case "oo": return 7;
            case "w": case "i": return 8;
            case "s": case "ii": return 9;
            case "f": case "iia": return 10;
            case "l": case "vv": return 11;
            case "ch": case "q": return 12;
            case "d": case "e": return 13;
            case "c": case "ee": return 14;
            case "kh": case "vva": return 15;
            case "th": case "xx": return 16;
            case "kr": case "v": return 17;
            case "r": case "@": return 18;
            case "h": case "x": return 19;
            case "b": case "qq": return 20;
            case "pr": case "ia": return 21;
            case "ph": case "ua": return 22;
            case "kw": return 23;
            case "kl": return 24;
            case "tr": return 25;
            case "khl": return 26;
            case "khr": return 27;
            case "khw": return 28;
            case "phr": return 29;
            case "phl": return 30;
            case "pl": return 31;
            case "br": return 32;
            case "fr": return 33;
            case "thr": return 34;
            case "fl": return 35;
            case "bl": return 36;
            case "dr": return 37;
        }
        return 38;
    }
    /**g
     * Adds a word to the Trie
     * @param phonetic the phonetic to add
     */
    public void addPhonetic(List<String> phonetic, String thai) {
        if(phonetic.size() == 0) return ;
        LinkedList<String> z = new LinkedList<>(phonetic);
        for (int i = 0; i < z.size(); i++) {
            if(z.get(i).isEmpty()) {
                z.remove(i--);
                Log.d(Trie.TAG, "srsly bug #1");
            }
        }
        root.addPhonetic(z, thai);
    }

    /**
     * Get the word in the Trie with the given phonetic
     * @param phonetic a phonetic
     * @return a String objects containing the word in the Trie with the given phonetic.
     */
    public List<String> getWord(List<String> phonetic) {
        TrieNode curNode = root;
        for (String e : phonetic) {
            e = e.toLowerCase();
            curNode = curNode.children[map(e)];
            //If no node matches, then no words exist, return empty list
            if (curNode == null) return new LinkedList<>();
        }
        if(curNode.thaiWords == null) return new LinkedList<>();
        if(curNode.thaiWords.length() == 0) Log.d(TAG, "srsly bug, " + curNode.thaiWords.toString());
        return Arrays.asList(curNode.thaiWords.toString().split(","));
    }
}

