//
//author:   zxyeh
//datatime: 2020-08-17 00:41:06
//

package com.founder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

class EventProb {
	List<String> happened;
	String pred;
	double prob;

	EventProb(List<String> happened, String pred, double prob) {
		this.happened = happened;
		this.pred = pred;
		this.prob = prob;
	}
}

public class EventPredictor {
	private static final String COMMA_DELIMITER = ",";
	private static final String NOPRED = "无";
	private static final int GOODSIZE = 40;
	private static final int SMALLSIZE = 10;
	private static final int TOOSMALLSIZE = 2;
	int minLenDefault;
	int sortLenDefault;
	HashMap<String, HashMap<String, Integer>> trainMap;

	EventPredictor(int minlen, int sortlen) {
		trainMap = new HashMap<>();
		this.minLenDefault = minlen;
		this.sortLenDefault = sortlen;
	}

	EventPredictor() {
		this(1, 0);
	}

	private List<List<String>> ReadCSV(String url) {
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(url))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(COMMA_DELIMITER);
				records.add(Arrays.asList(values));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return records;
	}

	private HashMap<String, List<String>> GroupCases(List<List<String>> raw_cases) {
		HashMap<String, List<String>> seq = new HashMap<>();
		for (int i = 1; i < raw_cases.size(); ++i) {
			String case_id = raw_cases.get(i).get(0);
			String activity_id = raw_cases.get(i).get(1);
			// String complete_timestamp = raw_cases.get(i).get(2);
			if (seq.containsKey(case_id)) {
				seq.get(case_id).add(activity_id);
			} else {
				List<String> new_seq = new ArrayList<>();
				new_seq.add(activity_id);
				seq.put(case_id, new_seq);
			}
		}
		return seq;
	}

	private void ConvertToTrain(HashMap<String, List<String>> seq) {
		HashMap<String, Integer> newHashMap = new HashMap<>();
		trainMap.put("root", newHashMap);
		for (String strkey : seq.keySet()) {
			List<String> case_seq = seq.get(strkey);
			Insert(trainMap, "root", case_seq.get(0));
			List<List<String>> s_seq = SubSeq(case_seq);
			for (List<String> s : s_seq) {
				Insert(trainMap, s.get(0), s.get(1));
			}
		}
	}

	private void Insert(HashMap<String, HashMap<String, Integer>> train_dist, String key, String value) {
		if (train_dist.containsKey(key)) {
			if (train_dist.get(key).containsKey(value)) {
				train_dist.get(key).put(value, train_dist.get(key).get(value) + 1);
			} else {
				train_dist.get(key).put(value, 1);
			}
		} else {
			HashMap<String, Integer> emptyKey = new HashMap<>();
			emptyKey.put(value, 1);
			train_dist.put(key, emptyKey);
		}
	}

	private List<List<String>> SubSeq(List<String> case_seq) {
		List<List<String>> res = new ArrayList<>();
		String cur_seq = "";
		for (int i = 0; i < case_seq.size() - 1; ++i) {
			cur_seq += case_seq.get(i);
			List<String> newSeq = new ArrayList<>();
			newSeq.add(cur_seq);
			newSeq.add(case_seq.get(i + 1));
			res.add(newSeq);
		}
		return res;
	}

	public List<EventProb> predictSeqWithProbs(ArrayList<ArrayList<String>> test_seq) {
		List<String> seq = new ArrayList<>();
		for (List<String> s : test_seq) {
			seq.add(String.join(",", s));
		}
		List<List<String>> seqs = new ArrayList<>();
		seqs.add(seq);
		return predictWithProb(seqs);
	}

	public EventProb predictOneWithProb(List<String> test_seq) {
		StringBuilder query = new StringBuilder();
		if (test_seq.size() == 0) {
			query = new StringBuilder("root");
		} else {
			for (String line : test_seq) {
				query.append(line);
			}
		}
		/* System.out.println("query: " + query); */
		Integer total = 0;
		String pred = NOPRED;
		double prob = 0.0;
		if (trainMap.containsKey(query.toString())) {
			HashMap<String, Integer> all_con = trainMap.get(query.toString());
			for (String strkey : all_con.keySet()) {
				total += all_con.get(strkey);
			}
			for (String strkey : all_con.keySet()) {
				Double p = (double) all_con.get(strkey) / (double) total;
				if (p > prob) {
					prob = p;
					pred = strkey;
				}
			}
		}
		return new EventProb(test_seq, pred, prob);
	}

	public String predictOne(List<String> test_seq) {
		return predictOneWithProb(test_seq).pred;
	}

	public List<EventProb> predictWithProb(List<List<String>> seqs) {
		List<EventProb> results = new ArrayList<EventProb>();
		for (List<String> list : seqs) {
			results.add(predictOneWithProb(list));
		}
		return results;
	}

	public List<EventProb> predictBeautifulWithProb(List<List<String>> seqs) {
		seqs = (ArrayList<List<String>>) seqs.stream().distinct().collect(Collectors.toList());
		List<EventProb> results = predictWithProb(seqs);
		this.sortResults(results);
		if (results.size() < SMALLSIZE) {
			return results;
		}
		List<EventProb> goodResults = results.stream().filter(ep -> ep.pred != NOPRED)
				.collect(Collectors.toList());
		if (goodResults.size() < TOOSMALLSIZE)
			return results;
		else if (goodResults.size() < GOODSIZE)
			return goodResults;
		return goodResults.subList(0, GOODSIZE);
	}

	public List<String> predictBeautiful(List<List<String>> test_seqs) {
		List<EventProb> result = predictBeautifulWithProb(test_seqs);
		ArrayList<String> simpleResult = new ArrayList<>();
		for (EventProb eventProb : result) {
			simpleResult.add(eventProb.pred);
		}
		return simpleResult;
	}

	private void sortResults(List<EventProb> seqs) {
		seqs.sort(new Comparator<EventProb>() {
			@Override
			public int compare(EventProb p1, EventProb p2) {
				int len1 = p1.happened.size();
				int len2 = p2.happened.size();
				// 如果两个都在sortlen一边，则按频率从大到小排序
				if ((len1 - sortLenDefault) * (len2 - sortLenDefault) > 0) {
					if (p1.prob != p2.prob) {
						return -(int) Math.signum(p1.prob - p2.prob);
					} else {
						return -(len1 - len2);
					}
					// 否则两个在sortlen两边，则按长度从大到小排序
				} else {
					if (len1 != len2) {
						return -(len1 - len2);
					} else {
						return -(int) Math.signum(p1.prob - p2.prob);
					}
				}
			}
		});
	}

	public void train(List<List<String>> trainData) {
		HashMap<String, List<String>> seq = GroupCases(trainData);
		ConvertToTrain(seq);
	}

	public void trainGrouped(HashMap<String, List<String>> seq) {
		ConvertToTrain(seq);
	}

	public static void main(String[] args) {
		EventPredictor ep = new EventPredictor();
		List<String> test_seq = Arrays.asList("1");
		List<List<String>> raw_cases = ep.ReadCSV("/home/fcg/Downloads/Model/data/helpdesk.csv");
		ep.train(raw_cases);
		System.out.println("predicted:" + ep.predictOne(test_seq));
	}
}
