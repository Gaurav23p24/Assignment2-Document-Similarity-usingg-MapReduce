# Assignment 2 — Document Similarity using MapReduce

**Course:** Cloud Computing for Data Analysis (ITCS 6190/8190, Fall 2025)  
**Instructor:** Marco Vieira  
**Student:** Gaurav Bharatkumar Patel (801426641)

---

## 📌 Overview

This project computes **pairwise Jaccard Similarity** between documents using **Hadoop MapReduce**.  
Each line of the input file contains a `DocumentID` followed by its text. The pipeline:

1. **Parses** each line, normalizes case, and removes punctuation.
2. **Builds sets of unique tokens** per document.
3. **Counts intersections** of document pairs via shared tokens across reducers.
4. **Computes** Jaccard = \|A ∩ B\| / \|A ∪ B\| and outputs `DocumentX, DocumentY  Similarity: s.xx`.

---

## 🧠 Approach & Implementation

### Mapper (`DocumentSimilarityMapper`)
- **Input:** `(LongWritable offset, Text line)`
- **Steps:**
  1. Split into `docID` and `content`.
  2. Normalize: lowercase + strip punctuation.
  3. Tokenize and insert into a `HashSet<String>` to get **unique** terms; size ⇒ `docSize`.
  4. **Emit:** `(word, "docID:docSize")` for every unique word in the document.
- **Example emit:** `("spark", "Document12:157")`

### Reducer (`DocumentSimilarityReducer`)
- **Input:** `key = word`, `values = ["docID:docSize", ...]`
- **reduce()**
  - For the given `word`, collect all `(docID, docSize)` pairs.
  - Record per-document sizes in `docSizes: Map<String,Integer>`.
  - Generate **unique pairs** among docs containing `word` and increment  
    `intersectionCounts: Map<Pair, Integer>` for that pair.
- **cleanup()**
  - For each pair `(A,B)` in `intersectionCounts`:
    - `inter = intersectionCounts[A,B]`
    - `|A| = docSizes[A]`, `|B| = docSizes[B]`
    - `union = |A| + |B| - inter`
    - `jaccard = inter / union` → **rounded to two decimals**
  - **Emit:** `"A,B"  →  "Similarity: 0.xx"`

> This single job approach avoids materializing all pairs in the mapper and leverages reducer-side aggregation of intersections.

---

# Setup and Execution

Follow these steps to run the Document Similarity MapReduce job on the Hadoop Docker cluster.

---

## 1. Start the Hadoop Cluster

Run the following command to start the Docker containers for Hadoop:

```bash
docker compose up -d
````

---

## 2. Build the Code

Build the Java project using Maven, which compiles the code and packages it into a JAR file:

```bash
mvn clean package
```

---

## 3. Copy JAR to Docker Container

Copy the generated JAR file into the `resourcemanager` container (replace the JAR name if different):

```bash
docker cp target/DocumentSimilarity-0.0.1-SNAPSHOT.jar resourcemanager:/opt/hadoop-3.2.1/share/hadoop/mapreduce/
```
---

## 4. Copy Dataset to Docker Container

Copy your input dataset (for example, `small_dataset.txt`) into the container:

```bash
docker cp input/small_dataset.txt resourcemanager:/opt/hadoop-3.2.1/share/hadoop/mapreduce/
```

---

## 5. Execute the MapReduce Job

First, connect to the container, move the dataset into HDFS, and then run the MapReduce job.

```bash
# Access the container's command line
docker exec -it resourcemanager /bin/bash

# Navigate to the Hadoop directory inside the container
cd /opt/hadoop-3.2.1/share/hadoop/mapreduce/

# Create an input directory in HDFS (only needed once)
hadoop fs -mkdir -p /input/docsim

# Copy the dataset from the container's local disk to HDFS
hadoop fs -put ./small_dataset.txt /input/docsim

