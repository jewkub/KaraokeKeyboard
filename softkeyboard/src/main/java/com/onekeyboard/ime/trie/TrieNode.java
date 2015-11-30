package com.onekeyboard.ime.trie;

/**
 * Created by Jew on 11/30/2015.
 */
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

public class TrieNode
{
    private TrieNode parent;
    private Map<String, TrieNode> children;
    private boolean isWord;     // Does this node represent the Thai word
    private String phoneme;     // The phoneme this node represents
    private static final int SIZE = 30;

    /**
     * Constructor for top level root node.
     */
    public TrieNode()
    {
        children = new HashMap<String, TrieNode>(SIZE);
        isWord = false;
    }

    /**
     * Constructor for child node.
     */
    public TrieNode(String key)
    {
        this();
        this.phoneme = key;
    }

    /**
     * Constructor for child node specified key and parent
     */
    public TrieNode(String key, TrieNode parent)
    {
        this(key);
        this.parent = parent;

    }
    /**
     * Constructor for child node specified key, parent and isWord
     */
    public TrieNode(String key, TrieNode parent, boolean isWord)
    {
        this(key, parent);
        this.isWord = isWord;
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
            //Log.d("Dictionary", "3 " + thai);
            children.put("WORD", new TrieNode(thai, this, true));
            return ;
        }
        String keyPos = phonetic.remove(0);
        if (children.get(keyPos) == null)
        {
            children.put(keyPos, new TrieNode(keyPos, this));
            // children[charPos] = new TrieNode(word.charAt(0));
            // children.get(keyPos).parent = this;
        }
        children.get(keyPos).addPhonetic(phonetic, thai);
    }

    /**
     * Returns the child TrieNode representing the given phoneme,
     * or null if no node exists.
     * @param phoneme
     * @return that node
     */
    protected TrieNode getNode(String phoneme)
    {
        return children.get(phoneme);
    }

    /**
     * Returns a List of String objects which are lower in the
     * hierarchy that this node.
     * @return
     */
    protected List getWords() {
        //Create a list to return
        List words = new ArrayList();

        //If this node represents a word, add it
        if (isWord) {
            words.add(phoneme); // in this case, phoneme = word
        }
        for (String key : children.keySet()) {
            words.addAll(children.get(key).getWords());
        }
        return words;
    }

    /**
     * Gets the String that this node represents.
     * For example, if this node represents the character t, whose parent
     * represents the charater a, whose parent represents the character
     * c, then the String would be "cat".
     * @return
     */
    public String toString()
    {
        if (parent == null)
        {
            return "";
        }
        else
        {
            return parent.toString() + "-" + phoneme;
        }

    }
}
