import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Description: Write a program, topN, that given an arbitrarily large file and
 * a number, N, containing individual numbers on each line (e.g. 200Gb file),
 * will output the largest N numbers, highest first. Tell me about the run
 * time/space complexity of it, and whether you think there's room for
 * improvement in your approach.
 * 
 * 
 * Assumption: This example code assumes you have at least 1GB RAM as a starting
 * point for Java objects spaces in JVM. Each number is within 32bit integer
 * range and the count of them is within the range of 64bit long.
 * 
 * Time complexity: (1) split into sub files O(n) (2) calculate HashMap for each
 * sub file O(n) (3) keep the maximum heap with size k, you need to compare all
 * the elements with elements in the heap O(log(k)) * O(n) Final time
 * complexity: O(n), if k is relatively small to n
 * 
 * 
 * Space complexity: (1) split into sub files O(n) (2) calculate HashMap for
 * each sub file O(n) (3) keep the maximum heap with size k, you need to compare
 * all the elements with elements in the heap O(k) Final time complexity: O(n),
 * if k is relatively small to n
 * 
 * Improvement: (1) Split files: class LineNumberReader and multi-thread pattern
 * might save some time when we split the file on multi-core cpu or distributed
 * system. It allows to read the same file with multiple thread on different
 * parts in parellel. (2) .
 * 
 * @author ZhenshengTan
 *
 */
public class TopK {

	private int topNum;
	private PriorityQueue<Count> countsHeap;

	/**
	 * Representing key as the number, and value as the count of that number, to
	 * be used in the sorting.
	 * 
	 * @author ZhenshengTan
	 *
	 */
	public class Count {
		private long count;
		private int key;

		public Count(int key, long count) {
			this.count = count;
			this.key = key;
		}
	}

	public class CountComparator implements Comparator<Count> {
		@Override
		public int compare(Count x, Count y) {
			if (x.count > y.count)
				return -1;
			else if (x.count < y.count)
				return 1;
			else
				return 0;
		}
	}

	public TopK(int topNum) {
		this.topNum = topNum;
		countsHeap = new PriorityQueue<Count>(topNum, new CountComparator());
	};

