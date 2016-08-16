package com.ambition.chat.utils;

public class Utils {

	
	
	public static int getIntervalByRole(int role) {
		int val = -1;
		if (role < 0) {
			val = 3000;
		} else if (role == 0) {
			val = 2000;
		} else if (role >= 8) {
			val = 0;
		} else if ((role & 0x07) > 0) {
			val = 1000;
		}
		return val;
	}
}
