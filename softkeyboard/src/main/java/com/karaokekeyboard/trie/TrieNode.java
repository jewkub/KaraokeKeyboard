package com.karaokekeyboard.trie;

/**
 * This file was created by Jew on 11/30/2015.
 */

import java.io.Serializable;
import java.util.List;

/**
 * Class for each node in trie. Some leaf nodes contain Thai word(s).
 */
public class TrieNode implements Serializable
{
    protected TrieNode[] children;
    protected StringBuilder thaiWords;     // The phoneme this node represents, separated by comma

    /**
     * Constructor
     */
    public TrieNode()
    {
        children = new TrieNode[Trie.SIZE];
        thaiWords = null;
    }

    /**
     * Adds a phonetic to this node. This method is called recursively and
     * adds child nodes for each successive phoneme in the phonetic.
     * @param phonetic the phonetic to add
     * @param thai the Thai word to add
     */
    protected void addPhonetic(List<String> phonetic, String thai) {
        if (phonetic.size() == 0)
        {
            if(thaiWords != null) thaiWords.append(Trie.COMMA);
            else thaiWords = new StringBuilder();
            thaiWords.append(thai);
            return ;
        }
        int keyPos = Trie.map(phonetic.remove(0));
        if (children[keyPos] == null)
        {
            children[keyPos] = new TrieNode();
            //Log.d(Trie.TAG, "added " + keyPos);
        }
        children[keyPos].addPhonetic(phonetic, thai);
    }
}
