package com.karaokekeyboard.ime.trie;

/**
 * This file was created by Jew on 11/30/2015.
 */
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TrieNode
{
    //private static final String TAG = "Dictionary";
    //private TrieNode parent;
    private Map<String, TrieNode> children;
    //private boolean isWord;     // Does this node represent the Thai word
    public List<String> thaiWords;     // The phoneme this node represents

    /**
     * Constructor for top level root node.
     */
    public TrieNode()
    {
        children = new HashMap<String, TrieNode>(Trie.SIZE);
        this.thaiWords = null;
        //isWord = false;
    }

    /**
     * Constructor for child node.
     */
    public TrieNode(String key)
    {
        this();
        this.thaiWords = new LinkedList<String>();
        this.thaiWords.add(key);
    }

    /**
     * Adds a phonetic to this node. This method is called recursively and
     * adds child nodes for each successive phoneme in the phonetic.
     * @param phonetic the phonetic to add
     */
    protected void addPhonetic(List<String> phonetic, String thai)
    {
        if (phonetic.size() == 0)
        {
            if(children.containsKey("WORD")) children.get("WORD").thaiWords.add(thai);
            else children.put("WORD", new TrieNode(thai));
            return ;
        }
        String keyPos = phonetic.remove(0);
        if (children.get(keyPos) == null)
        {
            if(children.size() == Trie.SIZE) Log.d(Trie.TAG, "FULL!");
            children.put(keyPos, new TrieNode());
            //Log.d(Trie.TAG, "added " + keyPos);
        }
        children.get(keyPos).addPhonetic(phonetic, thai);
    }

    /**
     * Returns the child TrieNode representing the given phoneme,
     * or null if no node exists.
     * @param phoneme the phoneme
     * @return that node
     */
    protected TrieNode getNode(String phoneme)
    {
        return children.get(phoneme);
    }

    /**
     * Returns a List of String objects which are lower in the
     * hierarchy that this node.
     * @return list of Thai words
     */
    protected List<String> getWord() {
        if(!children.containsKey("WORD")) return new LinkedList<String>();
        return children.get("WORD").thaiWords;
    }
}
