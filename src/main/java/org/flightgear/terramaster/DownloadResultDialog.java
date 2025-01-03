package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.SystemColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.flightgear.terramaster.dns.WeightedUrl;

public class DownloadResultDialog extends JDialog {
  private static Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);

  private final JPanel contentPanel = new JPanel();
  private ArrayList<Exception> exceptions = new ArrayList<>();

  /**
   * Create the dialog.
   * 
   * @param completeStats
   */
  public DownloadResultDialog(Map<WeightedUrl, TileResult> completeStats) {
    setAlwaysOnTop(true);
    setBounds(100, 100, 633, 381);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new BorderLayout(0, 0));
    JEditorPane jEditorPane = new JEditorPane();
    jEditorPane.setBackground(SystemColor.control);
    jEditorPane.setEditable(false);
    jEditorPane.addHyperlinkListener(e -> {
      if (e.getEventType() == EventType.ACTIVATED) {
        new ExceptionDialog(exceptions.get(Integer.parseInt(e.getURL().getHost()))).setVisible(true);
      }
    });
    HTMLEditorKit kit = new HTMLEditorKit();
    cssStyling(kit);
    jEditorPane.setEditorKit(kit);
    Document doc = kit.createDefaultDocument();
    jEditorPane.setDocument(doc);
    jEditorPane.setText(getHTML(completeStats));
    jEditorPane.setCaretPosition(0);
    JScrollPane scrollPane = new JScrollPane(jEditorPane);
    contentPanel.add(scrollPane, BorderLayout.CENTER);
    JPanel buttonPane = new JPanel();
    getContentPane().add(buttonPane, BorderLayout.SOUTH);
    buttonPane.setLayout(new BorderLayout(0, 0));
    JPanel panel = new JPanel();
    buttonPane.add(panel, BorderLayout.NORTH);
    JButton okButton = new JButton("OK");
    okButton.addActionListener(ae -> setVisible(false));

    panel.add(okButton);
    getRootPane().setDefaultButton(okButton);
  }

  public void cssStyling(HTMLEditorKit kit) {
    // add some styles to the html
    StyleSheet styleSheet = kit.getStyleSheet();
    styleSheet.addRule(String.format("h1 {color:#000; font-family:%s; margin: 2px; }", Font.DIALOG));
    styleSheet.addRule(String.format("h2 {font : %dpx %s}", 10, Font.DIALOG));
    styleSheet.addRule(String.format("h3 {font : bold %dpx %s }", 12, Font.DIALOG));
    styleSheet.addRule(String.format("body {font : %dpx %s}", 10, Font.DIALOG));
    styleSheet.addRule(String.format("pre {font : %dpx %s }", 10, Font.DIALOG));
  }

  private String getHTML(Map<WeightedUrl, TileResult> completeStats) {

    StringBuilder sb = new StringBuilder();
    sb.append("<HTML>");
    for (Entry<WeightedUrl, TileResult> entry : completeStats.entrySet()) {
      sb.append("<H3>" + entry.getKey().getUrl().toExternalForm() + "</H3>");
      sb.append("404s " + entry.getValue().notFound + " Downloads " + entry.getValue().actualDownloads + " Equal "
          + entry.getValue().equal + " Errors " + entry.getValue().errors  + "<BR>");
      sb.append(String.format("Dowloaded %s in %4.2f seconds<BR>", getBytes(entry.getValue().numberBytes),
          (double) entry.getValue().time / 1000));
      if (entry.getValue().time > 1000) {
        double seconds = (double) entry.getValue().time / 1000;
        sb.append(String.format("Dowload Speed %s<BR>", getSpeedBytes(entry.getValue().numberBytes, seconds)));
      }
      if (entry.getValue().getException() != null) {
        sb.append(String.format("<A href=\"http://%d\">%s</A>", exceptions.size(),
            entry.getValue().getException().toString()));
        exceptions.add(entry.getValue().getException());
      }
    }
    sb.append("</HTML>");
    return sb.toString();
  }

  private String getBytes(long numberBytes) {
    if (numberBytes > 1024 * 1024)
      return String.format("%4.2f MB", (float) numberBytes / (1024 * 1024));
    if (numberBytes > 1024)
      return String.format("%4.2f kB", (float) numberBytes / 1024);
    return String.format("%d B", numberBytes);
  }

  private String getSpeedBytes(long numberBytes, double seconds) {
    if (numberBytes > 1024 * 1024)
      return String.format("%4.2f MB/s", ((double) numberBytes / seconds) / (1024 * 1024));
    if (numberBytes > 1024)
      return String.format("%4.2f kB/s<BR>", ((double) numberBytes / seconds) / 1024);
    return String.format("%4.2f B/s<BR>", ((double) numberBytes / seconds));
  }
}
