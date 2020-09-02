package com.intumit.smartwiki.recommend;

public class KeywordNode extends Node {

	/**
	 *
	 */
	private static final long serialVersionUID = 3470617244732596705L;

	private int[] childs = null;
	public String word = null;

	public int[] getChilds() {
		return childs;
	}

	public void setChilds(int[] childs) {
		this.childs = childs;
	}

	public String toString(int i) {
        StringBuffer sb = new StringBuffer();
        for (int k = 0; k < i; k++) {
            sb.append(' ');
        }

        sb.append(this.info);
        if (this.word != null) {
            sb.append("   ->|" + this.word + "|");
        }
        sb.append("\n");
        Node p = null;
        for ( p = this.getFirstChild(); !p.isEmpty(); p = p.getBrother()) {
            sb.append(p.toString(i + 1));
        }
        return sb.toString();
    }

}
