package com.intumit.android.search.fuzzy;

import java.io.Serializable;

public class Output implements Serializable {
	private static final long serialVersionUID = -1608554135347808251L;
	
	public static final int NOT_SYNONYM = 0;
	private int output;
	private int fieldCode;
	int synonymCode;
	
	
	public Output(int output, int fieldCode, int mappingCode) {
		super();
		this.output = output;
		this.fieldCode = fieldCode;
		this.synonymCode = mappingCode;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fieldCode;
		result = prime * result + synonymCode;
		result = prime * result + output;
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
		Output other = (Output) obj;
		if (fieldCode != other.fieldCode)
			return false;
		if (synonymCode != other.synonymCode)
			return false;
		if (output != other.output)
			return false;
		return true;
	}
	
	public void writeTo(ByteBuffer bb, int position) {
		bb.putInt(position, output);
		bb.putInt(position+4, fieldCode);
		bb.putInt(position+8, synonymCode);
	}

	public static Output readFrom(ByteBuffer bb, int position) {
		int output = bb.getInt(position);
		int fieldCode = bb.getInt(position + 4);
		int mappingCode = bb.getInt(position + 8);
		return new Output(output, fieldCode, mappingCode);
	}

	@Override
	public String toString() {
		return "Output [output=" + output + ", fieldCode=" + fieldCode
				+ ", mappingCode=" + synonymCode + "]";
	}


	public int getFieldCode() {
		return fieldCode;
	}

	public int getDocId() {
		return output;
	}
	
}
