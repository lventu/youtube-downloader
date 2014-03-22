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
import it.ventuland.ytd.utils.UrlConverter;

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
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.configuration.Configuration;


public class GUIClient extends JFrame {

	private static final long serialVersionUID = 6791957129816930254L;

	private static final String newline = "\n";

	// more or less (internal) output
	// set to True or add 'd' after mod-time
	private boolean mIsDebug = false;

	// just report file size of HTTP header - don't download binary data (the video)
	private boolean mNoDowload = mIsDebug;

	// save diskspace - try to download e.g. 720p before 1080p if HD is set
	public static boolean bSaveDiskSpace = false;

	private static String sproxy = null;

	// RFC-1123 ? hostname [with protocol]	
	public static final String szPROXYREGEX = "(^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?([a-zA-Z0-9]+:[a-zA-Z0-9]+@)?([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*(:[0-90-90-90-9]{1,4})?$)|()";

	// RFC-1738 URL characters - not a regex for a real URL!!
	public static final String szURLREGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?[a-zA-Z0-9;/\\?@=&\\$\\-_\\.+!*\\'\\(\\),%]+$"; // without ":", plus "%"

	private static final String szPLAYLISTREGEX = "/view_play_list\\?p=([A-Za-z0-9]*)&playnext=[0-9]{1,2}&v=";

	// all characters that do not belong to an HTTP URL - could be written shorter?? (where did I use this?? dont now anymore)
	final String snotsourcecodeurl = "[^(a-z)^(A-Z)^(0-9)^%^&^=^\\.^:^/^\\?^_^-]";

	JPanel panel = null;
	JSplitPane middlepane = null;
	static JTextArea textarea = null;
	JList<String> urllist = null;
	JButton quitbutton = null;
	JButton directorybutton = null;
	JTextField directorytextfield = null;
	static JTextField textinputfield = null;
	
	ButtonGroup mVideoResolutionBtnGrp;
	ButtonGroup mVideoQualityBtnGrp;
	JCheckBox save3dcheckbox = null;
	
	JCheckBox saveconfigcheckbox = null;

	private DefaultListModel<String> dlm = null;
	private UrlList mUrlQueue = null;
	private DownloaderStub downloadExecutor = null;
	private Configuration mAppContext = null;
	private IConfiguration mSavedConfiguration = null;

	private String mSaveDirPath;

	public static synchronized String getProxy() {
		return sproxy;
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
		}catch (Exception e) {
		}
	}
/*
	private Boolean isButtonSelected(String pButtonName){
		
		Enumeration<AbstractButton> e = mVideoResolutionBtnGrp.getElements();
		boolean lbRet = false;
		while(e.hasMoreElements()){
			JRadioButton lRb = (JRadioButton) e;
			String lActionCommand = lRb.getActionCommand();
			if(pButtonName.equals(lActionCommand) && lRb.isSelected() ){
				lbRet = true;
				break;
			}
		}
		return Boolean.valueOf(lbRet);
	}
*/
	private String getResolutionSelected(){
		String lName = null;
		Object[] lSelection = mVideoResolutionBtnGrp.getSelection().getSelectedObjects();
		if(lSelection != null && lSelection.length>0){
			JRadioButton lRb =(JRadioButton) lSelection[0];
			lName = lRb.getName();
		}
		return lName;
	}
	
	private String getFormatSelected(){
		String lName = null;
		Object[] lSelection = mVideoQualityBtnGrp.getSelection().getSelectedObjects();
		if(lSelection != null && lSelection.length>0){
			JRadioButton lRb =(JRadioButton) lSelection[0];
			lName = lRb.getName();
		}
		return lName;
	}
	
