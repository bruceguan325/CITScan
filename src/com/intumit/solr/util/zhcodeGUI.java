package com.intumit.solr.util;

import java.lang.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.*;

class zhcodeGUI extends JPanel {
	private JFileChooser filechooser;
	// JPanel srcpanel, outpanel, optionpanel;
	private JComboBox srcencoding, outencoding;
	private JButton srcbutton, outbutton, convertbutton, optionbutton;
	private JTextField srcfilefield, outfilefield;
	private String srcfilename, outfilename;
	private JFrame topframe;
	private Component temp;
	private File srcfile, outfile;
	private zhcode zhcoder;
	private SinoDetect sinodetector;

	public zhcodeGUI() {
		zhcoder = new zhcode();
		sinodetector = new SinoDetect();
		initGUI();
	}

	public void initGUI() {
		int i;
		GridBagConstraints c = new GridBagConstraints();

		filechooser = new JFileChooser();
		filechooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		temp = this;

		this.setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createEmptyBorder(25, 15, 25, 15));

		// srcpanel = new JPanel(new GridBagLayout());

		c.insets = new Insets(5, 5, 5, 5);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = c.weighty = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		this.add(new JLabel("Source File Name: "), c);

		srcfilefield = new JTextField(20);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(srcfilefield, c);

		srcbutton = new JButton("Choose File");
		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = c.weighty = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		this.add(srcbutton, c);

		srcencoding = new JComboBox();
		for (i = 0; i < zhcode.TOTALTYPES - 2; i++) {
			srcencoding.addItem(zhcoder.nicename[i]);
		}

		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = c.weighty = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;

		this.add(srcencoding, c);

		// outpanel = new JPanel(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = c.weighty = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		this.add(new JLabel("Target File Name: "), c);

		outfilefield = new JTextField(20);
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(outfilefield, c);

		outbutton = new JButton("Choose File");
		c.gridx = 2;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = c.weighty = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		this.add(outbutton, c);

		outencoding = new JComboBox();
		for (i = 0; i < zhcode.TOTALTYPES - 2; i++) {
			outencoding.addItem(zhcoder.nicename[i]);
		}
		c.gridx = 3;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = c.weighty = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		this.add(outencoding, c);

		// optionpanel = new JPanel();

		convertbutton = new JButton("Convert File");
		ActionListener convertbuttonlistener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				srcfilename = srcfilefield.getText();
				outfilename = outfilefield.getText();
				srcfile = new File(srcfilename);
				outfile = new File(outfilename);

