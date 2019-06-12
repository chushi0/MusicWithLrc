package online.cszt0.music;

import com.sun.javafx.application.PlatformImpl;

/**
 * @author 初始状态0
 * @date 2019/5/22 18:47
 */
public class Main {
	public static void main(String[] args) {
		PlatformImpl.startup(MainFrame::showFrame);
	}
}
