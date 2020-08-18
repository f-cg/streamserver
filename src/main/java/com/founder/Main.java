//
//author:   zxyeh
//datatime: 2020-08-17 00:41:06
//

package cn.edu.pku;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {
    private static final String COMMA_DELIMITER = ",";

    public static List<List<String>> ReadCSV(String url) {
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

    public static HashMap<String, List<String>> GroupCases(List<List<String>> raw_cases) {
        HashMap<String, List<String>> seq = new HashMap<>();
        for (int i = 1; i < raw_cases.size(); ++i) {
            String case_id = raw_cases.get(i).get(0);
            String activity_id = raw_cases.get(i).get(1);
//            String complete_timestamp = raw_cases.get(i).get(2);
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

    public static HashMap<String, HashMap<String, Integer>> ConvertToTrain(HashMap<String, List<String>> seq) {
        HashMap<String, HashMap<String, Integer>> train = new HashMap<>();
        HashMap<String, Integer> newHashMap = new HashMap<>();
        train.put("root", newHashMap);
        for (String strkey : seq.keySet()) {
            List<String> case_seq = seq.get(strkey);
            Insert(train, "root", case_seq.get(0));
            List<List<String>> s_seq = SubSeq(case_seq);
            for (List<String> s : s_seq) {
                Insert(train, s.get(0), s.get(1));
            }
        }
        return train;
    }

    public static void Insert(HashMap<String, HashMap<String, Integer>> train_dist, String key, String value) {
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

    public static List<List<String>> SubSeq(List<String> case_seq) {
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

    public static void ComputeProb(HashMap<String, HashMap<String, Integer>> train, List<String> test_seq) {
        StringBuilder query = new StringBuilder();
        if (test_seq.size() == 0) {
            query = new StringBuilder("root");
        } else {
            for (String line : test_seq) {
                query.append(line);
            }
        }
        System.out.println("query: " + query);
        Integer total = 0;
        if (train.containsKey(query.toString())) {
            HashMap<String, Integer> all_con = train.get(query.toString());
            for (String strkey : all_con.keySet()) {
                total += all_con.get(strkey);
            }
            for (String strkey : all_con.keySet()) {
                Double prob = (double) all_con.get(strkey) / (double) total;
                System.out.println(String.format("%s: %f", strkey, prob));
            }
        } else {
            System.out.println("no path found.");
        }
    }

    public static void main(String[] args) {
        List<String> test_seq = Arrays.asList("1", "6");
        List<List<String>> raw_cases = ReadCSV("C:\\Users\\zxyeh\\IdeaProjects\\Model\\data\\helpdesk.csv");
        HashMap<String, List<String>> seq = GroupCases(raw_cases);
        HashMap<String, HashMap<String, Integer>> train = ConvertToTrain(seq);
        ComputeProb(train, test_seq);
    }
}
