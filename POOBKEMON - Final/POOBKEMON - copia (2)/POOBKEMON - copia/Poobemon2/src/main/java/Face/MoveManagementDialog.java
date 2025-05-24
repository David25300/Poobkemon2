package Face;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;

public class MoveManagementDialog extends JDialog {
    private Pokemon pokemonToManage;
    private SwingGUI parentFrame; 
    private JPanel movesPanel;
    // private List<JButton> moveButtons; // No es necesario si se recrean

    private JLabel currentLevelLabel; // Para mostrar el nivel actual
    private JSpinner levelSpinner;    // Para cambiar el nivel

    private static final Color PIXEL_BLUE_DARK = Color.decode("#0070C0");
    private static final Color PIXEL_YELLOW = Color.decode("#FFDE00");
    private static final Color PIXEL_TEXT_LIGHT = Color.WHITE; // Para texto sobre fondo oscuro
    private static final Color PIXEL_BUTTON_TEXT = Color.BLACK; 
    private static final Color PIXEL_BUTTON_BG = Color.WHITE; 

    public MoveManagementDialog(SwingGUI owner, Pokemon pokemon) {
        super(owner, "Gestionar " + pokemon.getNombre(), true); 
        this.parentFrame = owner;
        this.pokemonToManage = pokemon;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        if (getContentPane() instanceof JPanel) { 
            ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(15,15,15,15)); // Más padding
        }
        getContentPane().setBackground(PIXEL_BLUE_DARK);

        Font generalFont = owner.getPixelArtFont() != null ? owner.getPixelArtFont().deriveFont(Font.PLAIN, 12f) : new Font("Monospaced", Font.PLAIN, 12);
        Font titleFont = owner.getPixelArtFont() != null ? owner.getPixelArtFont().deriveFont(Font.BOLD, 18f) : new Font("Monospaced", Font.BOLD, 18);
        
        // --- Panel Superior: Título y Gestión de Nivel ---
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Gestionar " + pokemon.getNombre(), SwingConstants.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(PIXEL_YELLOW);
        topPanel.add(titleLabel, BorderLayout.NORTH);

        // Panel para el nivel
        JPanel levelManagementPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        levelManagementPanel.setOpaque(false);
        
        currentLevelLabel = new JLabel("Nivel Actual: " + pokemonToManage.getNivel());
        currentLevelLabel.setFont(generalFont);
        currentLevelLabel.setForeground(PIXEL_TEXT_LIGHT);
        levelManagementPanel.add(currentLevelLabel);

