package com.intumit.android.search.fuzzy;

public class Piece implements Comparable<Piece> {
	
	int startPos;
	int endPos;
	String text;
	String pinyin = null;
	String matchedBy = null;
	float score;
	
	public Piece(String text, int startPos, int endPos) {
		super();
		this.startPos = startPos;
		this.endPos = endPos;
		this.text = text;
	}
	
	public String getPinyin() {
		return pinyin;
	}

	public void setPinyin(String pinyin) {
		this.pinyin = pinyin;
	}

	public String getMatchedBy() {
		return matchedBy;
	}

	void setMatchedBy(String matchedBy) {
		this.matchedBy = matchedBy;
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public float getScore() {
		return score;
	}

	void setScore(float score) {
		this.score = score;
	}

	public int getStartPos() {
		return startPos;
	}

	public int getEndPos() {
		return endPos;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endPos;
		result = prime * result
				+ ((matchedBy == null) ? 0 : matchedBy.hashCode());
		result = prime * result + ((pinyin == null) ? 0 : pinyin.hashCode());
		result = prime * result + startPos;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Piece other = (Piece) obj;
		if (endPos != other.endPos)
			return false;
		if (matchedBy == null) {
			if (other.matchedBy != null)
				return false;
		} else if (!matchedBy.equals(other.matchedBy))
			return false;
		if (pinyin == null) {
			if (other.pinyin != null)
				return false;
		} else if (!pinyin.equals(other.pinyin))
			return false;
		if (startPos != other.startPos)
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Piece (score=" + score + ") [text=" + text + ", pinyin=" + pinyin + ", matchedBy=" + matchedBy + "]"
				+ "[startPos=" + startPos 
				+ ", endPos=" + endPos 
				+ "]"
				;
	}

	@Override
	public int compareTo(Piece o) {
		return this.score > o.score ? -1 : 1;
	}
}