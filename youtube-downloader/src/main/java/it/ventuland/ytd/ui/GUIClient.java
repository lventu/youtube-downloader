/**
 *  This file is part of ytd2
 *
 *  ytd2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ytd2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with ytd2.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package it.ventuland.ytd.ui;

import it.ventuland.ytd.DownloaderStub;
import it.ventuland.ytd.config.IConfiguration;
import it.ventuland.ytd.config.YtdConfigManager;
import it.ventuland.ytd.event.DownloadEvent;
import it.ventuland.ytd.event.DownloaderListener;
import it.ventuland.ytd.utils.URLUtils;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


public class GUIClient extends JFrame implements ActionListener, WindowListener, DocumentListener, ChangeListener, DropTargetListener {

	public static final String szVersion = "V20131210_0613e by MrKnödelmann";

	private static final long serialVersionUID = 6791957129816930254L;

	private static final String newline = "\n";

	// more or less (internal) output
	// set to True or add 'd' after mod-time
	private static boolean bDEBUG = GUIClient.szVersion.matches("V[0-9]+_[0-9]+d.*");

	// just report file size of HTTP header - don't download binary data (the video)
	private static boolean bNODOWNLOAD = bDEBUG;

	// save diskspace - try to download e.g. 720p before 1080p if HD is set
	public static boolean bSaveDiskSpace = false;

	private static String sproxy = null;

	public static String szDLSTATE = "downloading ";

	// RFC-1123 ? hostname [with protocol]	
	public static final String szPROXYREGEX = "(^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?([a-zA-Z0-9]+:[a-zA-Z0-9]+@)?([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*(:[0-90-90-90-9]{1,4})?$)|()";

	// RFC-1738 URL characters - not a regex for a real URL!!
	public static final String szURLREGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?[a-zA-Z0-9;/\\?@=&\\$\\-_\\.+!*\\'\\(\\),%]+$"; // without ":", plus "%"

	private static final String szPLAYLISTREGEX = "/view_play_list\\?p=([A-Za-z0-9]*)&playnext=[0-9]{1,2}&v=";

	// all characters that do not belong to an HTTP URL - could be written shorter?? (where did I use this?? dont now anymore)
	final String snotsourcecodeurl = "[^(a-z)^(A-Z)^(0-9)^%^&^=^\\.^:^/^\\?^_^-]";

	private static Boolean bQuitrequested = false;

	JPanel panel = null;
	JSplitPane middlepane = null;
	static JTextArea textarea = null;
	JList<String> urllist = null;
	JButton quitbutton = null;
	JButton directorybutton = null;
	JTextField directorytextfield = null;
	static JTextField textinputfield = null;
	static JRadioButton hdbutton = null;
	static JRadioButton stdbutton = null;
	static JRadioButton ldbutton = null;
	static JRadioButton mpgbutton = null;
	static JRadioButton flvbutton = null;
	static JRadioButton webmbutton = null;
	JCheckBox saveconfigcheckbox = null;
	static JCheckBox save3dcheckbox = null;

	UrlList mUrlQueue = null;
	DownloaderStub downloadExecutor = null;
	IConfiguration mConfiguration = null;

	private DefaultListModel<String> dlm = null;

	enum eCLIdownloadQuality { LD, SD, HD} ;
	static eCLIdownloadQuality CLIdownloadQuality ;
	enum eCLIdownloadFormat { MPG, WEBM, FLV };
	static eCLIdownloadFormat CLIdownloadFormat; 

	public GUIClient( ) {
		initialize();
	}

	protected void initialize() {

		mConfiguration = YtdConfigManager.getInstance();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});

		try {
			UIManager.setLookAndFeel( "javax.swing.plaf.metal.MetalLookAndFeel" );
			javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					try {
						initializeUI();
						initializeThreads();
					}  catch (java.awt.HeadlessException he) {
						System.exit(1);
					}
				}
			});
		} catch (java.lang.InternalError ie) {
			System.exit(1);
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	public static synchronized Boolean getbQuitrequested() {
		return bQuitrequested;
	}

	public static synchronized String getProxy() {
		return sproxy;
	}

	public synchronized static void setbQuitrequested(Boolean bQuitrequested) {
		GUIClient.bQuitrequested = bQuitrequested;
	}

	/**
	 * get state of downloadbutton as Integer 
	 * 
	 * @return
	 */
	public synchronized static int getIdlbuttonstate() {
		return ((hdbutton.isSelected()?4:0) + (stdbutton.isSelected()?2:0) + (ldbutton.isSelected()?1:0));
	}


	/**
	 * get state of formatbutton for mpg as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean getBmpgbuttonstate() {
		return (mpgbutton.isSelected()); 
	}

	/**
	 * get state of formatbutton for flv as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean getBflvbuttonstate() {
		return (flvbutton.isSelected()); 
	}

	/**
	 * get state of formatbutton for webm as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean getBwebmbuttonstate() {
		return (webmbutton.isSelected()); 
	}


	/**
	 * get state of 3dbutton as Boolean 
	 * 
	 * @return
	 */
	public synchronized static Boolean get3Dbuttonstate() {
		return (save3dcheckbox.isSelected()); 
	}	

	/**
	 * append text to textarea
	 * 
	 * @param Object o
	 */
	public static void addTextToConsole( Object o ) {
		try {
			textarea.append( o.toString().concat( newline ) );
			textarea.setCaretPosition( textarea.getDocument().getLength() );
			textinputfield.requestFocusInWindow();
		} catch (NullPointerException npe) {
			// for CLI run only
		} catch (Exception e) {
		}
	}


	private void addYTURLToList( String sname ) {
		String sn = sname;
		// bring all URLs into the same form
		if (sname.toLowerCase().startsWith("youtube")) {
			sn = "http://www.".concat(sname);
		}
		if (sname.toLowerCase().startsWith("www")) {
			sn = "http://".concat(sname);
		}
		synchronized (dlm) {
			dlm.addElement( sn );
//			dlm.notify();
		}
		try {
			mUrlQueue.setElement(sn);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void exchangeYTURLInList( String sfromname, String stoname) {
		synchronized (dlm) {
			try {
				int i = dlm.indexOf( sfromname );
				dlm.setElementAt(stoname, i);
			} catch (Throwable t) {}
		}
	}

	private void removeURLFromList( String sname ) {
		synchronized (dlm) {
			try {
				int i = dlm.indexOf( sname );
				dlm.remove( i );
			} catch (IndexOutOfBoundsException ioobe) {}
		}
	}
/*
	private void clearURLList() {
		try {
			synchronized (dlm) {
				dlm.clear();
			}
		} catch (NullPointerException npe) {}
	}
*/
	private void shutdown() {
		if (saveconfigcheckbox.isSelected()) {
			mConfiguration.saveConfiguration(sproxy, getIdlbuttonstate(), getBmpgbuttonstate());
		}
		GUIClient.setbQuitrequested(true);
		downloadExecutor.killAll();
		this.dispose();
	} 

	/**
	 * @param string
	 * @param regex
	 * @param replaceWith
	 * @return changed String
	 */
	String replaceAll(String string, String regex, String replaceWith) {
		Pattern myPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		return (myPattern.matcher(string).replaceAll(replaceWith));
	} 

	/**
	 * process events of ActionListener
	 * 
	 */
	public void actionPerformed( final ActionEvent e ) {
		if (e.getSource().equals( textinputfield )) {
			if (!e.getActionCommand().equals( "" )) { 
				if (e.getActionCommand().matches(URLUtils.szYTREGEX)) {
					addYTURLToList(e.getActionCommand());
				} else {
					addTextToConsole(e.getActionCommand());
				}
			}
			synchronized (textinputfield) {
				textinputfield.setText("");				
			}
			return;
		}

		// let the user choose another dir
		if (e.getSource().equals( directorybutton )) {
			JFileChooser fc = new JFileChooser();
			fc.setMultiSelectionEnabled(false);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			synchronized (directorytextfield) {
				// we have to set current directory here because it gets lost when fc is lost
				fc.setCurrentDirectory( new File( directorytextfield.getText()) );
			}
			if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			String snewdirectory = fc.getSelectedFile().getAbsolutePath();
			// append file.seperator if last character is not file.seperator (the user choosed a directory other than root)
			snewdirectory.concat(snewdirectory.endsWith(System.getProperty("file.separator"))?"":System.getProperty("file.separator"));
			File ftest = new File(snewdirectory);
			if (ftest.exists()) {
				if (ftest.isDirectory()) {
					synchronized (directorytextfield) {
						directorytextfield.setText( snewdirectory );
					}
					mConfiguration.setSaveDirectory(snewdirectory);
				} 
			}
			return;
		}

		// let the user choose another download resolution
		if (e.getActionCommand().equals(hdbutton.getActionCommand()) || 
				e.getActionCommand().equals(stdbutton.getActionCommand()) ||
				e.getActionCommand().equals(ldbutton.getActionCommand()) ) {

			return;
		}

		// let the user choose another video format
		if (e.getActionCommand().equals(mpgbutton.getActionCommand()) ||
				e.getActionCommand().equals(flvbutton.getActionCommand()) || 
				e.getActionCommand().equals(webmbutton.getActionCommand()) ) {
			return;
		} 

		if (e.getActionCommand().equals( "quit" )) {
			addTextToConsole("quit requested - signaling donwload threads to terminate, this may take a while!");
			// seems to have to effect:
			//repaint();
			this.shutdown();
			return;
		}

	}

	static synchronized void setbNODOWNLOAD( boolean pBNODOWNLOAD ) {
		bNODOWNLOAD = pBNODOWNLOAD;
	}

	public static synchronized boolean getbNODOWNLOAD() {
		// no download if we debug
		try {
			return(bNODOWNLOAD);
		} catch (NullPointerException npe) {
			return(GUIClient.getbDEBUG());
		}
	}

	public static synchronized boolean getbDEBUG() {
		try {
			return(bDEBUG);
		} catch (NullPointerException npe) {
			return GUIClient.szVersion.matches("V[0-9]+_[0-9]+d.*");
		}
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	private void initializeUI() {
		String sv = "YTD2 ".concat(szVersion).concat(" ").concat("http://sourceforge.net/projects/ytd2/"); // ytd2.sf.net is shorter
		setDefaultLookAndFeelDecorated(false);
		this.setTitle(sv);
		this.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		this.addComponentsToPane( this.getContentPane() );
		this.pack();
		this.setVisible( true );

		sv = "version: ".concat( szVersion ).concat(GUIClient.getbDEBUG()?" DEBUG ":"");

		sproxy = System.getenv("http_proxy");
		if (sproxy==null) {
			sproxy="";
		}
		sv = "HTTP Proxy: ".concat(sproxy);

		sv = "initial download folder: ";
		sv = sv.concat(directorytextfield.getText());
		mConfiguration.setSaveDirectory(directorytextfield.getText());
	}

	private void initializeThreads(){
		// lets respect the upload limit of google (youtube)
		// downloading is faster than viewing anyway so don't start more than six threads and don't play around with the URL-strings please!!!
		mUrlQueue = new UrlList();
		downloadExecutor = new DownloaderStub(mUrlQueue,  new DnlListener());
		downloadExecutor.startAll();
	}

	private void addComponentsToPane( final Container pane ) {
		this.panel = new JPanel();

		this.panel.setLayout( new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.anchor = GridBagConstraints.WEST;

		dlm = new DefaultListModel<String>();
		this.urllist = new JList<String>( dlm );
		// TODO maybe we add a button to remove added URLs from list?
		//this.userlist.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		this.urllist.setFocusable( false );
		textarea = new JTextArea( 2, 2 );
		textarea.setEditable( true );
		textarea.setFocusable( false );

		JScrollPane leftscrollpane = new JScrollPane( this.urllist );
		JScrollPane rightscrollpane = new JScrollPane( textarea );
		this.middlepane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, leftscrollpane, rightscrollpane );
		this.middlepane.setOneTouchExpandable( true );
		this.middlepane.setDividerLocation( 150 );

		Dimension minimumSize = new Dimension( 25, 25 );
		leftscrollpane.setMinimumSize( minimumSize );
		rightscrollpane.setMinimumSize( minimumSize );

		this.directorybutton = new JButton("", createImageIcon("images/open.png",""));
		gbc.gridx = 0;
		gbc.gridy = 0;
		this.directorybutton.addActionListener( this );
		this.panel.add( this.directorybutton, gbc );

		this.saveconfigcheckbox = new JCheckBox("Save config");
		this.saveconfigcheckbox.setSelected(false);

		this.panel.add(this.saveconfigcheckbox);

		this.saveconfigcheckbox.setEnabled(false);

		String sfilesep = System.getProperty("file.separator");

		// TODO check if initial download directory exists
		// assume that at least the users homedir exists
		String shomedir = System.getProperty("user.home").concat(sfilesep);
		if (System.getProperty("user.home").equals("/home/knoedel")) {
			shomedir = "/home/knoedel/YouTube Downloads/";
		}

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.directorytextfield = new JTextField( shomedir, 20+(GUIClient.getbDEBUG()?48:0) );
		this.directorytextfield.setEnabled( false );
		this.directorytextfield.setFocusable( true );
		this.directorytextfield.addActionListener( this );
		this.panel.add( this.directorytextfield, gbc);

		JLabel dirhint = new JLabel("Download to folder:");

		gbc.gridx = 0;
		gbc.gridy = 1;
		this.panel.add( dirhint, gbc);

		this.middlepane.setPreferredSize( new Dimension( Toolkit.getDefaultToolkit().getScreenSize().width/3, Toolkit.getDefaultToolkit().getScreenSize().height/4+(GUIClient.getbDEBUG()?200:0) ) );

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 2;
		gbc.weightx = 2;
		gbc.gridwidth = 2;
		this.panel.add( this.middlepane, gbc );

		// radio buttons for resolution to download
		hdbutton = new JRadioButton("HD"); hdbutton.setActionCommand("hd"); hdbutton.addActionListener(this); hdbutton.setToolTipText("1080p/720p");
		stdbutton = new JRadioButton("Std"); stdbutton.setActionCommand("std"); stdbutton.addActionListener(this); stdbutton.setToolTipText("480p/360p");
		ldbutton = new JRadioButton("LD"); ldbutton.setActionCommand("ld"); ldbutton.addActionListener(this); ldbutton.setToolTipText("< 360p");

		stdbutton.setSelected(true);
		hdbutton.setEnabled(true);
		ldbutton.setEnabled(true);

		ButtonGroup bgroup = new ButtonGroup();
		bgroup.add(hdbutton);
		bgroup.add(stdbutton);
		bgroup.add(ldbutton);

		JPanel radiopanel = new JPanel(new GridLayout(1,0));
		radiopanel.add(hdbutton);
		radiopanel.add(stdbutton);
		radiopanel.add(ldbutton);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 0;
		gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		this.panel.add( radiopanel, gbc );

		// radio buttons for video format to download
		mpgbutton = new JRadioButton("MPEG"); mpgbutton.setActionCommand("mpg"); mpgbutton.addActionListener(this); mpgbutton.setToolTipText("Codec: H.264 MPEG-4");
		webmbutton = new JRadioButton("WEBM"); webmbutton.setActionCommand("webm"); webmbutton.addActionListener(this); webmbutton.setToolTipText("Codec: Google/On2's VP8 or Googles WebM");
		flvbutton = new JRadioButton("FLV"); flvbutton.setActionCommand("flv"); flvbutton.addActionListener(this); flvbutton.setToolTipText("Codec: Flash Video (FLV1)");

		bgroup = new ButtonGroup();
		bgroup.add(mpgbutton);
		bgroup.add(webmbutton);
		bgroup.add(flvbutton);

		mpgbutton.setSelected(true);
		mpgbutton.setEnabled(true);
		webmbutton.setEnabled(true);
		flvbutton.setEnabled(true);

		save3dcheckbox = new JCheckBox("3D");
		save3dcheckbox.setToolTipText("stereoscopic video");
		save3dcheckbox.setSelected(false);
		save3dcheckbox.setEnabled(true);

		radiopanel = new JPanel(new GridLayout(1,0));
		radiopanel.add(save3dcheckbox);
		radiopanel.add(mpgbutton);
		radiopanel.add(webmbutton);
		radiopanel.add(flvbutton);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 0;
		gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		this.panel.add( radiopanel, gbc );

		JLabel hint = new JLabel("Type, paste or drag'n drop a YouTube video address:");

		gbc.fill = 0;
		gbc.gridwidth = 0;
		gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.WEST;
		this.panel.add( hint, gbc );

		textinputfield = new JTextField( 20 );
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		textinputfield.setEnabled( true );
		textinputfield.setFocusable( true );
		textinputfield.addActionListener( this );
		textinputfield.getDocument().addDocumentListener(this);
		this.panel.add( textinputfield, gbc );

		this.quitbutton = new JButton( "" ,createImageIcon("images/exit.png",""));		
		gbc.gridx = 2;
		gbc.gridy = 5;
		gbc.gridwidth = 0;
		this.quitbutton.addActionListener( this );
		this.quitbutton.setActionCommand( "quit" );
		this.quitbutton.setToolTipText( "Exit." );

		this.panel.add( this.quitbutton, gbc );

		pane.add( this.panel );
		addWindowListener( this );

		this.setDropTarget(new DropTarget(this, this));
		textarea.setTransferHandler(null); // otherwise the dropped text would be inserted

	}

	public void windowActivated( WindowEvent e ) {
		textinputfield.requestFocusInWindow();
	}

	public void windowClosed( WindowEvent e ) {
	}

	/**
	 * quit==exit
	 * 
	 */
	public void windowClosing( WindowEvent e ) {
		this.shutdown();
	}

	public void windowDeactivated( WindowEvent e ) {
	}

	public void windowDeiconified( WindowEvent e ) {
	}

	public void windowIconified( WindowEvent e ) {
	}

	public void windowOpened( WindowEvent e ) {
	}

	@Override
	public void processComponentEvent(ComponentEvent e) {
		switch (e.getID()) {
		case ComponentEvent.COMPONENT_MOVED:
			break;
		case ComponentEvent.COMPONENT_RESIZED:
			middlepane.setDividerLocation(middlepane.getWidth() / 3);
			break;
		case ComponentEvent.COMPONENT_HIDDEN:
			break;
		case ComponentEvent.COMPONENT_SHOWN:
			break;
		}
	} // processComponentEvent

	public void changedUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	}


	public void insertUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	} 

	public void removeUpdate(DocumentEvent e) {
		checkInputFieldforYTURLs();
	}
	/**
	 * check if a youtube-URL was pasted or typed in
	 * if yes cut it out and send it to the URLList to get processed by one of the threads
	 * 
	 * the user can paste a long string containing many youtube-URLs .. but here is work to do because we have to erase the string(s) that remain(s)
	 */
	void checkInputFieldforYTURLs() {
		String sinput = textinputfield.getText(); // don't call .toLowerCase() !

		sinput = sinput.replaceAll("/watch?.*&v=", "/watch?v=");
		sinput = sinput.replaceAll(" ", "");
		sinput = sinput.replaceAll(szPLAYLISTREGEX, "/watch?v=");

		String surl = sinput.replaceFirst(URLUtils.szYTREGEX, "");

		// if nothing could be replaced we have to yt-URL found
		if (sinput.equals(surl)) {
			return;
		}

		// starting at index 0 because szYTREGEX should start with ^ // if szYTREGEX does not start with ^ then you have to find the index where the match is before you can cut out the URL 
		surl = sinput.substring(0, sinput.length()-surl.length());
		addYTURLToList(surl);
		sinput = sinput.substring(surl.length());

		// if remaining text is shorter than shortest possible yt-url we delete it
		if (sinput.length()<"youtube.com/watch?v=0123456789a".length()) {
			sinput = "";
		}

		//frame.textinputfield.setText(sinput); // generates a java.lang.IllegalStateException: Attempt to mutate in notification

		final String fs = sinput;

		// let a thread update the textfield in the UI
		Thread worker = new Thread() {
			@Override
			public void run() {
				synchronized (textinputfield) {
					textinputfield.setText(fs);
				}
			}
		};
		SwingUtilities.invokeLater (worker);
	} // checkInputFieldforYTURLS

	ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getClassLoader().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	} // createImageIcon

	public void stateChanged(ChangeEvent e) {
	}


	public void dragEnter(DropTargetDragEvent dtde) {
	}


	public void dragOver(DropTargetDragEvent dtde) {
	}


	public void dropActionChanged(DropTargetDragEvent dtde) {
	}


	public void dragExit(DropTargetEvent dte) {
	}


	/**
	 * processing event of dropping a HTTP URL, YT-Video Image or plain text (URL) onto the frame
	 * 
	 * seems not to work with M$-IE (8,9) - what a pity!
	 */
	public void drop(DropTargetDropEvent dtde) {
		Transferable tr = dtde.getTransferable();
		DataFlavor[] flavors = tr.getTransferDataFlavors();
		DataFlavor fl = null;
		String str = "";
		for (int i = 0; i < flavors.length; i++) {
			fl = flavors[i];
			if (fl.isFlavorTextType() /* || fl.isMimeTypeEqual("text/html") || fl.isMimeTypeEqual("application/x-java-url") || fl.isMimeTypeEqual("text/uri-list")*/) {
				try {
					dtde.acceptDrop(dtde.getDropAction());
				} catch (Throwable t) {
				}
				try {
					if (tr.getTransferData(fl) instanceof InputStreamReader) {
						BufferedReader textreader = new BufferedReader( (Reader) tr.getTransferData(fl));
						String sline = "";
						try {
							while (sline != null) {
								sline = textreader.readLine();
								if (sline != null) {
									str += sline;
								}
							}
						} catch (Exception e) {
						} finally {
							textreader.close();
						}
						str = str.replaceAll("<[^>]*>", ""); // remove HTML tags, especially a hrefs - ignore HTML characters like &szlig; (which are no tags)
					} else if (tr.getTransferData(fl) instanceof InputStream) {
						InputStream input = new BufferedInputStream((InputStream) tr.getTransferData(fl));
						int idata = 0;
						StringBuilder sresult = new StringBuilder();
						while ( (idata = input.read()) != -1) {
							if (idata != 0) {
								sresult.append( new Character((char) idata).toString() );
							}
						}
					} else {
						str = tr.getTransferData(fl).toString();
					}
				} catch (IOException ioe) {
				} catch (UnsupportedFlavorException ufe) {
				}

				// insert text into textfield - almost the same as user drops text/url into this field
				// except special characaters -> from http://de.wikipedia.org/wiki/GNU-Projekt („GNU is not Unix“)(&bdquo;GNU is not Unix&ldquo;)
				// two drops from same source .. one time in textfield and elsewhere - maybe we change that later?!
				if (str.matches(URLUtils.szYTREGEX.concat("(.*)"))) {
					synchronized (textinputfield) {
						textinputfield.setText(str.concat(textinputfield.getText()));
					}
					break;
				}
			}
		}

		dtde.dropComplete(true);
	} 

	private class DnlListener implements DownloaderListener{

		private String sOldURL = null;
		
		@Override
		public void downloadStarted(DownloadEvent e) {
			sOldURL = e.getStatus().toString().concat(" ").concat(e.getVideoUrl());
			GUIClient.this.exchangeYTURLInList(e.getVideoUrl(), sOldURL);
		}

		@Override
		public void downloadProgress(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat("(").concat(Long.toString(e.getCompletePerc()).concat(" %) ").concat(e.getVideoUrl()));
			GUIClient.this.exchangeYTURLInList(sOldURL, sNewURL);
			sOldURL = sNewURL;
		}

		@Override
		public void downloadCompleted(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat("(").concat(Long.toString(e.getCompletePerc()).concat(" %) ").concat(e.getVideoUrl()));
			GUIClient.this.exchangeYTURLInList(sOldURL, sNewURL);
			//GUIClient.this.removeURLFromList(GUIClient.szDLSTATE.concat(e.getVideoUrl()));
		}

		@Override
		public void downloadFailure(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat(e.getVideoUrl());
			GUIClient.this.exchangeYTURLInList(sOldURL, sNewURL);
			//GUIClient.this.removeURLFromList(GUIClient.szDLSTATE.concat(e.getVideoUrl()));
		}

		@Override
		public void downloadCompletedNotDownloaded(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat(sOldURL);
			GUIClient.this.exchangeYTURLInList(sOldURL, sNewURL);
			//GUIClient.this.removeURLFromList(GUIClient.szDLSTATE.concat(e.getVideoUrl()));
		}

		@Override
		public void threadIdle(DownloadEvent e) {
			
		}
		
	}
	
}