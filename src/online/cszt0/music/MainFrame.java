package online.cszt0.music;

import com.sun.awt.AWTUtilities;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

/**
 * @author 初始状态0
 * @date 2019/5/22 18:49
 */
@SuppressWarnings("WeakerAccess")
public class MainFrame extends JFrame implements Runnable {

	final File musicDir = new File("music");
	final File lrcDir = new File("lrc");

	LrcFrame lrcFrame;

	JLabel currentMusic;
	JList<String> musicList;
	JProgressBar progressBar;
	JButton prevButton;
	JButton nextButton;
	JButton playButton;

	JCheckBoxMenuItem repeat;

	MediaPlayer mediaPlayer;
	TrayIcon trayIcon;

	LimitStack<String> lastMusic;
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
		repeat = new JCheckBoxMenuItem("洗脑循环");
		JMenuBar jMenuBar = new JMenuBar();
		jMenuBar.add(repeat);
		setJMenuBar(jMenuBar);
		updateAudioList();
		lastMusic = new LimitStack<>();
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
			if (mediaPlayer == null) {
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
			String[] musicExtension = {".wav", ".aac", ".mp3", ".pcm"};
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
		mainFrame.setIconImage(new ImageIcon("icon.png").getImage());
		mainFrame.setSize(new Dimension(400, 300));
		mainFrame.setLocationRelativeTo(null);
		if (SystemTray.isSupported()) {
			mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			Image icon = new ImageIcon("icon.png").getImage();
			mainFrame.trayIcon = new TrayIcon(icon, "已停止");
			mainFrame.trayIcon.setImageAutoSize(true);
			PopupMenu popupMenu = new PopupMenu();
			MenuItem reshow = new MenuItem("打开主界面");
			reshow.addActionListener(e -> {
				mainFrame.setVisible(true);
				SystemTray.getSystemTray().remove(mainFrame.trayIcon);
			});
			MenuItem exit = new MenuItem("退出");
			exit.addActionListener(e -> System.exit(0));
			popupMenu.add(reshow);
			popupMenu.addSeparator();
			popupMenu.add(exit);
			mainFrame.trayIcon.setPopupMenu(popupMenu);
			mainFrame.trayIcon.addActionListener(e -> {
				mainFrame.setVisible(true);
				SystemTray.getSystemTray().remove(mainFrame.trayIcon);
			});
			mainFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					super.windowClosing(e);
					mainFrame.setVisible(false);
					try {
						SystemTray.getSystemTray().add(mainFrame.trayIcon);
					} catch (AWTException e1) {
						e1.printStackTrace();
						System.exit(0);
					}
				}
			});
		} else {
			mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		}
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
		loadLrc(musicName);
		File file = new File(musicDir, musicName);
		Media media = new Media(file.toURI().toString());
		mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setOnEndOfMedia(this::onMusicStop);
		mediaPlayer.setOnError(() -> {
			JOptionPane.showMessageDialog(this, "无法播放：" + media.getSource() + "\n" + mediaPlayer.getError().getMessage());
			stopMusic();
		});
		mediaPlayer.play();
		playButton.setText("■");
		if (trayIcon != null) {
			trayIcon.setToolTip("正在播放：" + musicName);
		}
	}

	private void loadLrc(String musicName) {
		musicName = musicName.substring(0, musicName.lastIndexOf('.')) + ".lrc";
		File lrcFile = new File(lrcDir, musicName);
		lrcFrame.lrc = Lrc.fromFile(lrcFile);
		if (lrcFrame.lrc == null) {
			lrcFrame.lrc = new Lrc(lrcFile.getName());
		}
	}

	private synchronized void stopMusic() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.dispose();
			mediaPlayer = null;
		}
		if (trayIcon != null) {
			trayIcon.setToolTip("已停止");
		}
		lrcFrame.lrc = null;
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
				if (mediaPlayer != null) {
					progressBar.setMaximum((int) mediaPlayer.getTotalDuration().toMillis());
					progressBar.setValue((int) mediaPlayer.getCurrentTime().toMillis());
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
		synchronized (this) {
			nextMusic();
		}
	}

	private void nextMusic() {
		updateAudioList();
		if (list != null) {
			if (repeat.isSelected()) {
				playMusic(lastMusic.peek());
				return;
			}
			int len = list.length;
			playMusic(list[(int) (System.currentTimeMillis() % len)]);
		}
	}

	class LrcFrame extends JWindow implements Runnable {
		Lrc lrc;
		ProgressTextLabel text;
		ProgressTextLabel translate;

		LrcFrame() {
			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.anchor = GridBagConstraints.CENTER;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.gridx = 0;
			text = new ProgressTextLabel();
			text.setFont(new Font(Font.DIALOG, Font.PLAIN, 32));
			panel.add(text, gridBagConstraints);
			translate = new ProgressTextLabel();
			translate.setFont(new Font(Font.DIALOG, Font.PLAIN, 24));
			gridBagConstraints.gridy = 1;
			panel.add(translate, gridBagConstraints);
			setContentPane(panel);
			Thread thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}

		@Override
		public void run() {
			//noinspection InfiniteLoopStatement
			while (true) {
				try {
					// 更新文本框文字
					if (lrc == null || mediaPlayer == null) {
						text.setText("Lrc - View");
						text.setProgress(-1);
						translate.setText(null);
					} else {
						long curTime = (long) mediaPlayer.getCurrentTime().toMillis();
						Lrc.Info info = lrc.getInfoByTime(curTime);
						text.setText(info.text);
						text.setProgress(info.progress);
						translate.setText(info.subText);
						translate.setProgress(info.progress);
					}
				} catch (NullPointerException ignore) {
				}
			}
		}
	}

}