# Execute the job
# NOTE: The output directory (/output/small_results) MUST NOT exist before running the job
hadoop jar DocumentSimilarity-0.0.1-SNAPSHOT.jar com.example.controller.DocumentSimilarityDriver /input/docsim/small_dataset.txt /output/small_results
```

## 6. View and Copy the Output

After the job finishes, you can view the results and copy them from HDFS to the container local filesystem, then to your host machine.

```bash
# View the output directly in the container terminal
hadoop fs -cat /output/small_results/*

# Copy the output from HDFS to the container's local disk
hdfs dfs -get /output/small_results .

# Exit the container shell
exit

# From your host (Codespace) terminal, copy the results from the container to your project folder
docker cp resourcemanager:/opt/hadoop-3.2.1/share/hadoop/mapreduce/small_results/ shared-folder/output/
```
## 📑 Sample Input Snippets

### 🟢 Small Dataset (Excerpt)
```text
Document1  This is a small dataset containing simple text for testing purposes
Document2  The quick brown fox jumps over the lazy dog in this example sentence
Document3  Spark makes big data processing simple and efficient with its APIs
Document4  Machine learning models require clean structured and labeled data
Document5  Text processing often involves tokenization filtering and normalization
```
### 🟢 Small Dataset
```
Document101  Distributed computing allows processing large volumes of data efficiently
Document102  MapReduce jobs split tasks into map and reduce phases to scale horizontally
Document103  Data engineers frequently use Hadoop and Spark for batch data processing
Document104  Fault tolerance is a key feature of HDFS which replicates blocks across nodes
Document105  Tuning block size and parallelism parameters improves performance on clusters
```
### 🟢 Small Dataset
```
Document2001  Machine learning on big data often requires feature engineering at scale
Document2002  Clusters must be monitored to avoid resource bottlenecks and data skew
Document2003  Joins in distributed systems can cause expensive shuffles if not optimized
Document2004  Using combiners reduces intermediate data size and improves job efficiency
Document2005  Real-world pipelines often combine batch processing with streaming analytics
```
### Observations:
```
• The 3-node cluster consistently outperformed the 1-node cluster across all datasets.
• Performance gain was minimal for the small dataset but significant for medium and large datasets.
• The parallel execution of map and reduce tasks distributed the load efficiently.
• Using "hdfs dfs -put -f" ensured smooth overwriting of input datasets between test runs.
• Clearing old HDFS output directories before re-runs prevented job failures due to existing paths.
```

---


## 🗂️ Repository Layout

```text
Assignment2-Document-Similarity-usingg-MapReduce/
├─ src/
│  └─ main/
│     └─ java/
│        └─ com/
│           └─ example/
│              ├─ mapper/
│              │  └─ DocumentSimilarityMapper.java
│              ├─ reducer/
│              │  └─ DocumentSimilarityReducer.java
│              └─ controller/
│                 └─ DocumentSimilarityDriver.java
├─ input/
│  ├─ small_dataset.txt
│  ├─ medium_dataset.txt
│  └─ large_dataset.txt
├─ outputs/
│  ├─ 3nodes/
│  │  ├─ small_results.txt
│  │  ├─ medium_results.txt
│  │  └─ large_results.txt
│  └─ 1node/
│     ├─ small_results_1node.txt
│     ├─ medium_results_1node.txt
│     └─ large_results_1node.txt
├─ docker-compose.yml
├─ pom.xml
├─ README.md
└─ target/                    # generated by Maven; usually not committed
   └─ DocumentSimilarity-0.0.1-SNAPSHOT.jar
```

---

## Sample Outputs (Truncated)

### Small Dataset
| Document Pair | Similarity |
|--------------|-----------|
| Document10,Document19 | 0.42 |
| Document20,Document3  | 0.53 |
| Document10,Document18 | 0.50 |
| Document20,Document4  | 0.63 |
| Document20,Document5  | 0.61 |
| Document7,Document9   | 0.60 |
| Document13,Document16 | 0.67 |
| Document18,Document6  | 0.83 |
| Document1,Document15  | 0.69 |
| Document4,Document7   | 0.74 |

---

### Medium Dataset
| Document Pair | Similarity |
|--------------|-----------|
| Document13,Document2  | 0.84 |
| Document22,Document4  | 0.89 |
| Document22,Document27 | 0.90 |
| Document2,Document24  | 0.95 |
| Document32,Document47 | 0.85 |
| Document25,Document36 | 1.00 |
| Document47,Document7  | 0.89 |
| Document16,Document30 | 0.95 |
| Document23,Document6  | 0.88 |
| Document27,Document4  | 0.90 |

---

### Large Dataset
| Document Pair | Similarity |
|--------------|-----------|
| Document89,Document96 | 0.95 |
| Document62,Document74 | 0.89 |
| Document71,Document78 | 1.00 |
| Document31,Document79 | 0.95 |
| Document48,Document66 | 1.00 |
| Document48,Document73 | 1.00 |
| Document18,Document71 | 0.95 |
| Document18,Document99 | 1.00 |
| Document65,Document84 | 0.95 |
| Document48,Document85 | 1.00 |


---

## Challenges & Actions Taken

| Challenge | Action Taken |
|----------|--------------|
| **ClassNotFoundException for Driver Class** | Initially, the JAR built with Maven could not locate the `DocumentSimilarityDriver`. This happened because the Java files were placed directly under `src/main/` instead of `src/main/java/`. After restructuring the project to follow the Maven convention and rebuilding, the JAR executed successfully. |
| **JAR Missing After Docker Restart** | Each `docker compose down` removed the container, wiping the previously copied JAR. The solution was to always re-copy the built JAR into the namenode container after starting the cluster. |
| **HDFS Output Not Accessible from Host** | Attempting to `docker cp` results directly from `/output` failed because Hadoop stores job outputs in HDFS. The fix was to first use `hdfs dfs -get` inside the namenode to copy results to the container’s local filesystem, and then use `docker cp` to bring them to the Codespace host. |
| **Performance Measurement Across Clusters** | Needed accurate timing to compare 3-node vs 1-node runs. Used the shell’s built-in `time` command to measure job runtime for each dataset, recording map + reduce execution times. |
| **Overwriting Inputs During Iteration** | While re-running tests with updated datasets, old HDFS inputs conflicted. Solved this by using `hdfs dfs -put -f` which overwrites existing files safely. |
