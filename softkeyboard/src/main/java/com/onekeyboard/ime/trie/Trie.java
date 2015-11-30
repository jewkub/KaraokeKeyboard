package com.onekeyboard.ime.trie;

/**
 * Created by Jew on 11/30/2015.
 */
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Trie
{
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
     * @param phonetic
     */
    public void addPhonetic(List<String> phonetic, String thai)
    {
        if(phonetic.size() == 0) return ;
        List<String> z = new ArrayList<String>(phonetic);
        for (int i = 0; i < z.size(); i++) {
            char x;
            if(!z.get(i).isEmpty()) x = z.get(i).charAt(0);
            else x = '5';
            if(x >= '0' && x <= '9') z.remove(i--);
        }
        root.addPhonetic(z, thai);
    }

    /**
     * Get the words in the Trie with the given
     * prefix
     * @param prefix
     * @return a List containing String objects containing the words in
     *         the Trie with the given prefix.
     */
    public List<String> getPhonetics(String[] prefix)
    {
        //Find the node which represents the last letter of the prefix
        TrieNode curNode = root;
        for (String e : prefix) {
            curNode = curNode.getNode(e);

            //If no node matches, then no words exist, return empty list
            if (curNode == null) return new ArrayList();
        }

        //Return the words which eminate from the last node
        return curNode.getWords();
    }
}

