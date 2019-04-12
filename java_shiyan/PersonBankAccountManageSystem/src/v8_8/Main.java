/**
 * 
 */
package v8_8;

import java.util.*;
import java.io.*;

/**
 * @className Main
 * @description Main.java
 * @author 31415926535x
 * @date 2019��3��5�� ����9:23:46
 * @tag 
 * @version v8_8
 */
public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		//��ʼ����
		Date date = new Date(2008, 11, 1);
		
		//���������˻�
		SavingsAccount sa1 = new SavingsAccount(date, "S3755217", 0.015);
		SavingsAccount sa2 = new SavingsAccount(date, "02342342", 0.015);
		CreditAccount  ca = new CreditAccount(date, "C5392394", 10000, 0.0005, 50);
		
		Account[] accounts = new Account[]{sa1, sa2, ca};
		final int n = accounts.length;
		
		System.out.println("(d)deposit (w)withdraw (s)show (c)change day (n)next month (e)exit");
		
		char cmd;
		Scanner input = new Scanner(System.in);
		String tmpString;
		cmd = 's';
		do {
			//��ʾ���ں��ܽ��
			date.show();
			System.out.print("\tTotal: " + Account.getTotal() + "\tcommand> ");
			
			int index, day;
			double amount;
			String desc;
			
			tmpString = input.next();
			cmd = tmpString.charAt(0);

			switch (cmd) {
			case 'd':	//�����ֽ�
				index = input.nextInt();
				amount = input.nextInt();
				desc = input.nextLine();
				accounts[index].deposit(date, amount, desc);
				break;
			case 'w':	//ȡ���ֽ�
				index = input.nextInt();
				amount = input.nextInt();
				desc = input.nextLine();
				accounts[index].withdraw(date, amount, desc);
				break;
			case 's':	//��ѯ���˻���Ϣ
				for(int i = 0; i < n; ++i) {
					System.out.print("[" + i + "] ");
					accounts[i].show();
					System.out.println();
				}
				break;
			case 'c':	//�ı�����
				day = input.nextInt();
				if(day < date.getDay()) {
					System.out.print("You cannot specify a previous day");
				}
				else if(day > date.getMaxDay()) {
					System.out.print("Invalid day");
				}
				else {
					date = new Date(date.getYear(), date.getMonth(), day);
				}
				break;
			case 'n': 	//�����¸���
				if(date.getMonth() == 12) {
					date = new Date(date.getYear() + 1, 1, 1);
				}
				else {
					date = new Date(date.getYear(), date.getMonth() + 1, 1);
				}
				for(int i = 0; i < n; ++i)
					accounts[i].settle(date);
				break;
			}			
		}while(cmd != 'e');
	}
}