	private void addYTURLToList( String sname ) {
		try {
			String sn = UrlConverter.getInstance().execute(sname);
			synchronized (dlm) {
				dlm.addElement( sn );
			}
			VideoElement lElem = new VideoElement();
			lElem.videoUrl = sn;
			lElem.videoQuality = getResolutionSelected();
			lElem.videoFormat = getFormatSelected();
			lElem.is3D = save3dcheckbox.isSelected();
			mUrlQueue.setElement( lElem );
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private void renameUrlInList( String sfromname, String stoname) {
		synchronized (dlm) {
			try {
				int i = dlm.indexOf( sfromname );
				dlm.setElementAt(stoname, i);
			} catch (Throwable t) {}
		}
	}

/*
	private void removeURLFromList( String sname ) {
		synchronized (dlm) {
			try {
				int i = dlm.indexOf( sname );
				dlm.remove( i );
			} catch (IndexOutOfBoundsException ioobe) {}
		}
	}

	private void clearURLList() {
		try {
			synchronized (dlm) {
				dlm.clear();
			}
		} catch (NullPointerException npe) {}
	}
*/

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

	public GUIClient(Configuration pConfig ) {
		initialize(pConfig);
	}

	protected void initialize(Configuration pConfig) {

		mSavedConfiguration = YtdConfigManager.getInstance();
		mAppContext = pConfig;
		
		mIsDebug = mAppContext.getBoolean("youtube-downloader.debug", false);
		
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
	
	private void shutdown() {
		if (saveconfigcheckbox.isSelected()) {
			mSavedConfiguration.saveConfiguration(mSaveDirPath, sproxy, getResolutionSelected(), getFormatSelected());
		}
		downloadExecutor.killAll();
		this.dispose();
	} 
	
	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event dispatch thread.
	 */
	private void initializeUI() {
		String lAppName = mAppContext.getString("youtube-downloader.name");
		String lAppVersion = mAppContext.getString("youtube-downloader.version");
		String lAppUrl = mAppContext.getString("youtube-downloader.url");
		String sv = lAppName.concat(" ").concat(lAppVersion).concat(" ").concat(lAppUrl);
		
		setDefaultLookAndFeelDecorated(false);
		
		this.setTitle(sv);
		this.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
		this.addComponentsToPane( this.getContentPane() );
		this.pack();
		this.setVisible( true );

		sv = "version: ".concat( lAppVersion ).concat(mIsDebug?" DEBUG ":"");

		sproxy = System.getenv("http_proxy");
		if (sproxy==null) {
			sproxy="";
		}
		mSaveDirPath = directorytextfield.getText();
	}

	private void initializeThreads(){
		// lets respect the upload limit of google (youtube)
		// downloading is faster than viewing anyway so don't start more than six threads and don't play around with the URL-strings please!!!
		mUrlQueue = new UrlList();
		downloadExecutor = new DownloaderStub(mUrlQueue,  new DnlListener(), mIsDebug, mNoDowload);
		downloadExecutor.startAll();
	}

	private void addComponentsToPane( final Container pane ) {
		this.panel = new JPanel();
		this.panel.setLayout( new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.anchor = GridBagConstraints.WEST;

		ActionManager lActionManager = new ActionManager();
		
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
		this.directorybutton.addActionListener( lActionManager );
		this.panel.add( this.directorybutton, gbc );

		this.saveconfigcheckbox = new JCheckBox("Save config");
		this.saveconfigcheckbox.setSelected(false);

		this.panel.add(this.saveconfigcheckbox);

		this.saveconfigcheckbox.setEnabled(false);

		// TODO check if initial download directory exists
		// assume that at least the users homedir exists
		String shomedir = System.getProperty("user.home").concat(File.separator);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.directorytextfield = new JTextField( shomedir, 20+(mIsDebug?48:0) );
		this.directorytextfield.setEnabled( false );
		this.directorytextfield.setFocusable( true );
		this.directorytextfield.addActionListener( lActionManager );
		this.panel.add( this.directorytextfield, gbc);

		JLabel dirhint = new JLabel("Download to folder:");

		gbc.gridx = 0;
		gbc.gridy = 1;
		this.panel.add( dirhint, gbc);

		this.middlepane.setPreferredSize( new Dimension( Toolkit.getDefaultToolkit().getScreenSize().width/3, Toolkit.getDefaultToolkit().getScreenSize().height/4+(mIsDebug?200:0) ) );

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 2;
		gbc.weightx = 2;
		gbc.gridwidth = 2;
		this.panel.add( this.middlepane, gbc );

		// radio buttons for resolution to download
		mVideoResolutionBtnGrp = new ButtonGroup();
		JPanel lRadioPanel = new JPanel(new GridLayout(1,0));
		List<Object> lVidQ = mAppContext.getList("youtube-downloader.video-quality");
		JRadioButton lRadioButton = null;
		for (Object obj : lVidQ) {
			String lQuality = (String) obj;
			String lToolTip = mAppContext.getString("youtube-downloader.video-quality."+lQuality+".tooltip");
			boolean lSelected = mAppContext.getBoolean("youtube-downloader.video-quality."+lQuality+".selected");
			boolean lEnabled = mAppContext.getBoolean("youtube-downloader.video-quality."+lQuality+".enabled");
			lRadioButton = new JRadioButton(lQuality);
			lRadioButton.setName(lQuality);
			lRadioButton.setActionCommand(lQuality.toLowerCase());
			lRadioButton.addActionListener(lActionManager); 
			lRadioButton.setToolTipText(lToolTip);
			lRadioButton.setSelected(lSelected);
			lRadioButton.setEnabled(lEnabled);
			mVideoResolutionBtnGrp.add(lRadioButton);
			lRadioPanel.add(lRadioButton);
		}

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 0;
		gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		this.panel.add( lRadioPanel, gbc );

		// radio buttons for video format to download
		mVideoQualityBtnGrp = new ButtonGroup();
		lRadioPanel = new JPanel(new GridLayout(1,0));
		save3dcheckbox = new JCheckBox("3D");
		save3dcheckbox.setToolTipText("stereoscopic video");
		save3dcheckbox.setSelected(false);
		save3dcheckbox.setEnabled(true);
		lRadioPanel.add(save3dcheckbox);
		List<Object> lVidR = mAppContext.getList("youtube-downloader.video-resolution");
		lRadioButton = null;
		for (Object obj : lVidR) {
			String lResolution = (String) obj;
			String lToolTip = mAppContext.getString("youtube-downloader.video-resolution."+lResolution+".tooltip");
			boolean lSelected = mAppContext.getBoolean("youtube-downloader.video-resolution."+lResolution+".selected");
			boolean lEnabled = mAppContext.getBoolean("youtube-downloader.video-resolution."+lResolution+".enabled");
			lRadioButton = new JRadioButton(lResolution);
			lRadioButton.setName(lResolution);
			lRadioButton.setActionCommand(lResolution.toLowerCase());
			lRadioButton.addActionListener(lActionManager); 
			lRadioButton.setToolTipText(lToolTip);
			lRadioButton.setSelected(lSelected);
			lRadioButton.setEnabled(lEnabled);
			mVideoQualityBtnGrp.add(lRadioButton);
			lRadioPanel.add(lRadioButton);
		}

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridheight = 0;
		gbc.gridwidth = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		this.panel.add( lRadioPanel, gbc );

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
		textinputfield.addActionListener( lActionManager );
		textinputfield.getDocument().addDocumentListener(new UrlInsertListener());
		this.panel.add( textinputfield, gbc );

		this.quitbutton = new JButton( "" ,createImageIcon("images/exit.png",""));		
		gbc.gridx = 2;
		gbc.gridy = 5;
		gbc.gridwidth = 0;
		this.quitbutton.addActionListener( lActionManager );
		this.quitbutton.setActionCommand( "quit" );
		this.quitbutton.setToolTipText( "Exit." );

		this.panel.add( this.quitbutton, gbc );

		pane.add( this.panel );
		addWindowListener( new GUIWindowAdapter() );

		this.setDropTarget(new DropTarget(this, new DragDropListener()));
		textarea.setTransferHandler(null); // otherwise the dropped text would be inserted

	}

	private ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getClassLoader().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
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
	}
	
	private class ActionManager implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
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
				if (fc.showOpenDialog(GUIClient.this) != JFileChooser.APPROVE_OPTION) {
					return;
				}
				String snewdirectory = fc.getSelectedFile().getAbsolutePath();
				// append file.seperator if last character is not file.seperator (the user choosed a directory other than root)
				snewdirectory.concat(snewdirectory.endsWith(System.getProperty("file.separator"))?"":System.getProperty("file.separator"));
				File ftest = new File(snewdirectory);
				if (ftest.exists()) {
					if (ftest.isDirectory()) {
						synchronized (directorytextfield) {
							GUIClient.this.mSaveDirPath = snewdirectory;
							directorytextfield.setText( snewdirectory );
						}
					} 
				}
				return;
			}
			
			// let the user choose another download resolution
			if ( e.getActionCommand().equals(mVideoResolutionBtnGrp.getSelection().getActionCommand()) ) {
				return;
			}
			
			// let the user choose another video format
			if (e.getActionCommand().equals(mVideoQualityBtnGrp.getSelection().getActionCommand()) ) {
				return;
			} 

			if (e.getActionCommand().equals( "quit" )) {
				addTextToConsole("quit requested - signaling donwload threads to terminate, this may take a while!");
				// seems to have to effect:
				//repaint();
				GUIClient.this.shutdown();
				return;
			}
		}
		
	}
	
