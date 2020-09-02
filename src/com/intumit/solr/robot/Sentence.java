package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.intumit.solr.tenant.Tenant;


/**
Alt Comparison
Alt 為 Sentence，斷詞後一個 Sentence 包含多 Blocks

假設斷詞依照括弧分開成多個連續 block
判定依照每個 block 順序來判定
Block 
A contains B ：代表 B 是 A 的子集合
A partial contains B ：代表 A 跟 B 互不為子集合，但有交集
A equals B ：代表 A 跟 B 完全相同
A not contains any of B ：代表 A 跟 B 完全不同（沒有任何交集）

Sentence 之前的相似程度
1. A 完全包含 B（ B 的 Block 順序跟數量完全跟 A 相同，且每個 Block 都小於等於 A） 
2. A 部分包含 B（ B 的 Block 順序跟 A 的某段相同，但 B 的前或後（或者前後）缺少部分 Block

Merge Alt
1. 假設 A merge B，不可因為 Merge (A+B) 後大於 A 與 B 原本各自包含的組合
*/
public class Sentence {
	
	public boolean contains(Sentence sb) {
		boolean isContains = true;
		
		int offsetOfA = 0;
		
		for (Block ba: blocks) {
			Block bb = null;
			
			if (offsetOfA < sb.blocks.size()) 
				bb = sb.blocks.get(offsetOfA);
			
			if (ba.contains(bb)) {
				offsetOfA++;
			}
			else if (ba.canBeIgnore) {
				// keep offset
			}
			else {
				isContains = false;
				break;
			}
		}
		
		if (offsetOfA != sb.blocks.size())
			isContains = false;
		
		return isContains;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
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
		Sentence other = (Sentence) obj;
		if (blocks == null) {
			if (other.blocks != null)
				return false;
		} else if (!blocks.equals(other.blocks))
			return false;
		return true;
	}

