package com.intumit.smartwiki.recommend;

import java.io.Serializable;


public class Trie implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = -2973027420061112626L;

	public Node rootNode = null;

    public Trie() {
        rootNode = Node.createEmptyNode(null);
    }

    public String toString() {
        Node p = null;
        StringBuffer sb = new StringBuffer();
        for
        (p = rootNode.getFirstChild(); !p.isEmpty(); p = p.getBrother())
        {
            sb.append(p.toString());
        }
        return sb.toString();
    }

//    public void addString(char[] str, int wordId) {
//        //addString(rootNode.getFirstChild(), str, str.length, 0);
//    	addString(str, new String(str), wordId);
//    }

    public void addString(char[] path, String keyword, int wordId)
    {
    	addString(rootNode.getFirstChild(), path, path.length, 0, keyword, wordId);
    }

    public Node findString(char[] str) {
        return findString(rootNode.getFirstChild(), str, str.length, 0);
    }

    private Node findString(Node ptrFirstChild, char[] str, int strLen, int i) {
        Node p = null;
        for (p = ptrFirstChild; !p.isEmpty(); p = p.getBrother()) {
            if (p.info == str[i]) {
                if (i == strLen - 1) {
                    if (p instanceof KeywordNode)
                        return p;
                    else
                        return null;
                } else {
                    return findString(p.getFirstChild(), str, strLen, i + 1);
                }
            }
        }
        return null;
    }

    public void addString(Node ptrFirstChild, char[] str, int strLen, int i, String keyword, int wordId) {
    	Node p = null;

    	//path 底線後第一個字母轉大寫
    	 if(i-1 >= 0 && str[i-1] == '_')
        	str[i] = Character.toUpperCase(str[i]);

    	Node leftBro = null;
        for (p = ptrFirstChild; !p.isEmpty(); p = p.getBrother()) {

            if (p.info == str[i]) {
                if (i == strLen - 1) {
                	/*
        			p.word = new String(keyword);//new String(str);
                    p.wordId = wordId;
        			*/
                	p = new KeywordNode();
                	p.info = str[i];

                	p.parent = ptrFirstChild.parent;
                	((KeywordNode)p).word = String.valueOf(keyword);
                    p.wordId = wordId;
//                    ((KeywordNode)p).setChilds(WikiWord.childIds(wordId));

                    if(leftBro == null)	//關鍵字重疊時, 後來的將前面的覆蓋掉
                    {
                    	for (Node node = ptrFirstChild.fetchFirstChild(); node != null && !node.isEmpty(); node = node.fetchBrother())
                    		node.parent = p;
                    	p.setFirstChild(ptrFirstChild.getFirstChild());
                    	p.setBrother(ptrFirstChild.getBrother());
                    	ptrFirstChild.parent.setFirstChild(p);
                    }
                    else
                    {
                    	for (Node node = leftBro.getBrother().fetchFirstChild(); node != null && !node.isEmpty(); node = node.fetchBrother())
                    		node.parent = p;
                    	p.setBrother(leftBro.getBrother().getBrother());
                    	p.setFirstChild(leftBro.getBrother().getFirstChild());
                    	leftBro.setBrother(p);
                    }
                    return;
                } else {
                    addString(p.getFirstChild(), str, strLen, i + 1, keyword, wordId);
                    return;
                }
            }
            leftBro = p;
        }

        p.info = str[i];

        //p.setBrother( Node.createEmptyNode(p.parent));
        //p.setFirstChild( Node.createEmptyNode(p) );

//        p.label = new String(str, 0, i + 1);
        if (i == strLen - 1) {
        	/*
			p.word = new String(keyword);//new String(str);
            p.wordId = wordId;
			*/
        	p = new KeywordNode();
        	p.info = str[i];
        	p.parent = ptrFirstChild.parent;
        	((KeywordNode)p).word = new String(keyword);//new String(str);
            p.wordId = wordId;
//            ((KeywordNode)p).setChilds(WikiWord.childIds(wordId));
            if(leftBro == null)
            {
            	ptrFirstChild.parent.setFirstChild(p);
            }
            else
            {
            	leftBro.setBrother(p);
            }
            return;
        } else {
            addString(p.getFirstChild(), str, strLen, i + 1, keyword, wordId);
        }

    }

}
