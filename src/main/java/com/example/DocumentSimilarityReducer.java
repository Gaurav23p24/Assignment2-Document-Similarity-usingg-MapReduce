package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class DocumentSimilarityReducer extends Reducer<Text, Text, Text, Text> {

    // In-memory maps to store state during the reduce phase
    private Map<String, Integer> docSizes = new HashMap<>();
    private Map<String, Integer> intersectionCounts = new HashMap<>();
    
    private Text resultKey = new Text();
    private Text resultValue = new Text();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        // The key is a word.
        // The values are a list of "docId:docSize" strings.

        // 1. Collect all document info for this word.
        List<String[]> docInfos = new ArrayList<>();
        for (Text val : values) {
            String[] parts = val.toString().split(":");
            if (parts.length == 2) {
                docInfos.add(parts);
                // Store the document size. This might be redundant but ensures we have it.
                docSizes.put(parts[0], Integer.parseInt(parts[1]));
            }
        }

        // 2. Generate pairs and update intersection counts.
        if (docInfos.size() > 1) {
            for (int i = 0; i < docInfos.size(); i++) {
                for (int j = i + 1; j < docInfos.size(); j++) {
                    String doc1 = docInfos.get(i)[0];
                    String doc2 = docInfos.get(j)[0];
                    
                    // Create a consistent key for the pair, e.g., "Doc1,Doc2"
                    String pairKey;
                    if (doc1.compareTo(doc2) < 0) {
                        pairKey = doc1 + "," + doc2;
                    } else {
                        pairKey = doc2 + "," + doc1;
                    }

                    // Increment the intersection count for this pair.
                    intersectionCounts.put(pairKey, intersectionCounts.getOrDefault(pairKey, 0) + 1);
                }
            }
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        // This method is called once after all reduce calls are done.
        
        // 3. Iterate through all pairs and calculate Jaccard Similarity.
        for (Map.Entry<String, Integer> entry : intersectionCounts.entrySet()) {
            String[] docIds = entry.getKey().split(",");
            int intersection = entry.getValue();

            int doc1Size = docSizes.getOrDefault(docIds[0], 0);
            int doc2Size = docSizes.getOrDefault(docIds[1], 0);

            // |A U B| = |A| + |B| - |A intersect B|
            double union = doc1Size + doc2Size - intersection;
            
            if (union > 0) {
                double similarity = intersection / union;
                
                // Format the output as specified in the assignment.
                String formattedValue = String.format("Similarity: %.2f", similarity);
                resultKey.set(entry.getKey());
                resultValue.set(formattedValue);
                context.write(resultKey, resultValue);
            }
        }
    }
}