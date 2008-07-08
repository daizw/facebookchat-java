package facebookchat.ui.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.MenuElement;
import javax.swing.ScrollPaneConstants;

import facebookchat.common.FacebookUser;
import facebookchat.common.Launcher;
import facebookchat.ui.common.ObjectList;

@SuppressWarnings("serial")
public class ListsPane extends JPanel {
	private JScrollPane buddyListScrPane;

	ObjectList buddyList;
	Cheyenne parent;
	
	FacebookUser buddy;
	
	public ListsPane(Cheyenne par) {
		parent = par;
		
		buddyList = new ObjectList();
		
		// add to gui
		buddyListScrPane = new JScrollPane(buddyList,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		this.add(buddyList.getFilterField());
		this.add(buddyListScrPane);
		
		buddyList.addMouseListener(new MouseListener(){
			@SuppressWarnings("serial")
			public void mouseClicked(MouseEvent me) {
				buddy = (FacebookUser)buddyList.getSelectedValue();
				if(buddy == null)
					return;
				if(me.getClickCount() == 2){
					//TODO �ж��������cell������״̬���ж�Ӧ����, ��ʱֱ�ӵ����������촰��.
					/**
					 * TODO Ӧ�ö�ÿһ������ֻ��һ������, �����趨���, ����Ѿ�����һ������ʾ֮, �����´���
					 */
					ListsPane.this.showChatroom(buddy.uid);
				}else if(me.getButton() == MouseEvent.BUTTON3){
					final JPopupMenu friendOprMenu = new JPopupMenu();
					//System.out.println("You just Right Click the List Item!");
					friendOprMenu.add(new AbstractAction("Talk to him/her") {
						public void actionPerformed(ActionEvent e) {
							ListsPane.this.showChatroom(buddy.uid);
						}
					});
					friendOprMenu.add(new AbstractAction("His/Her information") {
						public void actionPerformed(ActionEvent e) {
							JOptionPane.showMessageDialog((Component) null, 
									"<html>"//<BODY bgColor=#ffffff>"
									+ "<img width=64 height=64 src=\""
									+ buddy.thumbSrc
									+ "\"><br>"
									+"<Font color=black>Name:</Font> <Font color=blue>"
									+ buddy.name
									+ "<br></Font>"
									+ "<Font color=black>UID:</Font> <Font color=blue>"
									+ buddy.uid
									+ "<br></Font>"
									+ "<Font color=black>Status:</Font> <Font color=blue>"
									+ buddy.onlineStatus.toString()
									+ "<br></Font>"
									+ "<a href=\"http://www.facebook.com/profile.php?id=" 
									+ buddy.uid
									+ "\"   >"
									+ "http://www.facebook.com/profile.php?id="
									+ buddy.uid
									+ "</a>"
									+ "</BODY></html>",
									"User Information", JOptionPane.INFORMATION_MESSAGE);
						}//http://www.facebook.com/profile.php?id=1190346972
					});
					MenuElement els[] = friendOprMenu.getSubElements();
					for(int i = 0; i < els.length; i++)
						els[i].getComponent().setBackground(Color.WHITE);
					friendOprMenu.setLightWeightPopupEnabled(true);
					friendOprMenu.pack();
					// λ��Ӧ���������Դ��λ��
					friendOprMenu.show((Component) me.getSource(), me.getPoint().x, me.getPoint().y);
				}
			}
			public void mouseEntered(MouseEvent arg0) {
			}
			public void mouseExited(MouseEvent arg0) {
			}
			public void mousePressed(MouseEvent arg0) {
			}
			public void mouseReleased(MouseEvent arg0) {
			}
		});
		
		this.setBackground(Color.BLACK);
		this.setForeground(Color.WHITE);

		this.setOpaque(false);
	}
	/**
	 * ����TooltipTxt��html��ʽ
	 * @param text
	 * @return
	 */
	public static String getHtmlText(String text) {
		return ("<html><BODY bgColor=#ffffff><Font color=black>" + text + "</Font></BODY></html>");
	}
	/**
	 * (��������˫�����ѻ�����ʱ������)�������촰��.
	 * @param listItem
	 */
	private void showChatroom(String uid) {
		Launcher.getChatroomAnyway(uid).setVisible(true);
	}
	public void refresh() {
		buddyList.update();
	}
}