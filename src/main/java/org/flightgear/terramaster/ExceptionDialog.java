package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.SystemColor;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import javax.swing.JScrollPane;

public class ExceptionDialog extends JDialog {

  /**
   * Create the dialog.
   */
  public ExceptionDialog(Exception ex) {
    setAlwaysOnTop(true);
    setBounds(100, 100, 885, 528);
    getContentPane().setLayout(new BorderLayout());
    JPanel contentPanel = new JPanel();
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new BorderLayout(0, 0));
    {
      JEditorPane jEditorPane = new JEditorPane();
      jEditorPane.setBackground(SystemColor.control);
      jEditorPane.setEditable(false);
      HTMLEditorKit kit = new HTMLEditorKit();
      jEditorPane.setEditorKit(kit);
      Document doc = kit.createDefaultDocument();
      jEditorPane.setDocument(doc);
      jEditorPane.setText(getHTML(ex));
      jEditorPane.setCaretPosition(0);
      JScrollPane scrollPane = new JScrollPane(jEditorPane);
      contentPanel.add(scrollPane, BorderLayout.CENTER);

    }
    {
      JPanel buttonPane = new JPanel();
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      buttonPane.setLayout(new BorderLayout(0, 0));
      {
        JPanel panel = new JPanel();
        buttonPane.add(panel, BorderLayout.NORTH);
        {
          JButton okButton = new JButton("OK");
          okButton.addActionListener(ae -> setVisible(false));

          panel.add(okButton);
          getRootPane().setDefaultButton(okButton);
        }
      }
    }
  }

  private String getHTML(Exception ignoredEx) {
    return "<HTML>HTML Exception Formatter not supported anymore</HTML>";
  }

}
