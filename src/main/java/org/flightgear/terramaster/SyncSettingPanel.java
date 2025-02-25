package org.flightgear.terramaster;

import java.awt.Component;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author PortreeKid
 */
public class SyncSettingPanel extends javax.swing.JPanel {

  private TerraSyncRootDirectoryType dirType = TerraSyncRootDirectoryType.UNKNOWN;
  private String version;

  /**
   * Creates new form SyncSettingPanel
   */
  public SyncSettingPanel() {
    initComponents();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    label = new javax.swing.JLabel();
    labelURL = new javax.swing.JLabel();
    enabled = new javax.swing.JCheckBox();
    dirname = new javax.swing.JTextField();
    jButton1 = new javax.swing.JButton();
    jPanel1 = new javax.swing.JPanel();

    java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
    layout.columnWidths = new int[] {0, 0, 0};
    layout.rowHeights = new int[] {0, 0, 0};
    layout.columnWeights = new double[] {0.1, 5.0, 0.1};
    setLayout(layout);

    label.setText("jLabel1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridy = 0;
    add(label, gridBagConstraints);

    labelURL.setText("jLabelURL");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.ipadx = 2;
    gridBagConstraints.ipady = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    add(labelURL, gridBagConstraints);

    enabled.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        enabledActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 0.1;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(enabled, gridBagConstraints);

    dirname.setText("jTextField1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 200.0;
    gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
    add(dirname, gridBagConstraints);

    jButton1.setText("...");
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(12, 6, 0, 6);
    add(jButton1, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.ipadx = 152;
    gridBagConstraints.ipady = 24;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 28, 0);
    add(jPanel1, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

    private void enabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enabledActionPerformed
      if (this.enabled.isSelected() && this.dirType==TerraSyncRootDirectoryType.WS30) {
        JOptionPane.showMessageDialog(this, "Beware WS30 is experimental", "Beware", JOptionPane.WARNING_MESSAGE);
      }
    }//GEN-LAST:event_enabledActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
      JFileChooser jFileChooser = new JFileChooser(this.dirname.getText());
      jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      jFileChooser.showDialog(this.getParent(), "Select");
      this.dirname.setText(jFileChooser.getSelectedFile().getAbsolutePath());
    }//GEN-LAST:event_jButton1ActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JTextField dirname;
  private javax.swing.JCheckBox enabled;
  private javax.swing.JButton jButton1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JLabel label;
  private javax.swing.JLabel labelURL;
  // End of variables declaration//GEN-END:variables

  void setTitle(String version) {
    this.version = version;
    label.setText(version + " (" + dirType.name() + ")");   
    labelURL.setText("http://terramaster.flightgear.org/terrasync/" + version);
  }

  void setDir(TerraSyncRootDirectory dirType) {
    this.dirType = dirType.getTerraSyncRootDirectoryType();
    label.setText(version + " (" + dirType.getTerraSyncRootDirectoryType().name() + ")");
    setDirTypes(Arrays.asList(dirType.getTypes()));
  }
  
  TerraSyncRootDirectoryType getDirType() {
    return this.dirType;
  }
  
  public void setDirTypes(Iterable<TerraSyncDirectoryType> dirtypes) {
    for (TerraSyncDirectoryType type : dirtypes) {
        JCheckBox chckbxTsdt = new JCheckBox(type.name());
        jPanel1.add(chckbxTsdt);
      }
  }

  void setDirname(String property) {
    this.dirname.setText(property);
  }

  void setEnabledDirs(String property) {
    if (property.isEmpty()) {
      return;
    }
    for (String string : property.split(",")) {
      TerraSyncDirectoryType type = TerraSyncDirectoryType.valueOf(string);
      Optional<Component> checkbox = Arrays.stream(jPanel1.getComponents()).filter(c -> ((JCheckBox)c).getLabel().equals(type.name())).findAny();
      if(checkbox.isPresent()) {
        ((JCheckBox)(checkbox.get())).setSelected(true);
      }
    }
  }
  
  public TerraSyncDirectoryType[] getDirEnabled() {
    TerraSyncDirectoryType[] ret = Arrays.stream(jPanel1.getComponents()).filter(c -> ((JCheckBox)c).isSelected()).map((t) -> TerraSyncDirectoryType.valueOf(((JCheckBox)t).getLabel())).toArray(TerraSyncDirectoryType[]::new);
    return  ret;
  }

  boolean save(Properties props) {
    props.setProperty(this.dirType + "." + TerraMasterProperties.SCENERY_PATH, this.dirname.getText());    
    String enabledTypesString = String.join(",", Arrays.stream(getDirEnabled()).map(t -> t.name()).collect(Collectors.toList()));
    props.setProperty(this.dirType + "." + TerraMasterProperties.ENABLED_DIRECTORIES, enabledTypesString);    
    props.setProperty(this.dirType + "." + TerraMasterProperties.ENABLED, Boolean.toString(enabled.isSelected()));    
    props.setProperty(this.dirType + "." + TerraMasterProperties.URL, labelURL.getText());    
    return this.enabled.isSelected();
  }

  void setDirEnabled(String property) {
    this.enabled.setSelected(Boolean.parseBoolean(property));
  }
}
