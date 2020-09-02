package com.intumit.solr.robot.entity;

public class QAEntityMatched extends QAEntity {

	String matchedValue;
	int matchedPosition;
	
	public String getMatchedValue() {
		return matchedValue;
	}
	public void setMatchedValue(String matchedValue) {
		this.matchedValue = matchedValue;
	}
	public int getMatchedPosition() {
		return matchedPosition;
	}
	public void setMatchedPosition(int matchedPosition) {
		this.matchedPosition = matchedPosition;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((matchedValue == null) ? 0 : matchedValue.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		QAEntityMatched other = (QAEntityMatched) obj;
		if (matchedValue == null) {
			if (other.matchedValue != null) return false;
		}
		else if (!matchedValue.equals(other.matchedValue)) return false;
		return true;
	}
}
