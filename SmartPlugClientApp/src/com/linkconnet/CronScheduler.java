package com.linkconnet;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class CronScheduler {
	
	public static void main(String[] args) {
		int min = -1, hour = -1, day = 20, month = 10;
		byte dow = 0x03;
		
		Calendar c = Calendar.getInstance();
		c.get(Calendar.DAY_OF_WEEK);
		
		int c_min = c.get(Calendar.MINUTE) + 1, c_hour = c.get(Calendar.HOUR),
				c_day = c.get(Calendar.DAY_OF_MONTH), c_month = c.get(Calendar.MONTH);
		byte c_dow = 0x0;
		switch(c.get(Calendar.DAY_OF_WEEK)) {
		case Calendar.SUNDAY:
			c_dow = (byte)0x80;
			break;
		case Calendar.MONDAY:
			c_dow = (byte)0x40;
			break;
		case Calendar.TUESDAY:
			c_dow = (byte)0x20;
			break;
		case Calendar.WEDNESDAY:
			c_dow = (byte)0x10;
			break;
		case Calendar.THURSDAY:
			c_dow = (byte)0x08;
			break;
		case Calendar.FRIDAY:
			c_dow = (byte)0x04;
			break;
		case Calendar.SATURDAY:
			c_dow = (byte)0x02;
			break;
		}
		
		if (min != -1 && min != c_min) {
			if(c_min > min) {
				c_hour++;
			}
			
			c_min = min;
		}
		
		if (hour != -1 && hour != c_hour) {
			if(c_hour > hour) {
				c_day ++;
			}
			
			c_hour = hour;
		}
	}

}