	/**
	 * This is to split the big file e.g. 200GB into fileNum It splits the big
	 * file into number specifies
	 * 
	 * @param input
	 *            file.
	 * @param fileNum
	 *            integer range.
	 * @return whether successfully split. The split is based on modular
	 *         operation of the number per line. For example, origin big file
	 *         has numbers like: 100,102,103,104,...200,201,202 If
	 *         fileNum(modular) is 100, after split then Sub file 0 contains
	 *         number: 100,200 Sub file 1 contains number: 101,201 Sub file 2
	 *         contains number: 102,202 ...
	 * 
	 *         This makes counting a number easier, we only need to count a
	 *         number in one sub file in the next step. But then we need to
	 *         consider the worst case, a lot of ints fall in one sub file, to
	 *         see if the RAM could hold the hash table for each sub file.
	 * 
	 *         For example, for 200GB, we could split into 1024(2ˆ10) sub files.
	 *         Why split into around 1000 files? 1.Assuming a scenario we have
	 *         1GB Java Heap size in Java VM 32bit, integer is within 2ˆ32. 2.We
	 *         want to hold a HashtMap with <Integer,Integer> type, I did a
	 *         experiment it needs 20B per item let's make it 32B max and then
	 *         2ˆ5 B. 3. We could hold 2ˆ32 / 2ˆ5 = 2 ˆ27 items only in the
	 *         memory for a big hash map. 4. Assuming the worst case all
	 *         integers appear in one sub file. In one sub file, the only range
	 *         appear is 2ˆ32 / 2ˆ10 = 2ˆ22 . 5. So the memory could hold 2 ˆ27
	 *         items, and the worst case, all integer appear in one sub file, is
	 *         only 2ˆ22. In theory, we could hold every sub file in a hash map
	 *         format. This is important. The theory is at least to split files
	 *         into 32 (2ˆ5) pieces or more (because 2ˆ32 / 2ˆ27 = 2ˆ5) if the
	 *         number is Integer type in 32bit.
	 * @throws URISyntaxException
	 */
	public boolean splitFiles(File input, int fileNum)
			throws URISyntaxException {
		if (input == null || !input.exists())
			return false;
		String newLineChar = System.getProperty("line.separator");
		String fileName = input.getName();
		FileInputStream fstream = null;
		BufferedReader br = null;
		HashMap<Integer, BufferedWriter> writers = new HashMap<Integer, BufferedWriter>();

		try {

			fstream = new FileInputStream(input);
			br = new BufferedReader(new InputStreamReader(fstream));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				int mod = (Integer.valueOf(strLine) % fileNum);
				if (!writers.containsKey(mod)) {
					BufferedWriter writer = new BufferedWriter(new FileWriter(
							new File(getPath(fileName)
									+ getSubFileName(fileName,
											String.valueOf(mod)))));
					writers.put(mod, writer);
					writer.write(strLine);
				} else {
					writers.get(mod).write(newLineChar + strLine);
				}
			}
		} catch (IOException e1) {
			System.err.println("IO error at split file step.");
			e1.printStackTrace();
			return false;
		} finally {
			if (br != null) {
				try {
					br.close();
					Iterator<Map.Entry<Integer, BufferedWriter>> it = writers
							.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<Integer, BufferedWriter> entry = it.next();
						entry.getValue().close();
					}
				} catch (IOException e) {
					System.err.println("IO error at split file step.");
					e.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}

	public static String getSubFileName(String name, String index) {
		return name.substring(0, name.lastIndexOf(".")) + "_sub_" + index
				+ ".txt";
	}

	public static String getPath(String name) throws URISyntaxException {
		String path = TopK.class.getResource(name).toURI().getPath();
		path = path.substring(0,
				path.lastIndexOf(System.getProperty("file.separator")) + 1);
		return path;
	}

	/**
	 * multi-thread pattern could save some time, mind the system file
	 * descriptor limits
	 * 
	 * @param input
	 */
	public void readAndCreateCountsMap(File inputFolder, String oriFilename) {
		for (final File fileEntry : inputFolder.listFiles()) {
			if (fileEntry.getName().equals(oriFilename)
					|| !fileEntry.getName().contains("txt"))
				continue;
			if (!fileEntry.isDirectory()) {
				FileInputStream fstream = null;
				BufferedReader br = null;
				HashMap<Integer, Long> counts = new HashMap<Integer, Long>();
				// Start creating hash map for a sub file
				try {
					fstream = new FileInputStream(new File(
							fileEntry.getAbsolutePath()));
					br = new BufferedReader(new InputStreamReader(fstream));
					String strLine;
					while ((strLine = br.readLine()) != null) {
						Integer key = Integer.valueOf(strLine);
						if (!counts.containsKey(key)) {
							counts.put(key, 1L);
						} else {
							counts.put(key, counts.get(key) + 1L);
						}
					}
				} catch (IOException e) {
					System.err.println("IO error at sub files hash map step.");
					e.printStackTrace();
				} finally {
					if (br != null) {
						try {
							br.close();
						} catch (IOException e) {
							System.err
									.println("IO error at sub files hash map step.");
							e.printStackTrace();
						}
					}
				}
				// End creating hash map for a sub file

				// Start putting the hashmap data to the PriorityQueue, a max
				// heap with size K
				Iterator<Map.Entry<Integer, Long>> it = counts.entrySet()
						.iterator();
				while (it.hasNext()) {
					Map.Entry<Integer, Long> entry = it.next();
					Count c = new Count(entry.getKey(), entry.getValue());
					if (countsHeap.size() == topNum)
						countsHeap.remove();
					countsHeap.offer(c);
				}
				// End putting the hashmap data to the PriorityQueue, a max heap
				// with size K.
			}
		}

	}

	public synchronized void printTopK() {
		long start4 = System.currentTimeMillis();

		List<Count> temp = new ArrayList<Count>();
		while (!countsHeap.isEmpty()) {
			Count c = countsHeap.poll();
			temp.add(c);
			System.out.println(((Count) c).key + " " + ((Count) c).count);
		}

		for (Count c : temp) {
			countsHeap.offer(c);
		}
		float secTaken4 = (System.currentTimeMillis() - start4) / 1000F;
		System.out.println("Get Top K takes " + secTaken4 + "s");
	}

	/**
	 * The name that will be generated at the classpath and tested.
	 * 
	 * @param fileNameAtClassPath
	 */
	public boolean process(String fileNameAtClassPath, int fileSizeInMB) {
		try {
			final File[] files = new File(getPath(this.getClass().getSimpleName()+".class")).listFiles( new FilenameFilter() {
			    @Override
			    public boolean accept( final File dir,
			                           final String name ) {
			        return name.matches( ".*\\.txt" );
			    }
			} );
			for ( final File file : files ) {
			    if ( !file.delete() ) {
			        System.err.println( "Can't remove " + file.getAbsolutePath() );
			    }
			}
	
			File file = new File(getPath(this.getClass().getSimpleName()+".class") + fileNameAtClassPath);
			if (!file.exists())
				file.createNewFile();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IO error at create file step.");
			return false;
		} catch (URISyntaxException urie) {
			urie.printStackTrace();
			System.err.println("class path error at create file step.");
			return false;
		}

		// Fill the new file with random integers till the size is reached, the
		// size counted in MB. 20480 means 20GB for example.
		long start = System.currentTimeMillis();
		try {
			this.createLargeFile(fileNameAtClassPath, fileSizeInMB);
			float secTaken = (System.currentTimeMillis() - start) / 1000F;
			System.out.println("Creating big file with random integers"
					+ fileSizeInMB + "MB takes " + secTaken + "s");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			System.err.println("class path error at create large file step.");
			return false;
		}

		// Split the big files into chunks. For example, in this case 1024
		// chunks.
		// This is to
		// This guarantee every number only appears in one single file.
		long start2 = System.currentTimeMillis();
		try {
			this.splitFiles(new File(getPath(fileNameAtClassPath)
					+ fileNameAtClassPath), 1024);
			float secTaken2 = (System.currentTimeMillis() - start2) / 1000F;
			System.out.println("Splitting files takes " + secTaken2 + "s");
		} catch (URISyntaxException e) {
			System.err.println("class path error at split file step.");
			e.printStackTrace();
			return false;
		}

		long start3 = System.currentTimeMillis();
		try {
			this.readAndCreateCountsMap(new File(getPath(fileNameAtClassPath)),
					fileNameAtClassPath);
			float secTaken3 = (System.currentTimeMillis() - start3) / 1000F;
			System.out.println("Totally processing all sub files take "
					+ secTaken3 + "s");
		} catch (URISyntaxException e) {
			System.err.println("URI syntax error at sub files hash map step.");
			e.printStackTrace();
			return false;
		}

		return true;

	}

	public static void main(String[] args) {

		// Creating a top K class to hold the maxinum heap here. Example here is
		// top 10.
		TopK topK = new TopK(10);

		// Create a file at the class path for experiment
		String filename = "very_big_file.txt";

		// Create a file containing random integer with 10GB.
		topK.process(filename, 1024);
		topK.printTopK();

	}

	public void createLargeFile(String filename, int MB)
			throws URISyntaxException {
		// Random integer range, we could use MAX VALUE, but the random algo
		// provieded by Java is quite evenly distributed. So the worst case is one number appear once.
		int range = Integer.MAX_VALUE;
		range = 1000000;
		String newLineChar = System.getProperty("line.separator");
		BufferedWriter writer = null;
		try {

			File file = new File(getPath(filename) + filename);
			writer = new BufferedWriter(new FileWriter(file, false));
			Random rand = new Random();
			int num = rand.nextInt(range);
			writer.write(String.valueOf(num));
			while (file.length() / 1024 / 1024 < MB) {
				writer.write(newLineChar + num);
				num = rand.nextInt(range);
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.err.println("IO error at create large file step.");
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("IO error at create large file step.");
				}
		}
	}

	/**
	 * A way to calculate the average hash map item size in Main method.
	 */
	// System.out.println("100 objects: " + calSize(100));
	// System.out.println("1000 objects: " + calSize(1000));
	// System.out.println("10000 objects: " + calSize(10000));
	// System.out.println("100000 objects: " + calSize(100000));

	/**
	 * 20 Byte per item for HashMap
	 * 
	 * @param s
	 * @return
	 * @throws IOException
	 */
	public static int calSize1(int s) throws IOException {
		HashMap<Integer, Integer> table = new HashMap<Integer, Integer>();
		for (int i = 0; i < s; i++) {
			table.put(new Integer(i), new Integer(i));
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(table);
		oos.close();
		return baos.size();
	}

	/**
	 * 28 Byte per item for HashMap
	 * 
	 * @param s
	 * @return
	 * @throws IOException
	 */
	public static int calSize2(int s) throws IOException {
		HashMap<Long, Long> table = new HashMap<Long, Long>();
		for (int i = 0; i < s; i++) {
			table.put(new Long(i), new Long(i));
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(table);
		oos.close();
		return baos.size();
	}

}