	private class UrlInsertListener implements DocumentListener{

		@Override
		public void insertUpdate(DocumentEvent e) {
			checkInputFieldforYTURLs();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			checkInputFieldforYTURLs();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
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
		}
		
	}
	
	private class GUIWindowAdapter extends WindowAdapter{
		
		@Override
		public void windowActivated(WindowEvent e) {
			textinputfield.requestFocusInWindow();
		}
		
		@Override
		public void windowClosing(WindowEvent e) {
			GUIClient.this.shutdown();
		}
		
	}

	private class DragDropListener extends DropTargetAdapter{

		@Override
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
		
	}

	private class DnlListener implements DownloaderListener{

		private String sOldURL = null;
		
		@Override
		public void downloadStarted(DownloadEvent e) {
			sOldURL = e.getStatus().toString().concat(" ").concat(e.getVideoUrl());
			GUIClient.this.renameUrlInList(e.getVideoUrl(), sOldURL);
		}

		@Override
		public void downloadProgress(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat("(").concat(Long.toString(e.getCompletePerc()).concat(" %) ").concat(e.getVideoUrl()));
			GUIClient.this.renameUrlInList(sOldURL, sNewURL);
			sOldURL = sNewURL;
		}

		@Override
		public void downloadCompleted(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat("(").concat(Long.toString(e.getCompletePerc()).concat(" %) ").concat(e.getVideoUrl()));
			GUIClient.this.renameUrlInList(sOldURL, sNewURL);
			//GUIClient.this.removeURLFromList(GUIClient.szDLSTATE.concat(e.getVideoUrl()));
		}

		@Override
		public void downloadFailure(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat(e.getVideoUrl());
			GUIClient.this.renameUrlInList(sOldURL, sNewURL);
			//GUIClient.this.removeURLFromList(GUIClient.szDLSTATE.concat(e.getVideoUrl()));
		}

		@Override
		public void downloadCompletedNotDownloaded(DownloadEvent e) {
			String sNewURL = e.getStatus().toString().concat(sOldURL);
			GUIClient.this.renameUrlInList(sOldURL, sNewURL);
			//GUIClient.this.removeURLFromList(GUIClient.szDLSTATE.concat(e.getVideoUrl()));
		}

		@Override
		public void threadIdle(DownloadEvent e) {
			
		}

		@Override
		public void threadAborted(DownloadEvent e) {
			
		}
		
	}
	
}