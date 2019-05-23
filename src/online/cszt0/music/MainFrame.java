package online.cszt0.music;

import com.sun.awt.AWTUtilities;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

/**
 * @author 初始状态0
 * @date 2019/5/22 18:49
 */
public class MainFrame extends JFrame implements Runnable {

	final File musicDir = new File("music");
	final File lrcDir = new File("lrc");
	final Pattern pattern = Pattern.compile("\\d+");

	LrcFrame lrcFrame;

	JLabel currentMusic;
	JList<String> musicList;
	JProgressBar progressBar;
	JButton prevButton;
	JButton nextButton;
	JButton playButton;

	AudioPlayer audioPlayer;
	AudioStream audioStream;

	Stack<String> lastMusic;
	String[] list;

	private MainFrame() {
		super("随机播放器");
		JPanel contentPanel = new JPanel(new BorderLayout());
		JPanel controlPanel = new JPanel(new BorderLayout());
		currentMusic = new JLabel();
		contentPanel.add(currentMusic, BorderLayout.NORTH);
		musicList = new JList<>();
		contentPanel.add(new JScrollPane(musicList), BorderLayout.CENTER);
		contentPanel.add(controlPanel, BorderLayout.SOUTH);
		prevButton = new JButton("|<");
		nextButton = new JButton(">|");
		playButton = new JButton("▶");
		progressBar = new JProgressBar();
		JPanel realControlPanel = new JPanel();
		realControlPanel.add(prevButton);
		realControlPanel.add(playButton);
		realControlPanel.add(nextButton);
		controlPanel.add(realControlPanel, BorderLayout.SOUTH);
		controlPanel.add(progressBar, BorderLayout.CENTER);
		setContentPane(contentPanel);
		updateAudioList();
		audioPlayer = AudioPlayer.player;
		lastMusic = new Stack<>();
		musicList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() == 2) {
					playMusic(musicList.getSelectedValue());
				}
			}
		});
		playButton.addActionListener(event -> {
			if (audioStream == null) {
				String music = musicList.getSelectedValue();
				if (music == null) {
					JOptionPane.showMessageDialog(this, "请先选择音乐");
					return;
				}
				playMusic(music);
			} else {
				stopMusic();
			}
		});
		prevButton.addActionListener(event -> {
			lastMusic.peek();
			playMusic(lastMusic.peek());
			lastMusic.peek();
		});
		nextButton.addActionListener(event -> nextMusic());
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	private void updateAudioList() {
		File[] files = musicDir.listFiles((dir1, name) -> {
			name = name.toLowerCase();
			// 识别的音频文件
			String[] musicExtension = {".wav"};
			boolean pass = false;
			for (String extension : musicExtension) {
				if (name.endsWith(extension)) {
					pass = true;
					break;
				}
			}
			if (!pass) {
				return false;
			}
			// 有歌词文件
			int dotIndex = name.lastIndexOf('.');
			String rawName = name.substring(0, dotIndex);
			return new File(lrcDir, rawName + ".lrc").exists();
		});
		if (files == null) {
			musicList.setListData(new String[0]);
			return;
		}
		int length = files.length;
		String[] names = new String[length];
		for (int i = 0; i < length; i++) {
			names[i] = files[i].getName();
		}
		musicList.setListData(list = names);
	}

	static void showFrame() {
		MainFrame mainFrame = new MainFrame();
		mainFrame.setSize(new Dimension(400, 300));
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
		mainFrame.lrcFrame = mainFrame.new LrcFrame();
		mainFrame.lrcFrame.setAlwaysOnTop(true);
		mainFrame.lrcFrame.setSize(1920, 100);
		AWTUtilities.setWindowOpaque(mainFrame.lrcFrame, false);
		mainFrame.lrcFrame.setVisible(true);
	}

	private synchronized void playMusic(String musicName) {
		stopMusic();
		lastMusic.push(musicName);
		try {
			loadLrc(musicName);
			File file = new File(musicDir, musicName);
			audioStream = new AudioStream(new FileInputStream(file));
			audioPlayer.start(audioStream);
			playButton.setText("■");
			progressBar.setMaximum(audioStream.available());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadLrc(String musicName) throws IOException {
		musicName = musicName.substring(0, musicName.lastIndexOf('.')) + ".lrc";
		lrcFrame.lrcs = null;
		lrcFrame.name = musicName.substring(0, musicName.lastIndexOf('.'));
		ArrayList<Lrc> lrcs = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(lrcDir, musicName)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (!line.startsWith("[")) {
					lrcs.get(lrcs.size() - 1).translate = line;
					continue;
				}
				int timeEnd = line.indexOf(']');
				String timeString = line.substring(1, timeEnd);
				Matcher matcher = pattern.matcher(timeString);
				if (!matcher.find()) {
					continue;
				}
				String minute = matcher.group();
				if (!matcher.find()) {
					continue;
				}
				String second = matcher.group();
				if (!matcher.find()) {
					continue;
				}
				String ms = matcher.group();
				long time = Long.parseLong(minute) * 60 * 1000 + Long.parseLong(second) * 1000 + Long.parseLong(ms);
				Lrc e = new Lrc();
				e.time = time;
				e.text = line.substring(timeEnd + 1);
				lrcs.add(e);
			}
		}
		lrcFrame.lrcs = lrcs;
		lrcFrame.startTime = System.currentTimeMillis();
	}

	private synchronized void stopMusic() {
		if (audioStream != null) {
			audioPlayer.stop(audioStream);
			try {
				audioStream.close();
				audioStream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		lrcFrame.lrcs = null;
		playButton.setText("▶");
	}

	@Override
	public void dispose() {
		stopMusic();
		super.dispose();
	}

	@Override
	public void run() {
		while (true) {
			synchronized (this) {
				if (audioStream != null) {
					try {
						int available = audioStream.available();
						progressBar.setValue(progressBar.getMaximum() - available);
						if (available == 0) {
							onMusicStop();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	private void onMusicStop() {
		nextMusic();
	}

	private void nextMusic() {
		updateAudioList();
		if (list != null) {
			int len = list.length;
			playMusic(list[(int) (System.currentTimeMillis() % len)]);
		}
	}

	class LrcFrame extends JWindow implements Runnable {
		ArrayList<Lrc> lrcs;
		String name;
		JLabel text;
		JLabel translate;
		long startTime;

		LrcFrame() {
			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.anchor = GridBagConstraints.CENTER;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.gridx = 0;
			text = new JLabel();
			text.setFont(new Font(Font.DIALOG, Font.PLAIN, 32));
			text.setForeground(Color.BLUE);
			panel.add(text, gridBagConstraints);
			translate = new JLabel();
			translate.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
			translate.setForeground(Color.BLUE);
			gridBagConstraints.gridy = 1;
			panel.add(translate, gridBagConstraints);
			setContentPane(panel);
			Thread thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}

		@Override
		public void run() {
			while (true) {
				// 更新文本框文字
				if (lrcs == null) {
					text.setText("Lrc - View");
					translate.setText(null);
				} else {
					long curTime = System.currentTimeMillis() - startTime;
					String t = name;
					String tran = null;
					for (Lrc lrc : lrcs) {
						if (curTime > lrc.time) {
							t = lrc.text;
							tran = lrc.translate;
						} else {
							break;
						}
					}
					text.setText(t);
					translate.setText(tran);
				}
				try {
					sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

}