				if (srcfile != null && srcfile.exists() == true
						&& outfile != null
						&& outfile.getParentFile().exists() == true) {
					if ((srcfile.isDirectory() == true && outfile.isDirectory() == false)
							|| (srcfile.isDirectory() == false && outfile
									.isDirectory() == true)) {
						JOptionPane.showMessageDialog(null,
								"Both must be directories.");
						return;
					}

					if (srcfile.isDirectory() == true
							&& outfile.isDirectory() == true) {
						zhcoder.autodetect = true;
					} else if (srcfile.isFile() == true
							&& outfile.isDirectory() == true) {
						zhcoder.autodetect = false;
					}

					zhcoder.convertFile(srcfile.getAbsolutePath(), outfile
							.getAbsolutePath(), srcencoding.getSelectedIndex(),
							outencoding.getSelectedIndex());
					if (outfile.exists() == true) {
						JOptionPane.showMessageDialog(null,
								"File successfully converted.");
					} else {
						JOptionPane.showMessageDialog(null,
								"File conversion failed.");
					}
					return;
				}
				if (srcfile == null) {
					JOptionPane.showMessageDialog(null,
							"Please specify a source file.");
				} else if (srcfile.exists() == false) {
					JOptionPane.showMessageDialog(null,
							"Source file doesn't exist.");
				}
				if (outfile == null) {
					JOptionPane.showMessageDialog(null,
							"Please specify a target file.");
				} else if (outfile.getParentFile().exists() == false) {
					JOptionPane.showMessageDialog(null,
							"Parent directory of target file:\n\""
									+ outfile.getParentFile().getAbsolutePath()
									+ "\"\ndoes not exist.");
				}
			}
		};
		convertbutton.addActionListener(convertbuttonlistener);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 4;
		c.gridheight = 1;
		c.weightx = c.weighty = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;

		JPanel buttonPanel = new JPanel();
		this.add(buttonPanel, c);
		buttonPanel.add(convertbutton);

		optionbutton = new JButton("Options");
		buttonPanel.add(optionbutton);
		ActionListener optionsbuttonlistener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// showOptionDialog();
				int currentStrategy = zhcoder.getUnsupportedStrategy();
				Object result = JOptionPane
						.showInputDialog(
								null,
								"<HTML>Some character sets contain glyphs that cannot<br>"
										+ "be expressed in other character sets.  If this<br>"
										+ "situation occurs, you can choose what to do with<br>"
										+ "the unsupported glyphs.  This is separate from trad/simp<br>"
										+ "character conversion.",
								"Encoding Converter Options",
								JOptionPane.QUESTION_MESSAGE, null,
								zhcode.strategyDescriptions,
								zhcode.strategyDescriptions[currentStrategy]);

				if (result == null) {
					return;
				}
				for (int i = 0; i < zhcode.TOTAL; i++) {
					if (zhcode.strategyDescriptions[i].equals(result)) {
						zhcoder.setUnsupportedStrategy(i);
					}
				}

			}
		};
		optionbutton.addActionListener(optionsbuttonlistener);

		// this.add(srcpanel);
		// this.add(outpanel);
		// this.add(optionpanel);

		ActionListener filebuttonlistener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal;
				if (srcbutton == (JButton) e.getSource()) {
					returnVal = filechooser.showOpenDialog(temp);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						srcfilefield.setText(filechooser.getSelectedFile()
								.getAbsolutePath());
						File srcfile = new File(srcfilefield.getText());
						if (srcfile != null && srcfile.exists() == true
								&& srcfile.isFile() == true) {
							int codeguess = sinodetector
									.detectEncoding(srcfile);
							if (codeguess != Encoding.ASCII
									&& codeguess != Encoding.OTHER) {
								srcencoding.setSelectedIndex(codeguess);
							}
						}

						// srcfile = filechooser.getSelectedFile();
						// srcfilename = srcfile.getAbsolutePath();
						// srcfilefield.setText(srcfilename;
					}
				} else if (outbutton == (JButton) e.getSource()) {
					returnVal = filechooser.showSaveDialog(temp);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						outfilefield.setText(filechooser.getSelectedFile()
								.getAbsolutePath());
						// outfile = filechooser.getSelectedFile();
						// outfilename = outfile.getAbsolutePath();
						// outfilefield.setText(outfilename);
					}
				}
			}
		};
		srcbutton.addActionListener(filebuttonlistener);
		outbutton.addActionListener(filebuttonlistener);

	}

	private void showOptionDialog() {
		final JDialog prefDialog = new JDialog((JFrame) null,
				"Encoding Converter Preferences");
		prefDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,
				0, false);
		Action escapeAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				prefDialog.dispose();
			}
		};

		prefDialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(escapeKeyStroke, "ESCAPE");
		prefDialog.getRootPane().getActionMap().put("ESCAPE", escapeAction);

		JLabel explanation = new JLabel(
				"<HTML>Some character sets contain glyphs that cannot be expressed in other character sets.  If this situation occurs, you can choose what to do with the unsupported glyphs.");

		final JButton okButton, cancelButton, applyButton;
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");

		ActionListener prefActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == cancelButton) {
					prefDialog.dispose();
					return;
				}

				prefDialog.dispose();
			}
		};

	}

	public static void main(String argc[]) {
		JFrame zhframe = new JFrame("Chinese Encoding Converter");
		zhcodeGUI mygui = new zhcodeGUI();
		URL iconurl;
		Image zi_icon;

		// Associate an icon with this frame
		String imageFileName = "zi.gif";
		InputStream gifStream = mygui.getClass().getResourceAsStream(
				imageFileName);
		Toolkit tk = Toolkit.getDefaultToolkit();
		Image img = null;
		try {
			byte imageBytes[] = new byte[gifStream.available()];
			gifStream.read(imageBytes);
			img = tk.createImage(imageBytes);
			zhframe.setIconImage(img);
		} catch (Exception ec) {
			System.err.println("Load image url exception " + ec);
		}

		zhframe.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		zhframe.getContentPane().add(mygui);
		zhframe.pack();
		zhframe.setSize(650, 180);
		zhframe.show();
	}

}
