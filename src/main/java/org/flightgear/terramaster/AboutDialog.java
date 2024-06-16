package org.flightgear.terramaster;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class AboutDialog extends JDialog {
  private static final String TEXT_HTML = "text/html";
  static Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  private JLabel lblV;

  private final class HyperLinkListener implements HyperlinkListener {
    public void hyperlinkUpdate(HyperlinkEvent hle) {
      if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
        log.fine(() -> "Calling {}" + hle.getURL().toExternalForm());
        try {
          Desktop.getDesktop().browse(new URI(hle.getURL().toExternalForm()));
        } catch (IOException | URISyntaxException e) {
          log.log(Level.WARNING, e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Create the application.
   */
  public AboutDialog() {
    setIconImage(Toolkit.getDefaultToolkit().getImage(AboutDialog.class.getResource("/TerraMaster logo cropped.ico")));
    setAlwaysOnTop(true);
    setResizable(false);
    initialize();
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    getContentPane().setFont(new Font("Tahoma", Font.PLAIN, 16));
    setBounds(100, 100, 524, 404);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    GridBagLayout gridBagLayout = new GridBagLayout();
    gridBagLayout.columnWidths = new int[] { 149, 120 };
    gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 37, 0 };
    gridBagLayout.columnWeights = new double[] { 1.0, 0.0 };
    gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
    getContentPane().setLayout(gridBagLayout);

    JButton btnNewButton = new JButton("Ok");
    btnNewButton.addActionListener(ev -> setVisible(false));
    
    JEditorPane dtrpnreed = new JEditorPane(TEXT_HTML, "<a href='http://wiki.flightgear.org/TerraMaster'>Help</a>");
    dtrpnreed.setForeground(SystemColor.textHighlight);
    dtrpnreed.setBackground(SystemColor.control);
    dtrpnreed.setForeground(new Color(0, 0, 153));
    dtrpnreed.setBackground(new Color(192, 192, 192));
    dtrpnreed.setOpaque(false);
    dtrpnreed.setEditable(false);
    dtrpnreed.addHyperlinkListener(new HyperLinkListener());
    
    GridBagConstraints gbc_dtrpnreed = new GridBagConstraints();
    gbc_dtrpnreed.insets = new Insets(0, 0, 5, 5);
    gbc_dtrpnreed.fill = GridBagConstraints.VERTICAL;
    gbc_dtrpnreed.gridx = 1;
    gbc_dtrpnreed.gridy = 1;
    getContentPane().add(dtrpnreed, gbc_dtrpnreed);

    JLabel label = new JLabel("Developed by :");
    GridBagConstraints gbc_label = new GridBagConstraints();
    gbc_label.insets = new Insets(0, 0, 5, 5);
    gbc_label.gridx = 0;
    gbc_label.gridy = 2;
    getContentPane().add(label, gbc_label);

    JEditorPane editorPane = new JEditorPane(TEXT_HTML, "<a href='https://github.com/open744'>reed</a>");
    GridBagConstraints gbc_editorPane = new GridBagConstraints();
    gbc_editorPane.insets = new Insets(0, 0, 5, 5);
    gbc_editorPane.gridx = 1;
    gbc_editorPane.gridy = 2;
    getContentPane().add(editorPane, gbc_editorPane);
    editorPane.setToolTipText("Code");
    editorPane.setOpaque(false);
    editorPane.setForeground(new Color(0, 0, 153));
    editorPane.setEditable(false);
    editorPane.setBackground(new Color(192, 192, 192));
    editorPane.addHyperlinkListener(new HyperLinkListener());

    JEditorPane editorPane_1 = new JEditorPane(TEXT_HTML, "<a href='https://github.com/Portree-Kid'>portree_kid</a>");
    GridBagConstraints gbc_editorPane_1 = new GridBagConstraints();
    gbc_editorPane_1.insets = new Insets(0, 10, 5, 10);
    gbc_editorPane_1.gridx = 2;
    gbc_editorPane_1.gridy = 2;
    getContentPane().add(editorPane_1, gbc_editorPane_1);
    editorPane_1.setToolTipText("Code");
    editorPane_1.setOpaque(false);
    editorPane_1.setEditable(false);
    editorPane_1.setBackground(Color.LIGHT_GRAY);
    editorPane_1.addHyperlinkListener(new HyperLinkListener());

    JEditorPane dtrpnclive = new JEditorPane(TEXT_HTML,
        "<a href='https://forum.flightgear.org/memberlist.php?mode=viewprofile&u=19112'>Clive2670</a>");
    GridBagConstraints gbc_dtrpnclive = new GridBagConstraints();
    gbc_dtrpnclive.insets = new Insets(0, 10, 5, 10);
    gbc_dtrpnclive.gridx = 3;
    gbc_dtrpnclive.gridy = 2;
    getContentPane().add(dtrpnclive, gbc_dtrpnclive);
    dtrpnclive.setToolTipText("Logo");
    dtrpnclive.setOpaque(false);
    dtrpnclive.setEditable(false);
    dtrpnclive.setBackground(Color.LIGHT_GRAY);
    dtrpnclive.addHyperlinkListener(new HyperLinkListener());

    JLabel lblLicense = new JLabel("License : ");
    GridBagConstraints gbc_lblLicense = new GridBagConstraints();
    gbc_lblLicense.insets = new Insets(0, 0, 5, 5);
    gbc_lblLicense.gridx = 0;
    gbc_lblLicense.gridy = 3;
    getContentPane().add(lblLicense, gbc_lblLicense);

    JEditorPane btnGpl = new JEditorPane(TEXT_HTML,
        "<a href='https://github.com/Portree-Kid/terramaster/blob/master/COPYING'>GPL 2.0</a>");
    GridBagConstraints gbc_btnGpl = new GridBagConstraints();
    gbc_btnGpl.insets = new Insets(0, 0, 5, 5);
    gbc_btnGpl.gridx = 1;
    gbc_btnGpl.gridy = 3;
    getContentPane().add(btnGpl, gbc_btnGpl);
    btnGpl.setFont(new Font("Arial", Font.PLAIN, 13));
    btnGpl.setBackground(Color.LIGHT_GRAY);
    btnGpl.setEditable(false);
    btnGpl.setOpaque(false);
    btnGpl.addHyperlinkListener(new HyperLinkListener());

    JEditorPane btnSource = new JEditorPane(TEXT_HTML,
        "<a href='https://github.com/Portree-Kid/terramaster'>Source</a>");
    GridBagConstraints gbc_btnSource = new GridBagConstraints();
    gbc_btnSource.insets = new Insets(0, 0, 5, 5);
    gbc_btnSource.gridx = 2;
    gbc_btnSource.gridy = 3;
    getContentPane().add(btnSource, gbc_btnSource);
    btnSource.setFont(new Font("Arial", Font.PLAIN, 13));
    btnSource.setBackground(Color.LIGHT_GRAY);
    btnSource.setEditable(false);
    btnSource.setOpaque(false);
    btnSource.addHyperlinkListener(new HyperLinkListener());

    JLabel lblNewLabel = new JLabel("");
    GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
    gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
    gbc_lblNewLabel.gridwidth = 4;
    gbc_lblNewLabel.anchor = GridBagConstraints.NORTH;
    gbc_lblNewLabel.gridx = 0;
    gbc_lblNewLabel.gridy = 0;
    getContentPane().add(lblNewLabel, gbc_lblNewLabel);
    lblNewLabel.setIcon(new ImageIcon(AboutDialog.class.getResource("/TerraMaster logo 2.png")));

    JLabel lblVersion = new JLabel("Version : ");
    GridBagConstraints gbc_lblVersion = new GridBagConstraints();
    gbc_lblVersion.insets = new Insets(0, 0, 5, 5);
    gbc_lblVersion.gridx = 0;
    gbc_lblVersion.gridy = 4;
    getContentPane().add(lblVersion, gbc_lblVersion);

    lblV = new JLabel("V");
    GridBagConstraints gbc_lblV = new GridBagConstraints();
    gbc_lblV.insets = new Insets(0, 0, 5, 5);
    gbc_lblV.gridx = 1;
    gbc_lblV.gridy = 4;
    getContentPane().add(lblV, gbc_lblV);
    GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
    gbc_btnNewButton.gridwidth = 4;
    gbc_btnNewButton.gridx = 0;
    gbc_btnNewButton.gridy = 5;
    getContentPane().add(btnNewButton, gbc_btnNewButton);
    loadVersion();
  }

  private void loadVersion() {
    try {

      Properties props = new Properties();
      props.put("version","-.-.-");
      try (InputStream is = getClass()
          .getResourceAsStream("/META-INF/maven/org.flightgear/terramaster/pom.properties")) {
        props.load(is);
      }
      // build.minor.number=10
      // build.major.number=1
      String v = props.getProperty("version");
      lblV.setText(v);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
