package Face;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import PokeBody.domain.Movements;
import PokeBody.domain.Pokemon;
import PokeBody.domain.Type;

public class MoveSelectionPanel extends JPanel {
    private JDialog parentDialog; 
    private List<Movements> allMoves;
    private List<Movements> filteredMoves;
    private JList<String> movesDisplayList;
    private DefaultListModel<String> listModel;
    private JTextField searchField;
    private JComboBox<Type.Tipo> typeFilterComboBox;
    private JComboBox<Movements.TipoAtaque> categoryFilterComboBox;
    // private JButton selectButton; // No es necesario como campo si la acción está en el listener
    // private final JButton cancelButton; // No es necesario como campo si la acción está en el listener
    private Pokemon selectedPokemon; 
    private int moveSlotToChange;   

    private static final Color PIXEL_BLUE_LIGHT = Color.decode("#00A8E8");

    public MoveSelectionPanel(JDialog parentDialog, Pokemon pokemon, int moveSlotToChange, List<Movements> allMoves, Font pixelFont) {
        this.parentDialog = parentDialog;
        this.selectedPokemon = pokemon;
        this.moveSlotToChange = moveSlotToChange;
        this.allMoves = new ArrayList<>(allMoves); 
        this.filteredMoves = new ArrayList<>(this.allMoves);

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(PIXEL_BLUE_LIGHT);

        // --- Panel de Filtros ---
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(new JLabel("Buscar:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        searchField = new JTextField(15);
        if (pixelFont != null) searchField.setFont(pixelFont.deriveFont(Font.PLAIN, 11f));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterMoves(); }
            public void removeUpdate(DocumentEvent e) { filterMoves(); }
            public void insertUpdate(DocumentEvent e) { filterMoves(); }
        });
        filterPanel.add(searchField, gbc);
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy = 1;
        filterPanel.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        Type.Tipo[] types = Type.Tipo.values();
        Type.Tipo[] typesWithAll = new Type.Tipo[types.length + 1];
        typesWithAll[0] = null; 
        System.arraycopy(types, 0, typesWithAll, 1, types.length);
        typeFilterComboBox = new JComboBox<>(typesWithAll);
        typeFilterComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) setText("Todos");
                else setText(((Type.Tipo) value).name());
                return this;
            }
        });
        if (pixelFont != null) typeFilterComboBox.setFont(pixelFont.deriveFont(Font.PLAIN, 11f));
        typeFilterComboBox.addActionListener(e -> filterMoves());
        filterPanel.add(typeFilterComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        filterPanel.add(new JLabel("Categoría:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        Movements.TipoAtaque[] categories = Movements.TipoAtaque.values();
        Movements.TipoAtaque[] categoriesWithAll = new Movements.TipoAtaque[categories.length + 1];
        categoriesWithAll[0] = null; 
        System.arraycopy(categories, 0, categoriesWithAll, 1, categories.length);
        categoryFilterComboBox = new JComboBox<>(categoriesWithAll);
         categoryFilterComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) setText("Todas");
                else setText(((Movements.TipoAtaque) value).name());
                return this;
            }
        });
        if (pixelFont != null) categoryFilterComboBox.setFont(pixelFont.deriveFont(Font.PLAIN, 11f));
        categoryFilterComboBox.addActionListener(e -> filterMoves());
        filterPanel.add(categoryFilterComboBox, gbc);

        add(filterPanel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        movesDisplayList = new JList<>(listModel);
        if (pixelFont != null) movesDisplayList.setFont(pixelFont.deriveFont(Font.PLAIN, 10f));
        movesDisplayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        movesDisplayList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String && index < filteredMoves.size() && index >= 0) { 
                    Movements move = filteredMoves.get(index); 
                    label.setText(String.format("%s (PP:%d, Pow:%d, Acc:%d, %s, %s)",
                                  move.getNombre(), move.getPpMax(), move.getPotencia(), move.getPrecision(),
                                  move.getTipo().name(), move.getCategoria().name()));
                } else if (value instanceof String) {
                    label.setText((String) value); 
                }
                return label;
            }
        });
        JScrollPane scrollPane = new JScrollPane(movesDisplayList);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        JButton selectButton = new JButton("Seleccionar"); // Local variable is fine
        if (pixelFont != null) selectButton.setFont(pixelFont.deriveFont(Font.PLAIN, 11f));
        selectButton.addActionListener(e -> selectMove());
        
        JButton cancelButton = new JButton("Cancelar"); // Local variable is fine
        if (pixelFont != null) cancelButton.setFont(pixelFont.deriveFont(Font.PLAIN, 11f));
        cancelButton.addActionListener(e -> parentDialog.dispose());

        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        filterMoves(); 
    }

    private void filterMoves() {
        String searchText = searchField.getText().toLowerCase().trim();
        Type.Tipo selectedType = (Type.Tipo) typeFilterComboBox.getSelectedItem();
        Movements.TipoAtaque selectedCategory = (Movements.TipoAtaque) categoryFilterComboBox.getSelectedItem();

        filteredMoves = allMoves.stream()
            .filter(move -> move.getNombre().toLowerCase().contains(searchText))
            .filter(move -> selectedType == null || move.getTipo() == selectedType)
            .filter(move -> selectedCategory == null || move.getCategoria() == selectedCategory)
            .collect(Collectors.toList());

        listModel.clear();
        for (Movements move : filteredMoves) {
            listModel.addElement(move.getNombre());
        }
    }

    private void selectMove() {
        int selectedListIndex = movesDisplayList.getSelectedIndex();
        if (selectedListIndex != -1 && selectedListIndex < filteredMoves.size()) {
            Movements selectedMoveFromMasterList = filteredMoves.get(selectedListIndex);

            List<Movements> currentPokemonMoves = selectedPokemon.getMovimientos();
            for (int i = 0; i < currentPokemonMoves.size(); i++) {
                if (i != moveSlotToChange && currentPokemonMoves.get(i) != null && 
                    currentPokemonMoves.get(i).getNombre().equals(selectedMoveFromMasterList.getNombre())) {
                    JOptionPane.showMessageDialog(parentDialog,
                        selectedPokemon.getNombre() + " ya conoce el movimiento " + selectedMoveFromMasterList.getNombre() + ".",
                        "Movimiento Duplicado", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            
            Movements learnedMoveInstance = new Movements(selectedMoveFromMasterList);
            
            selectedPokemon.setLearnedMove(moveSlotToChange, learnedMoveInstance);
            
            System.out.println("[MoveSelectionPanel] Movimiento asignado: " + learnedMoveInstance.getNombre() + 
                               " a " + selectedPokemon.getNombre() + " en slot " + moveSlotToChange + 
                               ". Movimientos actuales: " + selectedPokemon.getMovimientos());
            parentDialog.dispose(); 
        } else {
            JOptionPane.showMessageDialog(parentDialog, "Por favor, selecciona un movimiento de la lista.", "Ningún Movimiento Seleccionado", JOptionPane.WARNING_MESSAGE);
        }
    }
}
