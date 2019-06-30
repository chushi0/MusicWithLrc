package online.cszt0.music;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 初始状态0
 * @date 2019/5/22 20:56
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Lrc {
	private static final String OFFSET_LABEL = "offset";

	private final String filename;
	private LinkedList<Statement> statements;
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private Map<String, String> infoMap;

	public Lrc(String filename) {
		statements = new LinkedList<>();
		infoMap = new HashMap<>();
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0) {
			this.filename = filename.substring(0, dotIndex);
		} else {
			this.filename = filename;
		}
	}

	public Info getInfoByTime(long time) {
		Statement res = null;
		long endTime = 5000;
		for (Statement statement : statements) {
			if (statement.time <= time) {
				res = statement;
				endTime = statement.time + 5000;
			} else {
				endTime = statement.time;
				break;
			}
		}
		if (res == null) {
			return new Info(filename, null, Math.min((float) time / (float) endTime, 1f));
		}
		// 歌词的中间关键点
		int textStart = 0;
		int textEnd = res.text.length();
		long textStartTime = res.time;
		long textEndTime = endTime;
		int textBreakSize = res.textBreakPoints.length;
		for (int i = 0; i < textBreakSize; i++) {
			long breakTime = res.textBreakTimes[i];
			if (time >= breakTime) {
				textStart = res.textBreakPoints[i];
				textStartTime = breakTime;
			} else {
				textEnd = res.textBreakPoints[i];
				textEndTime = breakTime;
				break;
			}
		}
		float textProgress = Math.min((float) (time - textStartTime) / (float) (textEndTime - textStartTime), 1f);
		// 翻译的中间关键点
		int subTextStart = 0;
		int subTextEnd = res.subText.length();
		long subTextStartTime = res.time;
		long subTextEndTime = endTime;
		int subTextBreakSize = res.subTextBreakPoints.length;
		for (int i = 0; i < subTextBreakSize; i++) {
			long breakTime = res.subTextBreakTimes[i];
			if (time >= breakTime) {
				subTextStart = res.subTextBreakPoints[i];
				subTextStartTime = breakTime;
			} else {
				subTextEnd = res.subTextBreakPoints[i];
				subTextEndTime = breakTime;
				break;
			}
		}
		float subTextProgress = Math
				.min((float) (time - subTextStartTime) / (float) (subTextEndTime - subTextStartTime), 1f);
		// 返回
		return new Info(res.text, res.subText, textProgress, textStart, textEnd, subTextProgress, subTextStart,
				subTextEnd);
	}

	public int getIndexByTime(long time) {
		int index = -1;
		for (Statement statement : statements) {
			if (statement.time > time) {
				break;
			}
			index++;
		}
		return index;
	}

	public Info getInfoByIndex(int index) {
		Statement statement = statements.get(index);
		return new Info(statement.text, statement.subText, Float.NaN);
	}

	public synchronized void endStatementByTime(long time) {
		for (Statement statement : statements) {
			if (statement.time > time) {
				statement.time = time;
				return;
			}
		}
		Statement statement = statements.getLast();
		addMainBreak(statement, statement.text.length(), time);
		if (statement.subText != null && !statement.subText.isEmpty()) {
			addSubBreak(statement, statement.subText.length(), time);
		}
	}

	public void addMainBreakByTime(int index, long time) {
		if (index == 0) {
			return;
		}
		Statement statement = getStatementByTime(time);
		if (statement == null) {
			return;
		}
		addMainBreak(statement, index, time);
	}

	public void addSubBreakByTime(int index, long time) {
		if (index == 0) {
			return;
		}
		Statement statement = getStatementByTime(time);
		if (statement == null) {
			return;
		}
		addSubBreak(statement, index, time);
	}

	private Statement getStatementByTime(long time) {
		Statement res = null;
		for (Statement statement : statements) {
			if (statement.time <= time) {
				res = statement;
			} else {
				return res;
			}
		}
		return res;
	}

	private void addSubBreak(Statement statement, int index, long time) {
		int[] breakPoints = statement.subTextBreakPoints;
		long[] breakTimes = statement.subTextBreakTimes;
		int count = breakPoints.length;
		int insertIndex = getInsertIndex(breakPoints, index);
		statement.subTextBreakPoints = new int[count + 1];
		statement.subTextBreakTimes = new long[count + 1];
		System.arraycopy(breakPoints, 0, statement.subTextBreakPoints, 0, insertIndex);
		statement.subTextBreakPoints[insertIndex] = index;
		System.arraycopy(breakPoints, insertIndex, statement.subTextBreakPoints, insertIndex + 1, count - insertIndex);
		System.arraycopy(breakTimes, 0, statement.subTextBreakTimes, 0, insertIndex);
		statement.subTextBreakTimes[insertIndex] = time;
		System.arraycopy(breakTimes, insertIndex, statement.subTextBreakTimes, insertIndex + 1, count - insertIndex);
	}

	private void addMainBreak(Statement statement, int index, long time) {
		int[] breakPoints = statement.textBreakPoints;
		long[] breakTimes = statement.textBreakTimes;
		int count = breakPoints.length;
		int insertIndex = getInsertIndex(breakPoints, index);
		statement.textBreakPoints = new int[count + 1];
		statement.textBreakTimes = new long[count + 1];
		System.arraycopy(breakPoints, 0, statement.textBreakPoints, 0, insertIndex);
		statement.textBreakPoints[insertIndex] = index;
		System.arraycopy(breakPoints, insertIndex, statement.textBreakPoints, insertIndex + 1, count - insertIndex);
		System.arraycopy(breakTimes, 0, statement.textBreakTimes, 0, insertIndex);
		statement.textBreakTimes[insertIndex] = time;
		System.arraycopy(breakTimes, insertIndex, statement.textBreakTimes, insertIndex + 1, count - insertIndex);
	}

	private int getInsertIndex(int[] breakPoints, int index) {
		int len = breakPoints.length;
		for (int i = 0; i < len; i++) {
			if (breakPoints[i] > index) {
				return i;
			}
		}
		return len;
	}

	public static Lrc fromFile(File file) {
		String name = file.getName().toLowerCase();
		if (name.endsWith(".txt")) {
			return parseFileAsRawText(file);
		}
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			Pattern labelPattern = Pattern.compile("\\[\\s*(\\w+)\\s*:\\s*([^]]+)\\s*]");
			Pattern timePattern = Pattern.compile("\\[\\s*(\\d+)\\s*:\\s*(\\d+)\\s*\\.\\s*(\\d+)\\s*]([^\\[]*)");
			Lrc lrcInfo = new Lrc(file.getName());
			String line;
			long offset = 0;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				// 元信息
				Matcher matcher = labelPattern.matcher(line);
				if (matcher.matches()) {
					String key = matcher.group(1);
					String value = matcher.group(2);
					if (key.equalsIgnoreCase(OFFSET_LABEL)) {
						try {
							offset = Long.parseLong(value);
						} catch (NumberFormatException ignored) {
						}
					} else {
						lrcInfo.infoMap.put(key, value);
					}
					continue;
				}
				// 歌词
				matcher = timePattern.matcher(line);
				boolean find = matcher.find();
				if (find && matcher.start() == 0) {
					long time = Long.parseLong(matcher.group(1)) * 60 * 1000 + Long.parseLong(matcher.group(2)) * 1000
							+ Long.parseLong(matcher.group(3));
					String text = matcher.group(4);
					Statement statement = new Statement();
					statement.time = time;
					StringBuilder statementBuilder = new StringBuilder(text);
					ArrayList<Integer> breakPoints = new ArrayList<>();
					ArrayList<Long> breakTimes = new ArrayList<>();
					int index = text.length();
					while (matcher.find()) {
						breakPoints.add(index);
						breakTimes.add(Long.parseLong(matcher.group(1)) * 60 * 1000
								+ Long.parseLong(matcher.group(2)) * 1000 + Long.parseLong(matcher.group(3)));
						text = matcher.group(4);
						statementBuilder.append(text);
						index += text.length();
					}
					statement.text = statementBuilder.toString();
					statement.textBreakPoints = list2intArray(breakPoints);
					statement.textBreakTimes = list2LongArray(breakTimes);
					statement.subText = "";
					statement.subTextBreakTimes = new long[0];
					statement.subTextBreakPoints = new int[0];
					lrcInfo.statements.add(statement);
					continue;
				}
				// 译文
				if (!find) {
					Statement statement = lrcInfo.statements.getLast();
					statement.subText = line;
					statement.subTextBreakPoints = new int[0];
					statement.subTextBreakTimes = new long[0];
					continue;
				}
				int index = matcher.start();
				StringBuilder subTextBuilder = new StringBuilder(line.substring(0, index));
				ArrayList<Integer> breakPoints = new ArrayList<>();
				ArrayList<Long> breakTimes = new ArrayList<>();
				String text;
				do {
					breakPoints.add(index);
					breakTimes.add(Long.parseLong(matcher.group(1)) * 60 * 1000
							+ Long.parseLong(matcher.group(2)) * 1000 + Long.parseLong(matcher.group(3)));
					text = matcher.group(4);
					subTextBuilder.append(text);
					index += text.length();
				} while (matcher.find());
				Statement statement = lrcInfo.statements.getLast();
				statement.subText = subTextBuilder.toString();
				statement.subTextBreakPoints = list2intArray(breakPoints);
				statement.subTextBreakTimes = list2LongArray(breakTimes);
			}
			// 重新更新时间
			if (offset != 0) {
				for (Statement statement : lrcInfo.statements) {
					statement.time += offset;
					int size = statement.textBreakTimes.length;
					for (int i = 0; i < size; i++) {
						statement.textBreakTimes[i] += offset;
					}
					if (statement.subTextBreakPoints != null) {
						size = statement.subTextBreakTimes.length;
						for (int i = 0; i < size; i++) {
							statement.subTextBreakTimes[i] += offset;
						}
					}
				}
			}
			return lrcInfo;
		} catch (IOException e) {
			return null;
		}
	}

	private static long[] list2LongArray(ArrayList<Long> list) {
		int size = list.size();
		long[] res = new long[size];
		for (int i = 0; i < size; i++) {
			res[i] = list.get(i);
		}
		return res;
	}

	private static int[] list2intArray(ArrayList<Integer> list) {
		int size = list.size();
		int[] res = new int[size];
		for (int i = 0; i < size; i++) {
			res[i] = list.get(i);
		}
		return res;
	}

	private static Lrc parseFileAsRawText(File file) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			Lrc lrcInfo = new Lrc(file.getName());
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				Statement statement = new Statement();
				statement.time = Long.MAX_VALUE;
				statement.text = line;
				lrcInfo.statements.add(statement);
			}
			return lrcInfo;
		} catch (IOException e) {
			return null;
		}
	}

	public boolean isStatementEditableByTime(long time) {
		if (statements.isEmpty()) {
			return false;
		}
		return statements.getFirst().time <= time;
	}

	public synchronized void setStatementByTime(long time, String main, String sub) {
		Statement res = null;
		for (Statement statement : statements) {
			if (statement.time <= time) {
				res = statement;
			} else {
				break;
			}
		}
		assert res != null;
		if (!Objects.equals(res.text, main)) {
			res.text = main;
			res.textBreakPoints = new int[0];
			res.textBreakTimes = new long[0];
		}
		if (!Objects.equals(res.subText, sub)) {
			res.subText = sub;
			res.subTextBreakPoints = new int[0];
			res.subTextBreakTimes = new long[0];
		}
	}

	public String[] toStringArray() {
		int size = statements.size();
		String[] arr = new String[size];
		int index = 0;
		for (Statement statement : statements) {
			arr[index++] = statement.text;
		}
		return arr;
	}

	public synchronized void setStatementByIndex(int position, String main, String sub) {
		Statement statement = statements.get(position);
		if (!Objects.equals(statement.text, main)) {
			statement.text = main;
			statement.textBreakPoints = new int[0];
			statement.textBreakTimes = new long[0];
		}
		if (!Objects.equals(statement.subText, sub)) {
			statement.subText = sub;
			statement.subTextBreakPoints = new int[0];
			statement.subTextBreakTimes = new long[0];
		}
	}

	public synchronized void clearTime() {
		for (Statement statement : statements) {
			statement.time = Long.MAX_VALUE;
			statement.textBreakTimes = new long[0];
			statement.textBreakPoints = new int[0];
			statement.subTextBreakPoints = new int[0];
			statement.subTextBreakTimes = new long[0];
		}
	}

	public synchronized void deleteStatement(int position) {
		statements.remove(position);
	}

	public synchronized void clearTime(int position) {
		int size = statements.size();
		if (position == size - 1) {
			Statement statement = statements.getLast();
			statement.time = Long.MAX_VALUE;
			statement.textBreakTimes = new long[0];
			statement.textBreakPoints = new int[0];
			statement.subTextBreakPoints = new int[0];
			statement.subTextBreakTimes = new long[0];
		} else {
			Statement statement = statements.get(position);
			statement.time = statements.get(position + 1).time;
			statement.textBreakTimes = new long[0];
			statement.textBreakPoints = new int[0];
			statement.subTextBreakPoints = new int[0];
			statement.subTextBreakTimes = new long[0];
		}
	}

	public synchronized void addAfter(int position) {
		Statement statement = new Statement();
		int size = statements.size();
		if (position == size - 1) {
			statement.time = Long.MAX_VALUE;
		} else {
			statement.time = statements.get(position + 1).time;
		}
		statements.add(position + 1, statement);
	}

	public synchronized void addBefore(int position) {
		Statement statement = new Statement();
		Statement curStatement = statements.get(position);
		statement.time = curStatement.time;
		if (position == 0 && position != statements.size() - 1) {
			curStatement.time = statements.get(position + 1).time;
		}
		statements.add(position, statement);
	}

	public void writeToStream(OutputStream outputStream) {
		PrintStream printStream;
		try {
			printStream = new PrintStream(outputStream, false, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		// 元信息
		Set<HashMap.Entry<String, String>> entries = infoMap.entrySet();
		for (HashMap.Entry<String, String> entry : entries) {
			printStream.printf("[%s:%s]%n", entry.getKey(), entry.getValue());
		}
		// 歌词信息
		for (Statement statement : statements) {
			printStatement(printStream, statement);
		}
	}

	public String getStatementText(int index) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		PrintStream printStream;
		try {
			printStream = new PrintStream(byteArrayOutputStream,false, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		printStatement(printStream, statements.get(index));
		String s = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
		return s.substring(0, s.length() - 1);
	}

	public boolean replaceStatement(String statementText, int position) {
		Pattern timePattern = Pattern.compile("\\[\\s*(\\d+)\\s*:\\s*(\\d+)\\s*\\.\\s*(\\d+)\\s*]([^\\[]*)");
		Scanner scanner = new Scanner(statementText.replace("\r", ""));
		String line = scanner.nextLine();
		line = line.trim();
		Matcher matcher;
		Statement statement = new Statement();
		// 歌词
		matcher = timePattern.matcher(line);
		boolean find = matcher.find();
		if (find && matcher.start() == 0) {
			long time = Long.parseLong(matcher.group(1)) * 60 * 1000 + Long.parseLong(matcher.group(2)) * 1000
					+ Long.parseLong(matcher.group(3));
			String text = matcher.group(4);
			statement.time = time;
			StringBuilder statementBuilder = new StringBuilder(text);
			ArrayList<Integer> breakPoints = new ArrayList<>();
			ArrayList<Long> breakTimes = new ArrayList<>();
			int index = text.length();
			while (matcher.find()) {
				breakPoints.add(index);
				breakTimes.add(Long.parseLong(matcher.group(1)) * 60 * 1000
						+ Long.parseLong(matcher.group(2)) * 1000 + Long.parseLong(matcher.group(3)));
				text = matcher.group(4);
				statementBuilder.append(text);
				index += text.length();
			}
			statement.text = statementBuilder.toString();
			statement.textBreakPoints = list2intArray(breakPoints);
			statement.textBreakTimes = list2LongArray(breakTimes);
			statement.subText = "";
			statement.subTextBreakTimes = new long[0];
			statement.subTextBreakPoints = new int[0];
		} else {
			return false;
		}
		// 翻译
		if (scanner.hasNext()) {
			line = scanner.nextLine();
			line = line.trim();
			matcher = timePattern.matcher(line);
			find = matcher.find();
			// 译文
			if (!find) {
				statement.subText = line;
				statement.subTextBreakPoints = new int[0];
				statement.subTextBreakTimes = new long[0];
			} else {
				int index = matcher.start();
				StringBuilder subTextBuilder = new StringBuilder(line.substring(0, index));
				ArrayList<Integer> breakPoints = new ArrayList<>();
				ArrayList<Long> breakTimes = new ArrayList<>();
				String text;
				do {
					breakPoints.add(index);
					breakTimes.add(Long.parseLong(matcher.group(1)) * 60 * 1000
							+ Long.parseLong(matcher.group(2)) * 1000 + Long.parseLong(matcher.group(3)));
					text = matcher.group(4);
					subTextBuilder.append(text);
					index += text.length();
				} while (matcher.find());
				statement.subText = subTextBuilder.toString();
				statement.subTextBreakPoints = list2intArray(breakPoints);
				statement.subTextBreakTimes = list2LongArray(breakTimes);
			}
		}
		if (scanner.hasNext()) {
			return false;
		}
		// 替换
		statements.set(position, statement);
		return true;
	}

	private void printStatement(PrintStream printStream, Statement statement) {
		long time = statement.time;
		printStream.printf("[%d:%d.%d]", time / 60 / 1000, time / 1000 % 60, time % 1000);
		// 考虑到关键点
		int textBreakPointCount = statement.textBreakPoints.length;
		int lastIndex = 0;
		for (int i = 0; i < textBreakPointCount; i++) {
			time = statement.textBreakTimes[i];
			printStream.printf("%s[%d:%d.%d]", statement.text.substring(lastIndex, statement.textBreakPoints[i]),
					time / 60 / 1000, time / 1000 % 60, time % 1000);
			lastIndex = statement.textBreakPoints[i];
		}
		printStream.println(statement.text.substring(lastIndex));
		if (!statement.subText.isEmpty()) {
			int subTextBreakPointCount = statement.subTextBreakPoints.length;
			lastIndex = 0;
			for (int i = 0; i < subTextBreakPointCount; i++) {
				time = statement.subTextBreakTimes[i];
				printStream.printf("%s[%d:%d.%d]",
						statement.subText.substring(lastIndex, statement.subTextBreakPoints[i]), time / 60 / 1000,
						time / 1000 % 60, time % 1000);
				lastIndex = statement.subTextBreakPoints[i];
			}
			printStream.println(statement.subText.substring(lastIndex));
		}
	}

	public long getStartTimeByIndex(int index) {
		return statements.get(index).time;
	}

	public static class Info {
		public final String text;
		public final String subText;
		public final float textProgress;
		public final int textStart;
		public final int textEnd;
		public final float subTextProgress;
		public final int subTextStart;
		public final int subTextEnd;

		public Info(String text, String subText, float progress) {
			this.text = text;
			this.subText = subText == null ? "" : subText;
			this.textProgress = progress;
			this.subTextProgress = progress;
			this.textStart = 0;
			this.textEnd = text.length();
			this.subTextStart = 0;
			this.subTextEnd = this.subText.length();
		}

		public Info(String text, String subText, float textProgress, int textStart, int textEnd, float subTextProgress,
		            int subTextStart, int subTextEnd) {
			this.text = text;
			this.subText = subText;
			this.textProgress = textProgress;
			this.textStart = textStart;
			this.textEnd = textEnd;
			this.subTextProgress = subTextProgress;
			this.subTextStart = subTextStart;
			this.subTextEnd = subTextEnd;
		}
	}

	private static class Statement {
		private long time;
		private String text;
		private String subText;
		private int[] textBreakPoints;
		private long[] textBreakTimes;
		private int[] subTextBreakPoints;
		private long[] subTextBreakTimes;

		Statement() {
			text = "";
			textBreakPoints = new int[0];
			textBreakTimes = new long[0];
		}
	}
}