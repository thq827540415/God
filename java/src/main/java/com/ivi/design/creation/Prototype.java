package com.ivi.design.creation;


import com.ivi.algorithm.tree.TreeNode;

import java.util.*;

public class Prototype {

    private static class SearchWord {

        public long getLastUpdateTime() {
            return 0L;
        }

        public String getKeyword() {
            return "";
        }
    }

    private HashMap<String, SearchWord> currentKeyWords = new HashMap<>();
    private long lastUpdateTime = -1;

    //
    private void refresh() {
        List<SearchWord> toBeUpdateSearchWords = getSearchWords(lastUpdateTime);
        long maxNewUpdatedTime = lastUpdateTime;

        for (SearchWord searchWord : toBeUpdateSearchWords) {
            if (searchWord.getLastUpdateTime() > maxNewUpdatedTime) {
                maxNewUpdatedTime = searchWord.getLastUpdateTime();
            }

            if (currentKeyWords.containsKey(searchWord.getKeyword())) {
                // currentKeyWords.replace();
            }
        }
    }


    // 从数据库中获取 更新时间 > lastUpdateTime 的数据
    private List<SearchWord> getSearchWords(long lastUpdateTime) {
        return null;
    }


    public static void main(String[] args) {
    }
}
