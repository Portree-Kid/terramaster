package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Settings dialog of the GUI
 *
 * @author keith.paterson
 * @author Simon
 */
public class SettingsDialog extends JDialog {

  transient Logger log = Logger.getLogger(getClass().getName());
  private final ArrayList<Level> levels = new ArrayList<>();
  private final JComboBox<Level> cmbLogLevel;
  private Logger root;
  private final JTextField tileage;
  private final TerraMaster terraMaster;
  private String[] directories = new String[0];

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
    setBounds(100, 100, 466, 280);
    getContentPane().setLayout(new BorderLayout());
    JPanel contentPanel = new JPanel();
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    GridBagLayout gbl_contentPanel = new GridBagLayout();
    gbl_contentPanel.columnWidths = new int[]{0, 0, 40, 0};
    gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
    gbl_contentPanel.columnWeights = new double[]{0.0, 1.0, 1.0, Double.MIN_VALUE};
    gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    contentPanel.setLayout(gbl_contentPanel);
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
      JLabel lblNewLabel = new JLabel("Sync");
      GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
      gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
      gbc_lblNewLabel.gridx = 0;
      gbc_lblNewLabel.gridy = 2;
      contentPanel.add(lblNewLabel, gbc_lblNewLabel);
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
        if (root != null) {
          root.setLevel(cmbLogLevel.getItemAt(cmbLogLevel.getSelectedIndex()));
        }
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
    restoreValues();
    {
      new Thread(() -> {
        try {
          directories = ApacheDirectoryParser.listDirectories(new URL("http://terramaster.flightgear.org/terrasync/"));
          Component[] c = contentPanel.getComponents();
          for (Component component : c) {
            final GridBagLayout layout = (GridBagLayout) contentPanel.getLayout();
            GridBagConstraints constraints = layout.getConstraints(component);
            System.out.println(component.getName());
            constraints.gridy += directories.length + 1;
            layout.setConstraints(component, constraints);
          }
          contentPanel.revalidate();

          int i = 0;
          for (String version : directories) {
            TerraSyncRootDirectory dir = ApacheDirectoryParser.getType(new URL("http://terramaster.flightgear.org/terrasync/"), version);
            SyncSettingPanel syncPanel = new SyncSettingPanel();
            syncPanel.setTitle(version);
            syncPanel.setDir(dir);
            syncPanel.setDirname(terraMaster.getProps().getProperty(dir.getTerraSyncRootDirectoryType().name() + "." + TerraMasterProperties.SCENERY_PATH, ""));
            syncPanel.setEnabledDirs(terraMaster.getProps().getProperty(dir.getTerraSyncRootDirectoryType().name() + "." + TerraMasterProperties.ENABLED_DIRECTORIES, ""));
            syncPanel.setDirEnabled(terraMaster.getProps().getProperty(dir.getTerraSyncRootDirectoryType().name() + "." + TerraMasterProperties.ENABLED, "false"));
            GridBagConstraints gbcSyncTiles = new GridBagConstraints();
            gbcSyncTiles.insets = new Insets(0, 0, 5, 5);
            gbcSyncTiles.fill = GridBagConstraints.HORIZONTAL;

            gbcSyncTiles.gridwidth = 3;
            gbcSyncTiles.gridx = 0;
            gbcSyncTiles.gridy = ++i;
            contentPanel.add(syncPanel, gbcSyncTiles);
            syncPanel.revalidate();
            contentPanel.revalidate();
            revalidate();
            Dimension dim = getSize();
            dim.setSize(dim.width, dim.height + syncPanel.getHeight());
            setSize(dim);
            revalidate();
          }

          restoreValues();
        } catch (MalformedURLException ex) {
          Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
      }).start();
    }
  }


  private void restoreValues() {
    root = log;
    while (root.getParent() != null) {
      root = root.getParent();
    }

    cmbLogLevel.setSelectedItem(root.getLevel());
    tileage
            .setText("" + (Integer.parseInt(terraMaster.getProps().getProperty(TerraMasterProperties.MAX_TILE_AGE, "100"))
                    / (24 * 3600)));
  }

  private void saveValues() {
    try {
      final BorderLayout layout = (BorderLayout) getContentPane().getLayout();
      final JPanel center = (JPanel) layout.getLayoutComponent(BorderLayout.CENTER);

      Arrays.stream(center.getComponents()).filter((t) -> t.getClass().equals(SyncSettingPanel.class)).map((t) -> (SyncSettingPanel)t).forEach((t) -> {
        t.save(terraMaster.getProps()); 
      });


//      terraMaster.setMapScenery(terraMaster.getTileService().newScnMap(txtScenerypath.getText()));
      terraMaster.frame.map.repaint();
//      terraMaster.getProps().setProperty(TerraMasterProperties.SCENERY_PATH, txtScenerypath.getText());
//      terraMaster.getTileService().setScnPath(new File(txtScenerypath.getText()));
      terraMaster.getProps().setProperty(TerraMasterProperties.MAX_TILE_AGE,
              "" + (Integer.parseInt(tileage.getText()) * 24 * 3600));
      terraMaster.setTileService();
    } catch (Exception x) {
      log.log(Level.WARNING, x.toString(), x);
    }
  }
}