	public String getSentence() {
		return sentence;
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	String sentence;
	List<Block> blocks = new ArrayList<Block>();
	
	public Sentence(String sentence) {
		super();
		this.sentence = sentence;
		breakIntoBlocks();
	}

	void breakIntoBlocks() {
		Matcher m = Pattern.compile("\\(([^\\)]+)\\)").matcher(sentence);
		int lastEndOfBlock = 0;
		
		while (m.find()) {
			int start = m.start();
			int end = m.end();
			
			if (start > lastEndOfBlock) {
				String part = StringUtils.substring(sentence, lastEndOfBlock, start);
				
				while (part.indexOf("?") != -1) {
					int qpos = part.indexOf("?");
					
					if (qpos > 1) {
						Block sb = new Block(StringUtils.substring(part, 0, qpos-1));
						part = StringUtils.substring(part, qpos - 1);
						sb.isStatic = true;
						sb.terms.add(sb.blockStr);
						
						blocks.add(sb);
					}
					else if (qpos == 1) {
						Block sb = new Block(StringUtils.substring(part, 0, 1));
						part = StringUtils.substring(part, 2);
						sb.isStatic = false;
						sb.canBeIgnore = true;
						sb.terms.add(sb.blockStr);
						
						blocks.add(sb);
					}
					else if (part.length() == 0) {
						break;
					}
					else {
						break;
					}
				}

				if (part.length() > 0) {
					Block sb = new Block(part);
					sb.isStatic = true;
					sb.canBeIgnore = false;
					sb.terms.add(sb.blockStr);
					
					blocks.add(sb);
				}
			}
			
			lastEndOfBlock = end;
			String inners = m.group(1);
			
			Block b = new Block(inners);
			b.isInParentheses = true;
			blocks.add(b);
			
			b.isStatic = false;
			String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
			
			for (int i=0; i < innerArr.length; i++) {
				String inner = innerArr[i];
				
				if (StringUtils.isEmpty(inner)) {
					if (i == innerArr.length - 1) {
						b.canBeIgnore = true;
					}
					else {
						// 不是在最後面的 "|" 代表是輸入錯誤不小心放了兩個 "||" 在斷句中間，忽略他
					}
				}
				else {
					b.terms.add(inner);
				}
			}
		}
		
		if (lastEndOfBlock < sentence.length()) {
			String part = StringUtils.substring(sentence, lastEndOfBlock);
			
			while (part.indexOf("?") != -1) {
				int qpos = part.indexOf("?");
				
				if (qpos > 1) {
					Block sb = new Block(StringUtils.substring(part, 0, qpos-1));
					part = StringUtils.substring(part, qpos - 1);
					sb.isStatic = true;
					sb.terms.add(sb.blockStr);
					
					blocks.add(sb);
				}
				else if (qpos == 1) {
					Block sb = new Block(StringUtils.substring(part, 0, 1));
					part = StringUtils.substring(part, 2);
					sb.isStatic = false;
					sb.canBeIgnore = true;
					sb.terms.add(sb.blockStr);
					
					blocks.add(sb);
				}
				else if (part.length() == 0) {
					break;
				}
				else {
					break;
				}
			}
			if (part.length() > 0) {
				Block sb = new Block(part);
				sb.isStatic = true;
				sb.canBeIgnore = false;
				sb.terms.add(sb.blockStr);
				
				blocks.add(sb);
			}
		}
	}

	public String concateToString() {
		StringBuilder buf = new StringBuilder();
		
		for (Block b: blocks) {
			buf.append(b.concateToString());
		}
		return buf.toString();
	}
	
	public void replaceTerm(String term, String newTerm) {
		for (Block b: blocks) {
			List<String> newTerms = new ArrayList<>();
			
			for (String t: b.terms) {
				if (newTerms.contains(t)) continue;
				
				if (StringUtils.equalsIgnoreCase(term, t)) {
					newTerms.add(newTerm);
				}
				else {
					newTerms.add(t);
				}
			}
			
			b.terms = newTerms;
		}
	}
	
	public void replaceTerms(String newTerm, Set<String> terms) {
		for (Block b: blocks) {
			List<String> newTerms = new ArrayList<>();
			
			for (String t: b.terms) {
				if (newTerms.contains(t)) continue;
				
				if (terms.contains(t)) {
					if (!newTerms.contains(newTerm)) // 新詞可能也早已經存在，存在的話就跳過
						newTerms.add(newTerm);
				}
				else {
					newTerms.add(t);
				}
			}
			
			b.terms = newTerms;
		}
	}
	
	public void removeTermBIfTermAExist(String termA, String[] termBs) {
		for (Block b: blocks) {
			if (b.terms.contains(termA)) {
				b.terms.removeAll(Arrays.asList(termBs));
			}
		}
	}

	@Override
	public String toString() {
		return "Sentence [sentence=" + sentence + ", blocks=" + blocks + "]";
	}


	public class Block {
		String blockStr;
		List<String> terms = new ArrayList<String>();
		boolean canBeIgnore = false;
		boolean isStatic = false;
		boolean isInParentheses = false;
		
		public Block(String inners) {
			this.blockStr = inners;
		}
		
		public String getBlockStr() {
			return blockStr;
		}



		public List<String> getTerms() {
			return terms;
		}



		public boolean isCanBeIgnore() {
			return canBeIgnore;
		}



		public boolean isStatic() {
			return isStatic;
		}



		public boolean isInParentheses() {
			return isInParentheses;
		}



		public boolean contains(Block bb) {
			if (bb == null)
				return false;
			
			return (terms.containsAll(bb.terms));
		}
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((blockStr == null) ? 0 : blockStr.hashCode());
			result = prime * result + (canBeIgnore ? 1231 : 1237);
			result = prime * result + (isStatic ? 1231 : 1237);
			result = prime * result + ((terms == null) ? 0 : terms.hashCode());
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
			Block other = (Block) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (blockStr == null) {
				if (other.blockStr != null)
					return false;
			} else if (!blockStr.equals(other.blockStr))
				return false;
			if (canBeIgnore != other.canBeIgnore)
				return false;
			if (isStatic != other.isStatic)
				return false;
			if (terms == null) {
				if (other.terms != null)
					return false;
			} else if (!terms.equals(other.terms))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Block [blockStr=" + blockStr + ", terms=" + terms
					+ ", isInParentheses=" + isInParentheses + ", canBeIgnore=" + canBeIgnore + ", isStatic=" + isStatic
					+ "]";
		}

		private Sentence getOuterType() {
			return Sentence.this;
		}
		
		public String concateToString() {
			StringBuilder buf = new StringBuilder();
			
			for (String t: terms) {
				if (buf.length() > 0)
					buf.append('|');
				buf.append(t);
			}
			
			if (isInParentheses) {
				buf.insert(0, '(');
				
				if (canBeIgnore) {
					buf.append('|');
				}
				
				buf.append(')');
			}
			else {
				if (canBeIgnore) {
					buf.append('?');
				}
			}
			
			return buf.toString();
		}
	}
	
	public static void main(String[] args) {
		String str = "什麼?是(刷)(商務御璽卡)(國外|)(有||是否)什麼?(保障)嗎";
		System.out.println(str);
		Sentence s = new Sentence(str);
		
		for (Block b: s.blocks) {
			System.out.println(b);
/*			if (b.isInParentheses) {
				b.terms.add("碩網");
			}
*/
		}
		
		System.out.println(s.concateToString());
	}
}
