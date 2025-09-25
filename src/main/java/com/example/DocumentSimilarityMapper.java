package com.example;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class DocumentSimilarityMapper extends Mapper<Object, Text, Text, Text> {

    private Text word = new Text();
    private Text docIdAndSize = new Text();

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        // 1. Get the line as a string.
        String line = value.toString();

        // 2. Split into document ID and content.
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2) {
            return; // Skip malformed lines
        }
        
        String docId = parts[0];
        String textContent = parts[1];

        // 3. Pre-process text: lowercase and remove punctuation.
        textContent = textContent.toLowerCase().replaceAll("[^a-zA-Z\\s]", "");

        // 4. Tokenize and find all unique words to calculate doc size.
        StringTokenizer itr = new StringTokenizer(textContent);
        Set<String> uniqueWords = new HashSet<>();
        while (itr.hasMoreTokens()) {
            uniqueWords.add(itr.nextToken());
        }
        int docSize = uniqueWords.size();

        // 5. For each unique word, emit the word as key and "docId:docSize" as value.
        for (String uniqueWord : uniqueWords) {
            word.set(uniqueWord);
            docIdAndSize.set(docId + ":" + docSize);
            context.write(word, docIdAndSize);
        }
    }
}