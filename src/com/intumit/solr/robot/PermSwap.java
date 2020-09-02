package com.intumit.solr.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PermSwap {
	public static void perm(List<String> list, int k, int m, List<String> results) {
		if (k == m) {
			String result = "";
			for (int i = 0; i <= m; i++) {
				result += list.get(i);
			}
			results.add(result);
		} else {
			for (int i = k; i <= m; i++) {
				swap(list, k, i);
				perm(list, k + 1, m, results);
				swap(list, k, i);
			}
		}
	}

	public static void swap(List<String> list, int a, int b) {
		String temp;
		temp = list.get(a);
		list.set(a, list.get(b));
		list.set(b, temp);
	}

	public static void main(String args[]) {
		/*
		 * List<String> list = new ArrayList<String>(); for (int i = 0; i < 2; i++) {
		 * list.add("" + (i + 1)); } List<String> results = new ArrayList<String>();
		 * perm(list, 0, list.size() - 1, results); for (String result : results) {
		 * System.out.println(result); }
		 */

		List<String> q = new ArrayList<String>();
		q.add("111");
		q.add("222");
		q.add("333");
		q.add("444");
		Map<Integer, List<String>> r = doit(q, 2, false);
		System.out.println(r);
	}

	/**
	 * @param chars 总的字符序列（数组）
	 * @param n     要取出的字符的个数
	 */
	public static Map<Integer, List<String>> doit(List<String> q, int n, boolean reverse) {
		Map<Integer, List<String>> results = new TreeMap<Integer, List<String>>();
		if (n <= 0 || q == null || q.size() == 0) {
			return results;
		}
		List<String> qList = new ArrayList<String>();
		// 通过这一步初始化序列的长度
		for (int i = 0; i < n; i++) {
			qList.add("#");
		}
		listAll(results, qList, q, n, reverse);
		return results;
	}

	/**
	 * 从m个元素中任取n个并对结果进行全排列
	 * 
	 * @param list  用于承载可能的排列情况的List
	 * @param chars 总的字符数组，长度为m
	 * @param n     从中取得字符个数
	 * @return 
	 */
	public static void listAll(Map<Integer, List<String>> results, List<String> qlist, List<String> qs, int n, boolean reverse) {
		if (n == 0) {
			results.put(results.size() + 1, new ArrayList<String>(qlist));
			return;
		}
		for (String q : qs) { // 暴力尝试
			if (!qlist.contains(q)) { // 若List中不包含这一位元素
				if (!reverse && !qlist.get(0).equals("#")) {
					// 不允許往回
					if (qs.indexOf(qlist.get(0)) > qs.indexOf(q))
						continue;
				} else if (reverse) {
					// 允許往回
				}
				qlist.set(qlist.size() - n, q); // 将当前元素加入
			} else { // 否则跳到下一位
				continue;
			}
			listAll(results, qlist, qs, n - 1, reverse); // 下一位
			qlist.set(qlist.size() - n, "#"); // 还原
		}
	}
}