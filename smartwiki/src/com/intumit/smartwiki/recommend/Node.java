package com.intumit.smartwiki.recommend;

import java.io.Serializable;

public class Node implements Serializable {

    /**
	 *
	 */
	private static final long serialVersionUID = 8681363176341803567L;
	private static int cntInstances = 0;
    public int id = -1;

    //private static Vector instances = new Vector();
    /*public static void ClearCntInstances() {
        cntInstances = 0;
        //instances.removeAllElements();
    }*/

//    public static Node getNodeByID(int id) {
//        return (Node) instances.elementAt(id);
//    }

    protected Node() {

        this.info = '\uffff';

        this.ptrBrother = null;
        this.ptrFirstChild = null;
//        this.word = null;
        //this.label = null;

        cntInstances++;
        //instances.addElement(this);

        this.id = cntInstances;
        //this.ID = new Integer(cntInstances);
    }

    public char info;
    private Node ptrBrother = null;
    private Node ptrFirstChild = null;
    public Node parent = null;

//    public String word = null;
    public int wordId = -1;

//    public String label = null;

    public static Node createEmptyNode(Node parent) {
        Node n = new Node();
        //n.ptrFirstChild = new Node();
        //n.ptrBrother = new Node();
        n.parent = parent;
        //n.ptrFirstChild.parent = n;
        //n.ptrBrother.parent = n.parent;
        return n;
    }

    public boolean isEmpty() {
    	return this.info == '\uffff';
    }

    public String toString() {
        return this.toString(0);
    }

    public String toString(int i) {
        StringBuffer sb = new StringBuffer();
        for (int k = 0; k < i; k++) {
            sb.append(' ');
        }

        sb.append(this.info);
//        if (this.word != null) {
//            sb.append("   ->|" + this.word + "|");
//        }
        sb.append("\n");
        Node p = null;
        for
        (p = this.getFirstChild(); !p.isEmpty(); p = p.getBrother())
        {
            sb.append(p.toString(i + 1));
        }
        return sb.toString();
    }

    public Node getBrother() {

		if(this.ptrBrother == null)
		{
			Node node = new Node();
			this.setBrother(node);
			node.parent = this.parent;

		}
		return ptrBrother;
	}
    
    public Node fetchBrother()
    {
    	return ptrBrother;
    }

	public void setBrother(Node ptrBrother) {
		this.ptrBrother = ptrBrother;
	}

	public Node getFirstChild() {

		if(this.ptrFirstChild == null)
		{
			Node node = new Node();
			this.setFirstChild(node);
			node.parent = this;

		}

		return ptrFirstChild;
	}
	
	public Node fetchFirstChild()
	{
		return ptrFirstChild;
	}

	public void setFirstChild(Node ptrFirstChild) {
		this.ptrFirstChild = ptrFirstChild;
	}



	public String getPath(){

		StringBuffer str = new StringBuffer();
		Node current = this;
		while(current.parent != null)
		{
			str.insert(0, current.info);
//			str.append(current.info);
			current = current.parent;
		}
		return str.toString();
	}

	public boolean isSuffixOf(String suffix)
	{
		String path = this.getPath();
		if(path.endsWith(suffix))
			return true;
		else
			return false;
	}
}