        SpinnerModel levelModel = new SpinnerNumberModel(pokemonToManage.getNivel(), 1, 100, 1); // valor actual, min, max, paso
        levelSpinner = new JSpinner(levelModel);
        levelSpinner.setFont(generalFont);
        // Para asegurar que el editor del JSpinner también use la fuente pixelada si es posible
        JComponent editor = levelSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(generalFont);
            textField.setForeground(PIXEL_BUTTON_TEXT); // Color de texto del input
            textField.setBackground(PIXEL_BUTTON_BG);   // Fondo del input
            textField.setHorizontalAlignment(JTextField.CENTER);
        }
        levelSpinner.setPreferredSize(new Dimension(70, (int)generalFont.getSize()*2 + 10)); // Ajustar tamaño
        levelManagementPanel.add(new JLabel("Nuevo Nivel:")).setForeground(PIXEL_TEXT_LIGHT);
        levelManagementPanel.add(levelSpinner);

        JButton changeLevelButton = new JButton("Cambiar Nivel");
        changeLevelButton.setFont(generalFont);
        changeLevelButton.setBackground(PIXEL_BUTTON_BG);
        changeLevelButton.setForeground(PIXEL_BUTTON_TEXT);
        changeLevelButton.addActionListener(e -> changePokemonLevel());
        levelManagementPanel.add(changeLevelButton);
        
        topPanel.add(levelManagementPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // --- Panel Central: Movimientos ---
        JLabel movesTitleLabel = new JLabel("Movimientos:", SwingConstants.LEFT);
        movesTitleLabel.setFont(generalFont.deriveFont(Font.BOLD));
        movesTitleLabel.setForeground(PIXEL_TEXT_LIGHT);
        movesTitleLabel.setBorder(BorderFactory.createEmptyBorder(10,0,5,0)); // Espacio antes de los botones de mov.
        
        JPanel movesContainerPanel = new JPanel(new BorderLayout());
        movesContainerPanel.setOpaque(false);
        movesContainerPanel.add(movesTitleLabel, BorderLayout.NORTH);

        movesPanel = new JPanel(new GridLayout(0, 1, 8, 8)); 
        movesPanel.setOpaque(false);
        refreshMoveButtons();
        
        JScrollPane scrollPane = new JScrollPane(movesPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5,0,5,0)); 
        movesContainerPanel.add(scrollPane, BorderLayout.CENTER);

        add(movesContainerPanel, BorderLayout.CENTER);

        // --- Panel Inferior: Botón Cerrar ---
        JButton closeButton = new JButton("Cerrar");
        if (owner.getPixelArtFont() != null) closeButton.setFont(owner.getPixelArtFont().deriveFont(Font.PLAIN, 12f));
        closeButton.setBackground(PIXEL_BUTTON_BG);
        closeButton.setForeground(PIXEL_BUTTON_TEXT);
        closeButton.addActionListener(e -> dispose());
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(500, 450)); // Ajustar tamaño mínimo para el nuevo contenido
        setPreferredSize(new Dimension(550, 500)); 
        setLocationRelativeTo(owner);
    }

    private void changePokemonLevel() {
        try {
            int newLevel = (Integer) levelSpinner.getValue();
            if (newLevel >= 1 && newLevel <= 100) {
                System.out.println("[MoveManagementDialog] Cambiando nivel de " + pokemonToManage.getNombre() + " de " + pokemonToManage.getNivel() + " a " + newLevel);
                pokemonToManage.setNivel(newLevel); // Esto recalculará stats y HP
                currentLevelLabel.setText("Nivel Actual: " + pokemonToManage.getNivel()); // Actualizar label
                levelSpinner.setValue(pokemonToManage.getNivel()); // Sincronizar spinner por si acaso
                
                // Notificar a TeamSelectionPanel para que actualice las miniaturas/previsualizaciones
                if (parentFrame.getTeamSelectionPanel() != null) {
                    parentFrame.getTeamSelectionPanel().updateAllTeamDisplayPanels();
                }
                JOptionPane.showMessageDialog(this, pokemonToManage.getNombre() + " ahora es nivel " + newLevel + ".", "Nivel Cambiado", JOptionPane.INFORMATION_MESSAGE);
                // No es necesario refrescar los botones de movimiento aquí, ya que el nivel no los afecta directamente.
            } else {
                JOptionPane.showMessageDialog(this, "El nivel debe estar entre 1 y 100.", "Nivel Inválido", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor, ingresa un número válido para el nivel.", "Entrada Inválida", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshMoveButtons() {
        movesPanel.removeAll();

        Font moveButtonFont = parentFrame.getPixelArtFont() != null ? parentFrame.getPixelArtFont().deriveFont(Font.PLAIN, 11f) : new Font("Monospaced", Font.PLAIN, 11); // CORREGIDO: 11f a 11
        
        List<Movements> currentMoves = pokemonToManage.getMovimientos(); 

        System.out.println("[MoveManagementDialog refreshMoveButtons] Pokémon: " + pokemonToManage.getNombre() + ", Movimientos actuales: " + currentMoves);

        for (int i = 0; i < 4; i++) { 
            Movements move = null;
            if (i < currentMoves.size()) { 
                move = currentMoves.get(i);
            }

            final int slotIndex = i;
            JButton actionButton;

            if (move != null) {
                String buttonText = String.format("<html><body style='width: 280px; text-align: left; padding: 5px;'>" + 
                                                  "<b>%s</b><br>" +
                                                  "<font color='#333333' size='-1'>PP: %d/%d | Tipo: %s | Cat: %s</font>" +
                                                  "</body></html>",
                                                  move.getNombre(), move.getPpActual(), move.getPpMax(),
                                                  move.getTipo().name(), move.getCategoria().name());
                actionButton = new JButton(buttonText);
                actionButton.setFont(moveButtonFont); 
                actionButton.setBackground(PIXEL_BUTTON_BG);
                actionButton.setForeground(PIXEL_BUTTON_TEXT);
                actionButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createRaisedBevelBorder(),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
                actionButton.setToolTipText("Clic para cambiar este movimiento");
            } else {
                actionButton = new JButton("(Slot " + (i + 1) + " Vacío - Aprender Movimiento)");
                actionButton.setFont(moveButtonFont);
                actionButton.setBackground(Color.LIGHT_GRAY);
                actionButton.setForeground(Color.DARK_GRAY);
                 actionButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10) // Más padding vertical para botones vacíos
                ));
                actionButton.setToolTipText("Clic para aprender un nuevo movimiento en este slot");
            }
            
            actionButton.setHorizontalAlignment(SwingConstants.LEFT);
            actionButton.setFocusPainted(false); // Mejor estética
            actionButton.addActionListener(e -> openMoveSelectionDialog(slotIndex));
            movesPanel.add(actionButton);
        }
        movesPanel.revalidate();
        movesPanel.repaint();
    }

    private void openMoveSelectionDialog(int moveSlotIndex) {
        System.out.println("[MoveManagementDialog] openMoveSelectionDialog: Iniciando para slot " + moveSlotIndex);
        if (parentFrame == null) {
            System.err.println("[MoveManagementDialog] ERROR CRÍTICO: parentFrame es null!");
            JOptionPane.showMessageDialog(this, "Error interno: Referencia a la ventana principal no encontrada.", "Error Crítico", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (pokemonToManage == null) {
            System.err.println("[MoveManagementDialog] ERROR CRÍTICO: pokemonToManage es null!");
             JOptionPane.showMessageDialog(this, "Error interno: No se ha especificado un Pokémon para gestionar.", "Error Crítico", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.out.println("[MoveManagementDialog] Pokémon a gestionar: " + pokemonToManage.getNombre());

        Map<String, Movements> allMovesMap = parentFrame.getAllAvailableMoves();
        if (allMovesMap == null) {
            System.err.println("[MoveManagementDialog] ERROR CRÍTICO: parentFrame.getAllAvailableMoves() devolvió null! Los movimientos base no se cargaron.");
            JOptionPane.showMessageDialog(this, "Error: No se pudieron cargar los movimientos disponibles desde la base de datos del juego.", "Error Interno de Datos", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Movements> allGameMoves = new ArrayList<>(allMovesMap.values());
        System.out.println("[MoveManagementDialog] Número total de movimientos disponibles en el juego: " + allGameMoves.size());
        if (allGameMoves.isEmpty()) {
            System.err.println("[MoveManagementDialog] ADVERTENCIA: La lista de todos los movimientos del juego está vacía.");
             JOptionPane.showMessageDialog(this, "Advertencia: No hay movimientos disponibles en la base de datos del juego para seleccionar.", "Datos Faltantes", JOptionPane.WARNING_MESSAGE);
        }

        Font fontToUse = parentFrame.getPixelArtFont();
        if (fontToUse == null) {
            System.out.println("[MoveManagementDialog] Usando fuente por defecto porque parentFrame.getPixelArtFont() es null.");
            fontToUse = new Font("Monospaced", Font.PLAIN, 12); 
        }

        JDialog dialog = new JDialog(this, "Seleccionar Movimiento para Slot " + (moveSlotIndex + 1) + " de " + pokemonToManage.getNombre(), true);
        System.out.println("[MoveManagementDialog] JDialog para MoveSelectionPanel creado.");
        
        MoveSelectionPanel moveSelectionPanel;
        try {
            moveSelectionPanel = new MoveSelectionPanel(dialog, pokemonToManage, moveSlotIndex, allGameMoves, fontToUse);
            System.out.println("[MoveManagementDialog] MoveSelectionPanel instanciado exitosamente.");
        } catch (Exception e) {
            System.err.println("[MoveManagementDialog] EXCEPCIÓN al crear MoveSelectionPanel:");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error crítico al crear el panel de selección de movimientos:\n" + e.getMessage(), "Error Interno Grave", JOptionPane.ERROR_MESSAGE);
            dialog.dispose(); 
            return;
        }
        
        dialog.setContentPane(moveSelectionPanel);
        System.out.println("[MoveManagementDialog] ContentPane para MoveSelectionPanel asignado.");
        dialog.pack();
        System.out.println("[MoveManagementDialog] Dialog packed. Tamaño después de pack(): " + dialog.getSize());
        
        Dimension currentMinSize = dialog.getMinimumSize();
        Dimension preferredMinSize = new Dimension(450, 350); 
        dialog.setMinimumSize(new Dimension(Math.max(currentMinSize.width, preferredMinSize.width), Math.max(currentMinSize.height, preferredMinSize.height)));
        
        dialog.setLocationRelativeTo(this);
        System.out.println("[MoveManagementDialog] Intentando hacer visible el diálogo de selección de movimientos...");
        
        try {
            dialog.setVisible(true); 
            System.out.println("[MoveManagementDialog] Llamada a dialog.setVisible(true) completada. El diálogo debería estar o haber estado visible.");
        } catch (Exception e) {
            System.err.println("[MoveManagementDialog] EXCEPCIÓN durante dialog.setVisible(true):");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al intentar mostrar el diálogo de selección de movimientos:\n" + e.getMessage(), "Error de Visualización", JOptionPane.ERROR_MESSAGE);
            dialog.dispose(); 
            return;
        }

        System.out.println("[MoveManagementDialog] Diálogo de selección de movimiento cerrado. Refrescando botones en MoveManagementDialog...");
        refreshMoveButtons(); // Actualizar los botones de movimiento en este diálogo
        
        // También es crucial actualizar la visualización del Pokémon en TeamSelectionPanel
        if (parentFrame.getTeamSelectionPanel() != null) {
            parentFrame.getTeamSelectionPanel().updateAllTeamDisplayPanels(); 
            System.out.println("[MoveManagementDialog] TeamSelectionPanel.updateAllTeamDisplayPanels() llamado.");
        } else {
            System.err.println("[MoveManagementDialog] ADVERTENCIA: parentFrame.getTeamSelectionPanel() es null. No se puede actualizar el panel de selección de equipo.");
        }
    }
}