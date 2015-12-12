package com.onekeyboard.ime.trie;

/**
 * This file was created by Jew on 11/30/2015.
 */
import android.util.Log;

import com.onekeyboard.ime.SoftKeyboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Trie
{
    public static final int SIZE = 42;
    public static final String TAG = "Dictionary";
    private TrieNode root;

    /**
     * Constructor
     */
    public Trie()
    {
        root = new TrieNode();
    }

    /**
     * Adds a word to the Trie
     * @param phonetic the phonetic to add
     */
    public void addPhonetic(List<String> phonetic, String thai)
    {
        if(phonetic.size() == 0) return ;
        LinkedList<String> z = new LinkedList<String>(phonetic);
        for (int i = 0; i < z.size(); i++) {
            char x;
            if(!z.get(i).isEmpty()) x = z.get(i).charAt(0);
            else x = '5';
            if(x >= '0' && x <= '9') z.remove(i--);
        }
        root.addPhonetic(z, thai);
    }

    /**
     * Get the word in the Trie with the given phonetic
     * @param phonetic a phonetic
     * @return a String objects containing the word in the Trie with the given phonetic.
     */
    public List<String> getWord(List<String> phonetic)
    {
        TrieNode curNode = root;
        for (String e : phonetic) {
            e = e.toLowerCase();
            curNode = curNode.getNode(e);
            //If no node matches, then no words exist, return empty list
            if (curNode == null) return null;
        }
        return curNode.getWord();
    }
}

