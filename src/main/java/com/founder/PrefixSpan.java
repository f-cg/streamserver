package com.founder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * input: [[T,...],[],] output:[{patt, freq}]
 */
class FrequentPattern<T> {
	ArrayList<T> pattern;
	int frequence;

	FrequentPattern(ArrayList<T> patt, int freq) {
		this.pattern = patt;
		this.frequence = freq;
	}
}

class ProjectedIndex {
	final int seqidx;
	final int startpos;

	ProjectedIndex(int seqidx, int startpos) {
		this.seqidx = seqidx;
		this.startpos = startpos;
	}
}

public class PrefixSpan<T> {
	private int minSup = 2;
	double minSupDefault;
	int minLenDefault;
	int sortLenDefault;
	ArrayList<ArrayList<T>> seqs;
	/* FrequentPatterns<T> freqpatt; */
	ArrayList<FrequentPattern<T>> results;

	PrefixSpan(double minsup, int minlen, int sortlen) {
		this.minSupDefault = minsup;
		this.minLenDefault = minlen;
		this.sortLenDefault = sortlen;
	}

	PrefixSpan(double minsup) {
		this(minsup, 1, 0);
	}

	PrefixSpan() {
		this(0.1);
	}

	private void frequentRec(ArrayList<T> patt, ArrayList<ProjectedIndex> projectedIndices) {
		FrequentPattern<T> freqpatt = new FrequentPattern<T>(patt, projectedIndices.size());
		this.results.add(freqpatt);
		LinkedHashMap<T, ArrayList<ProjectedIndex>> occurs = new LinkedHashMap<T, ArrayList<ProjectedIndex>>();
		for (ProjectedIndex projectedIndex : projectedIndices) {
			int i = projectedIndex.seqidx;
			ArrayList<T> seq = seqs.get(i);
			int startpos = projectedIndex.startpos;
			for (int j = startpos + 1; j < seq.size(); j++) {
				T e = seq.get(j);
				if (!occurs.containsKey(e)) {
					occurs.put(e, new ArrayList<ProjectedIndex>());
				}
				ArrayList<ProjectedIndex> oc = occurs.get(e);
				if (oc.size() == 0 || oc.get(oc.size() - 1).seqidx != i)
					oc.add(new ProjectedIndex(i, j));
			}
		}
		for (HashMap.Entry<T, ArrayList<ProjectedIndex>> entry : occurs.entrySet()) {
			T e = entry.getKey();
			ArrayList<ProjectedIndex> newmdb = entry.getValue();
			if (newmdb.size() >= this.minSup) {
				patt.add(e);
				frequentRec(new ArrayList<T>(patt), newmdb);
				patt.remove(patt.size() - 1);
			}
		}
	}

	ArrayList<FrequentPattern<T>> run(ArrayList<ArrayList<T>> seqs, double minSupRate) {
		this.minSupDefault = minSupRate;
		this.minSup = (int) (seqs.size() * minSupRate);
		this.seqs = seqs;
		this.results = new ArrayList<FrequentPattern<T>>();
		ArrayList<ProjectedIndex> projectedIndices = new ArrayList<ProjectedIndex>();
		for (int i = 0; i < seqs.size(); i++) {
			projectedIndices.add(new ProjectedIndex(i, -1));
		}
		ArrayList<T> initPattern = new ArrayList<T>();
		frequentRec(initPattern, projectedIndices);
		sortResults();
		return this.results;
	}

	ArrayList<FrequentPattern<T>> run(ArrayList<ArrayList<T>> seqs) {
		return this.run(seqs, this.minSupDefault);
	}

	void printSeqs() {
		for (ArrayList<T> seq : seqs) {
			for (T e : seq) {
				System.out.print(e + " ");
			}
			System.out.print("\n");
		}
	}

	private void printFrequentPatterns(FrequentPattern<T> frequentPatterns) {
		System.out.print("freq:" + frequentPatterns.frequence);
		System.out.print("; patterns:");
		for (T e : frequentPatterns.pattern) {
			System.out.print(e + " ");
		}
		System.out.print("\n");
	}

	void printResults() {
		for (FrequentPattern<T> frequentPatterns : results) {
			this.printFrequentPatterns(frequentPatterns);
		}
	}

	ArrayList<ArrayList<Integer>> loadIntSeqs(String filepath, String delim) throws IOException {
		ArrayList<ArrayList<Integer>> seqs = new ArrayList<ArrayList<Integer>>();
		File file = new File(filepath);
		FileReader fr = new FileReader(file);
		BufferedReader bf = new BufferedReader(fr);
		String line;
		while ((line = bf.readLine()) != null) {
			ArrayList<Integer> seq = new ArrayList<Integer>();
			for (String str : line.split(delim)) {
				seq.add(Integer.parseInt(str));
			}
			seqs.add(seq);
		}
		bf.close();
		return seqs;
	}

	ArrayList<ArrayList<String>> loadStrSeqs(String filepath, String delim) throws IOException {
		ArrayList<ArrayList<String>> seqs = new ArrayList<ArrayList<String>>();
		File file = new File(filepath);
		FileReader fr = new FileReader(file);
		BufferedReader bf = new BufferedReader(fr);
		String line;
		while ((line = bf.readLine()) != null) {
			ArrayList<String> seq = new ArrayList<String>();
			for (String str : line.split(delim)) {
				seq.add(str);
			}
			seqs.add(seq);
		}
		bf.close();
		return seqs;
	}

	private void sortResults() {
		this.results.sort(new Comparator<FrequentPattern<T>>() {
			@Override
			public int compare(FrequentPattern<T> f1, FrequentPattern<T> f2) {
				int len1 = f1.pattern.size();
				int len2 = f2.pattern.size();
				// 如果两个都在sortlen一边，则按频率从大到小排序
				if ((len1 - sortLenDefault) * (len2 - sortLenDefault) > 0) {
					if (f1.frequence != f2.frequence) {
						return -(f1.frequence - f2.frequence);
					} else {
						return -(len1 - len2);
					}
					// 否则两个在sortlen两边，则按长度从大到小排序
				} else {
					if (len1 != len2) {
						return -(len1 - len2);
					} else {
						return -(f1.frequence - f2.frequence);
					}
				}
			}
		});
	}
}
