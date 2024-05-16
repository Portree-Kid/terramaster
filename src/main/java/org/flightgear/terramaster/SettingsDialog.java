package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.FlightgearNAPTRQuery.HealthStats;

/**
 * Settings dialog of the GUI
 *
 * @author keith.paterson
 * @author Simon
 */

public class SettingsDialog extends JDialog {

  transient Logger log = Logger.getLogger(getClass().getName());
  private final JTextField txtScenerypath;
  private final JList<String> cmbSceneryVersion;
  private final Vector<String> versions;
  private final ArrayList<Level> levels = new ArrayList<>();
  private final JComboBox<Level> cmbLogLevel;
  private Logger root;
  private final JTextField tileage;
  private final JCheckBox checkBoxGoogle;
  private final JCheckBox checkBoxGCA;
  private final TerraMaster terraMaster;

  {
    levels.add(Level.ALL);
    levels.add(Level.FINEST);
    levels.add(Level.FINER);
    levels.add(Level.INFO);
    levels.add(Level.WARNING);
    levels.add(Level.SEVERE);
  }

  /**
   * Create the dialog.
   */
  public SettingsDialog(TerraMaster terraMaster) {
    this.terraMaster = terraMaster;
    setTitle("Settings");
    setModal(true);
    setBounds(100, 100, 466, 327);
    getContentPane().setLayout(new BorderLayout());
    JPanel contentPanel = new JPanel();
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    GridBagLayout gbl_contentPanel = new GridBagLayout();
    gbl_contentPanel.columnWidths = new int[] { 0, 0, 40, 0 };
    gbl_contentPanel.rowHeights = new int[] { 0, 0, 22, 0, 0, 0 };
    gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 1.0, Double.MIN_VALUE };
    gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
    contentPanel.setLayout(gbl_contentPanel);
    {
      {
        JLabel lblSceneryPath = new JLabel("Scenery Path :");
        GridBagConstraints gbc_lblSceneryPath = new GridBagConstraints();
        gbc_lblSceneryPath.insets = new Insets(0, 0, 5, 5);
        gbc_lblSceneryPath.anchor = GridBagConstraints.EAST;
        gbc_lblSceneryPath.gridx = 0;
        gbc_lblSceneryPath.gridy = 0;
        contentPanel.add(lblSceneryPath, gbc_lblSceneryPath);
      }
      {
        txtScenerypath = new JTextField();
        GridBagConstraints gbc_txtScenerypath = new GridBagConstraints();
        gbc_txtScenerypath.weightx = 4.0;
        gbc_txtScenerypath.insets = new Insets(0, 0, 5, 5);
        gbc_txtScenerypath.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtScenerypath.gridx = 1;
        gbc_txtScenerypath.gridy = 0;
        contentPanel.add(txtScenerypath, gbc_txtScenerypath);
        txtScenerypath.setColumns(10);
      }
      txtScenerypath.setText((String) terraMaster.getProps().get(TerraMasterProperties.SCENERY_PATH));
    }
    final JButton selectDirectoryButton = new JButton("...");
    selectDirectoryButton.addActionListener(e -> {
      JFileChooser fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      fc.setCurrentDirectory(new File(txtScenerypath.getText()));

      if (fc.showOpenDialog(selectDirectoryButton) == JFileChooser.APPROVE_OPTION) {
        File f = fc.getSelectedFile();
        fc.setCurrentDirectory(f);
        txtScenerypath.setText(f.getAbsolutePath());
      }

    });
    GridBagConstraints gbc_selectDirectoryButton = new GridBagConstraints();
    gbc_selectDirectoryButton.insets = new Insets(0, 0, 5, 0);
    gbc_selectDirectoryButton.gridx = 2;
    gbc_selectDirectoryButton.gridy = 0;
    contentPanel.add(selectDirectoryButton, gbc_selectDirectoryButton);
    {
      JLabel lblSceneryVersion = new JLabel("Scenery Version :");
      GridBagConstraints gbc_lblScenerySource = new GridBagConstraints();
      gbc_lblScenerySource.anchor = GridBagConstraints.EAST;
      gbc_lblScenerySource.insets = new Insets(0, 0, 5, 5);
      gbc_lblScenerySource.gridx = 0;
      gbc_lblScenerySource.gridy = 1;
      contentPanel.add(lblSceneryVersion, gbc_lblScenerySource);
    }
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
          setVisible(false);
          saveValues();

        });
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
      {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
      }
    }
    {
      versions = new Vector<>();
      cmbSceneryVersion = new JList<>();
      cmbSceneryVersion.setSelectedValue(terraMaster.getProps().getProperty(TerraMasterProperties.SERVER_TYPE), false);
      GridBagConstraints gbc_cmbSceneryVersion = new GridBagConstraints();
      gbc_cmbSceneryVersion.insets = new Insets(0, 0, 5, 5);
      gbc_cmbSceneryVersion.fill = GridBagConstraints.HORIZONTAL;
      gbc_cmbSceneryVersion.gridx = 1;
      gbc_cmbSceneryVersion.gridy = 1;
      contentPanel.add(cmbSceneryVersion, gbc_cmbSceneryVersion);
      new Thread(() -> {
        FlightgearNAPTRQuery query = new FlightgearNAPTRQuery(terraMaster);
        query.queryDNSServer("Any String");

        for (String version : query.getTypes()) {
          if (!versions.contains(version)) {
            versions.add(version);
          }
        }
        for (Entry<String, HealthStats> entry : query.getStats().entrySet()) {
          log.fine(entry.getValue().toString());
        }
        cmbSceneryVersion.setListData(versions);
        restoreValues();
      }).start();
    }
    {
      JLabel lblNewLabel = new JLabel("Sync");
      GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
      gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
      gbc_lblNewLabel.gridx = 0;
      gbc_lblNewLabel.gridy = 2;
      contentPanel.add(lblNewLabel, gbc_lblNewLabel);
    }
    {
      JPanel panel = new JPanel();
      GridBagConstraints gbc_panel = new GridBagConstraints();
      gbc_panel.insets = new Insets(0, 0, 5, 5);
      gbc_panel.fill = GridBagConstraints.BOTH;
      gbc_panel.gridx = 1;
      gbc_panel.gridy = 2;
      contentPanel.add(panel, gbc_panel);
      panel.setLayout(new GridLayout(0, 3, 0, 0));
      for (TerraSyncDirectoryType type : TerraSyncDirectoryType.values()) {
        JCheckBox chckbxTsdt = new JCheckBox(type.name());
        panel.add(chckbxTsdt);
        chckbxTsdt.setSelected(
                Boolean.parseBoolean(terraMaster.getProps().getProperty(type.name(), "false")));
        chckbxTsdt.addActionListener(e -> terraMaster.getProps().setProperty(type.name(),
                Boolean.toString(chckbxTsdt.isSelected())));
      }
    }
    {
      JLabel lblLogLevel = new JLabel("Log Level");
      GridBagConstraints gbc_lblLogLevel = new GridBagConstraints();
      gbc_lblLogLevel.anchor = GridBagConstraints.EAST;
      gbc_lblLogLevel.insets = new Insets(0, 0, 5, 5);
      gbc_lblLogLevel.gridx = 0;
      gbc_lblLogLevel.gridy = 3;
      contentPanel.add(lblLogLevel, gbc_lblLogLevel);
    }
    {
      cmbLogLevel = new JComboBox<>();
      cmbLogLevel.addPropertyChangeListener(evt -> {
        if (root != null)
          root.setLevel(cmbLogLevel.getItemAt(cmbLogLevel.getSelectedIndex()));
      });
      cmbLogLevel.addActionListener(e -> {
        Level newLevell = cmbLogLevel.getItemAt(cmbLogLevel.getSelectedIndex());
        root.setLevel(newLevell);
        LogManager manager = LogManager.getLogManager();
        Enumeration<String> loggers = manager.getLoggerNames();
        while (loggers.hasMoreElements()) {
          String logger = loggers.nextElement();
          Logger logger2 = manager.getLogger(logger);
          if (logger2 != null && logger2.getLevel() != null) {
            logger2.setLevel(newLevell);
          }
        }
      });
      cmbLogLevel.setModel(new DefaultComboBoxModel<>(levels.toArray(new Level[0])));
      GridBagConstraints gbc_cmbLogLevel = new GridBagConstraints();
      gbc_cmbLogLevel.insets = new Insets(0, 0, 5, 5);
      gbc_cmbLogLevel.fill = GridBagConstraints.HORIZONTAL;
      gbc_cmbLogLevel.gridx = 1;
      gbc_cmbLogLevel.gridy = 3;
      contentPanel.add(cmbLogLevel, gbc_cmbLogLevel);
    }
    {
      JLabel lblMaxTileAge = new JLabel("max tile age");
      GridBagConstraints gbc_lblMaxTileAge = new GridBagConstraints();
      gbc_lblMaxTileAge.insets = new Insets(0, 0, 5, 5);
      gbc_lblMaxTileAge.anchor = GridBagConstraints.EAST;
      gbc_lblMaxTileAge.gridx = 0;
      gbc_lblMaxTileAge.gridy = 4;
      contentPanel.add(lblMaxTileAge, gbc_lblMaxTileAge);
    }
    {
      tileage = new JTextField();
      GridBagConstraints gbc_tileage = new GridBagConstraints();
      gbc_tileage.insets = new Insets(0, 0, 5, 5);
      gbc_tileage.fill = GridBagConstraints.HORIZONTAL;
      gbc_tileage.gridx = 1;
      gbc_tileage.gridy = 4;
      contentPanel.add(tileage, gbc_tileage);
      tileage.setColumns(10);
    }
    {
      JLabel lblDays = new JLabel("days");
      GridBagConstraints gbc_lblDays = new GridBagConstraints();
      gbc_lblDays.insets = new Insets(0, 0, 5, 0);
      gbc_lblDays.gridx = 2;
      gbc_lblDays.gridy = 4;
      contentPanel.add(lblDays, gbc_lblDays);
    }
    {
      JLabel lblAdditionalDns = new JLabel("additional DNS");
      GridBagConstraints gbc_lblAdditionalDns = new GridBagConstraints();
      gbc_lblAdditionalDns.insets = new Insets(0, 0, 0, 5);
      gbc_lblAdditionalDns.gridx = 0;
      gbc_lblAdditionalDns.gridy = 5;
      contentPanel.add(lblAdditionalDns, gbc_lblAdditionalDns);
    }
    {
      JPanel panel = new JPanel();
      GridBagConstraints gbc_panel = new GridBagConstraints();
      gbc_panel.fill = GridBagConstraints.BOTH;
      gbc_panel.insets = new Insets(0, 0, 0, 5);
      gbc_panel.gridx = 1;
      gbc_panel.gridy = 5;
      contentPanel.add(panel, gbc_panel);
      panel.setLayout(new GridLayout(0, 2, 0, 0));
      {
        checkBoxGoogle = new JCheckBox("8.8.8.8");
        checkBoxGoogle.setToolTipText("Use the Google DNS");
        panel.add(checkBoxGoogle);
      }
      {
        checkBoxGCA = new JCheckBox("9.9.9.9");
        checkBoxGCA.setToolTipText("Use the DNS of the Global Cyber Alliance");
        panel.add(checkBoxGCA);
      }
    }
    restoreValues();
  }

  private void restoreValues() {
    root = log;
    while (root.getParent() != null)
      root = root.getParent();

    cmbLogLevel.setSelectedItem(root.getLevel());
    tileage
        .setText("" + (Integer.parseInt(terraMaster.getProps().getProperty(TerraMasterProperties.MAX_TILE_AGE, "100"))
            / (24 * 3600)));
    List<Integer> indicies = new ArrayList<>();
    for (String version : terraMaster.getProps().getProperty(TerraMasterProperties.SCENERY_VERSION,
            TerraMasterProperties.DEFAULT_SCENERY_VERSION).split(",")) {
      for (int i = 0; i < cmbSceneryVersion.getModel().getSize(); i++) {
        if (cmbSceneryVersion.getModel().getElementAt(i).equals(version))
          indicies.add(i);
      }
    }
    cmbSceneryVersion.setSelectedIndices(indicies.stream().mapToInt(Integer::intValue).toArray());
    checkBoxGoogle.setSelected(
        Boolean.parseBoolean(terraMaster.getProps().getProperty(TerraMasterProperties.DNS_GOOGLE, "false")));
    checkBoxGCA
        .setSelected(Boolean.parseBoolean(terraMaster.getProps().getProperty(TerraMasterProperties.DNS_GCA, "false")));
  }

  private void saveValues() {
    try {
      String versionsString = String.join(",", versions);
      terraMaster.setMapScenery(terraMaster.getTileService().newScnMap(txtScenerypath.getText()));
      terraMaster.frame.map.repaint();
      terraMaster.getProps().setProperty(TerraMasterProperties.SCENERY_PATH, txtScenerypath.getText());
      terraMaster.getProps().setProperty(TerraMasterProperties.SERVER_TYPE, versionsString);
      terraMaster.getTileService().setScnPath(new File(txtScenerypath.getText()));
      terraMaster.getProps().setProperty(TerraMasterProperties.MAX_TILE_AGE,
          "" + (Integer.parseInt(tileage.getText()) * 24 * 3600));
      terraMaster.getProps().setProperty(TerraMasterProperties.SCENERY_VERSION, versionsString);
      terraMaster.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE,
          Boolean.toString(checkBoxGoogle.isSelected()));
      terraMaster.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.toString(checkBoxGCA.isSelected()));
      terraMaster.setTileService();
    } catch (Exception x) {
      log.log(Level.WARNING, x.toString(), x);
    }
  }
}
