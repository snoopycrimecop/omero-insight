/*
 * org.openmicroscopy.shoola.agents.util.SelectionWizardUI 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2014 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.util;


//Java imports
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


//Third-party libraries
import info.clearthought.layout.TableLayout;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import org.openmicroscopy.shoola.agents.treeviewer.util.TreeCellRenderer;
import org.openmicroscopy.shoola.agents.util.browser.TreeImageDisplay;
import org.openmicroscopy.shoola.agents.util.browser.TreeImageSet;
import org.openmicroscopy.shoola.agents.util.browser.TreeViewerTranslator;
//Application-internal dependencies
import org.openmicroscopy.shoola.util.ui.IconManager;
import org.openmicroscopy.shoola.util.ui.UIUtilities;
import pojos.DataObject;
import pojos.DatasetData;
import pojos.ExperimenterData;
import pojos.FileAnnotationData;
import pojos.GroupData;
import pojos.TagAnnotationData;


/**
 * Provided UI to select between two lists of objects.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * @since 3.0-Beta4
 */
public class SelectionWizardUI
    extends JPanel
    implements ActionListener, DocumentListener
{

    /** Bound property indicating that the selection has changed. */
    public static final String SELECTION_CHANGE = "selectionChange";

    /** The default text for the filter dialog.*/
    private static final String DEFAULT_FILTER_TEXT = "Filter";

    /** Action command ID to add a field to the result table. */
    private static final int ADD = 0;

    /** Action command ID to remove a field from the result table. */
    private static final int REMOVE = 1;

    /** Action command ID to add all fields to the result table. */
    private static final int ADD_ALL = 2;

    /** Action command ID to remove all fields from the result table. */
    private static final int REMOVE_ALL = 3;

    /** The original items before the user selects items. */
    private List<TreeImageDisplay> originalItems;

    /** The original selected items before the user selects items. */
    private List<TreeImageDisplay> originalSelectedItems;

    /** Collection of items, removed and/or added.*/
    private Set<TreeImageDisplay> children;

    /** Collection of available items. */
    private List<TreeImageDisplay> availableItems;

    /** Collection of all the selected items. */
    private List<TreeImageDisplay> selectedItems;

    /** The list box showing the available items. */
    private JTree availableItemsListbox;

    /** The list box showing the selected items. */
    private JTree selectedItemsListbox;

    /** The button to move an item from the remaining items to current items. */
    private JButton addButton;

    /** The button to move an item from the current items to remaining items. */
    private JButton removeButton;

    /** The button to move all items to the current items. */
    private JButton addAllButton;

    /** The button to move all items to the remaining items. */
    private JButton removeAllButton;

    /** Sorts the object. */
    private ViewerSorter sorter;

    /** The type to handle. */
    private Class type;

    /** The collection of immutable  nodes. */
    private Collection immutable;

    /** The group available.*/
    private Collection<GroupData> groups;

    /** Filter the data.*/
    private JTextField filterArea;

    /** Flag indicating to filter with letter anywhere in the text.*/
    private boolean filterAnywhere;

    /** The original color of a text field.*/
    private Color originalColor;

    /**
     * Returns <code>true</code> if the item is already selected or is
     * an item to create, <code>false</code> otherwise.
     *
     * @param elt The element to handle.
     * @return See above.
     */
    private boolean isSelected(Object elt)
    {
        for (TreeImageDisplay item : selectedItems) {
            DataObject data = (DataObject) item.getUserObject();
            if (elt == item || data.getId() < 0) {
                return true;
            }
        }
        return false;
    }
    /**
     * Filters the list of displayed items.
     *
     * @param insert Pass <code>true</code> when inserting new character
     *               <code>false</code> when removing.
     */
    private void filter(boolean insert)
    {
        String txt = filterArea.getText();
        if (DEFAULT_FILTER_TEXT.equals(txt)) {
            return;
        }
        List<TreeImageDisplay> ref;
        Iterator<TreeImageDisplay> i;
        TreeImageDisplay node, child;
        Object ho;
        String value;
        if (insert) {
            ref = availableItems;
        } else {
            ref = new ArrayList<TreeImageDisplay>();
            for (TreeImageDisplay item : originalItems)
                ref.add(item);
            for (TreeImageDisplay item : originalSelectedItems) {
                if (!isSelected(item))
                    ref.add(item);
            }
        }
        i = ref.iterator();

        txt = txt.toLowerCase();
        List<TreeImageDisplay> toKeep = new ArrayList<TreeImageDisplay>();
        while (i.hasNext()) {
            node = i.next();
            ho = node.getUserObject();
            value = null;
            if (ho instanceof TagAnnotationData) {
                TagAnnotationData tag = (TagAnnotationData) ho;
                if (!TagAnnotationData.INSIGHT_TAGSET_NS.equals(
                        tag.getNameSpace())) {
                    value = tag.getTagValue();
                } else {
                    
                    //toKeep.add(node);
                    List l = node.getChildrenDisplay();
                    Iterator j = l.iterator();
                    while (j.hasNext()) {
                        child = (TreeImageDisplay) j.next();
                        if (!children.contains(child)) {
                            ho = child.getUserObject();
                            if (ho instanceof TagAnnotationData) {
                                tag = (TagAnnotationData) ho;
                                value = tag.getTagValue();
                                value = value.toLowerCase();
                                if (filterAnywhere) {
                                    if (value.contains(txt)) {
                                        toKeep.add(node);
                                        break;
                                    }
                                } else {
                                    if (value.startsWith(txt)) {
                                        toKeep.add(node);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    value = null;
                }
            } else if (ho instanceof FileAnnotationData) {
                value = ((FileAnnotationData) ho).getFileName();
            } else if (ho instanceof DatasetData) {
                value = ((DatasetData) ho).getName();
            }
            if (value != null) {
                value = value.toLowerCase();
                if (filterAnywhere) {
                    if (value.contains(txt)) {
                        toKeep.add(node);
                    }
                } else {
                    if (value.startsWith(txt)) {
                        toKeep.add(node);
                    }
                }
            }
        }
        
        availableItems.clear();
        availableItems.addAll(toKeep);
        availableItems = sorter.sort(availableItems);
        populateTreeItems(availableItemsListbox, availableItems);
    }

    /**
     * Returns <code>true</code> if an object object of the same type 
     * already exist in the list, <code>false</code> otherwise.
     *
     * @param object The object to handle.
     * @return See above.
     */
    private boolean doesObjectExist(DataObject object)
    {
        if (object == null) return false;
        if (object instanceof TagAnnotationData) {
            Iterator<TreeImageDisplay> i = availableItems.iterator();
            TagAnnotationData ob;
            String value = ((TagAnnotationData) object).getTagValue();
            if (value == null) return false;
            String v;
            TreeImageDisplay node;
            while (i.hasNext()) {
                node = i.next();
                ob = (TagAnnotationData) node.getUserObject();
                if (ob != null) {
                    v = ob.getTagValue();
                    if (v != null && v.equals(value))
                        return true;
                }
            }
            i = selectedItems.iterator();
            while (i.hasNext()) {
                node = i.next();
                ob = (TagAnnotationData) node.getUserObject();
                if (ob != null) {
                    v = ob.getTagValue();
                    if (v != null && v.equals(value))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Sets the default text for the specified field.
     *
     * @param text The text to display
     */
    private void setTextFieldDefault(String text)
    {
        filterArea.getDocument().removeDocumentListener(this);
        if (text == null) {
            filterArea.setText("");
            filterArea.setForeground(originalColor);
        } else {
            filterArea.setText(text);
            filterArea.setForeground(Color.LIGHT_GRAY);
        }
        filterArea.getDocument().addDocumentListener(this);
    }

    /**
     * Initializes the specified tree
     * 
     * @param tree The tree to handle.
     * @param user The user currently logged in.
     */
    private void initializeTree(JTree tree, ExperimenterData user)
    {
        tree.setVisible(true);
        tree.setRootVisible(false);
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setCellRenderer(new TreeCellRenderer(user.getId()));
        tree.setShowsRootHandles(true);
        TreeImageSet root = new TreeImageSet("");
        tree.setModel(new DefaultTreeModel(root));
    }

    /** 
     * Initializes the components composing the display. 
     *
     * @param user The user currently logged in.
     */
    private void initComponents(ExperimenterData user)
    {
        filterAnywhere = true;
        filterArea = new JTextField();
        originalColor = filterArea.getForeground();
        setTextFieldDefault(DEFAULT_FILTER_TEXT);
        filterArea.getDocument().addDocumentListener(this);
        filterArea.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent evt) {
                String value = filterArea.getText();
                if (StringUtils.isBlank(value)) {
                    setTextFieldDefault(DEFAULT_FILTER_TEXT);
                }
            }
            
            @Override
            public void focusGained(FocusEvent evt) {
                String value = filterArea.getText();
                if (DEFAULT_FILTER_TEXT.equals(value)) {
                    //filterArea.setCaretPosition(0);
                    //setTextFieldDefault(null);
                }
            }
        });
        sorter = new ViewerSorter();
        availableItemsListbox = new JTree();
        initializeTree(availableItemsListbox, user);
        availableItemsListbox.addKeyListener(new KeyAdapter() {

            /**
             * Adds the items to the selected list.
             * @see KeyListener#keyPressed(KeyEvent)
             */
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (availableItemsListbox.isFocusOwner())
                        addItem();
                }
            }
        });
        availableItemsListbox.addMouseListener(new MouseAdapter() {

            /**
             * Adds the items to the selected list.
             * @see MouseListener#mouseReleased(MouseEvent)
             */
            public void mouseReleased(MouseEvent e)
            {
                if (e.getClickCount() == 2) {
                    if (availableItemsListbox.isFocusOwner())
                        addItem();
                }
            }
        });
        selectedItemsListbox = new JTree();
        initializeTree(selectedItemsListbox, user);
        selectedItemsListbox.addKeyListener(new KeyAdapter() {

            /**
             * Removes the selected elements from the selected list.
             * @see KeyListener#keyPressed(KeyEvent)
             */
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (selectedItemsListbox.isFocusOwner())
                        removeItem();
                }
            }
        });
        selectedItemsListbox.addMouseListener(new MouseAdapter() {

            /**
             * Removes the selected elements from the selected list.
             * @see MouseListener#mouseReleased(MouseEvent)
             */
            public void mouseReleased(MouseEvent e)
            {
                if (e.getClickCount() == 2) {
                    if (selectedItemsListbox.isFocusOwner())
                        removeItem();
                }
            }
        });
        IconManager icons = IconManager.getInstance();
        addButton = new JButton(icons.getIcon(IconManager.RIGHT_ARROW));
        removeButton = new JButton(icons.getIcon(IconManager.LEFT_ARROW));
        addAllButton = new JButton(
                icons.getIcon(IconManager.DOUBLE_RIGHT_ARROW));
        removeAllButton = new JButton(
                icons.getIcon(IconManager.DOUBLE_LEFT_ARROW));

        addButton.setActionCommand(""+ADD);
        addButton.addActionListener(this);
        addAllButton.setActionCommand(""+ADD_ALL);
        addAllButton.addActionListener(this);
        removeButton.setActionCommand(""+REMOVE);
        removeButton.addActionListener(this);
        removeAllButton.setActionCommand(""+REMOVE_ALL);
        removeAllButton.addActionListener(this);
        setImmutableElements(null);
    }

    /** Creates a copy of the original selections. */
    private void createOriginalSelections()
    {
        originalItems = new ArrayList<TreeImageDisplay>();
        if (availableItems != null) {
            for (TreeImageDisplay item : availableItems)
                originalItems.add(item);
        }

        originalSelectedItems  = new ArrayList<TreeImageDisplay>();
        if (selectedItems != null) {
            for (TreeImageDisplay item : selectedItems)
                originalSelectedItems.add(item);
        }
    }

    /** Adds all the items to the selection. */
    private void addAllItems()
    {
        TreeImageDisplay child;
        List<TreeImageDisplay> toKeep = new ArrayList<TreeImageDisplay>();
        for (TreeImageDisplay node: availableItems) {
            if (node.hasChildrenDisplay()) { //tagset
                toKeep.add(node);
                List l = node.getChildrenDisplay();
                Iterator j = l.iterator();
                while (j.hasNext()) {
                    child = (TreeImageDisplay) j.next();
                    if (!isSelected(child)) {
                        selectedItems.add(child);
                        children.add(child);
                    }
                }
            } else {
                if (!isSelected(node)) {
                    selectedItems.add(node);
                    TreeImageDisplay parent = node.getParentDisplay();
                    if (parent != null &&
                            parent.getUserObject() instanceof DataObject) {
                        children.add(node);
                    }
                }
            }
        }
        availableItems.retainAll(toKeep);
        sortLists();
        populateTreeItems(availableItemsListbox, availableItems);
        populateTreeItems(selectedItemsListbox, selectedItems);
        setSelectionChange();
    }

    /**
     * Returns <code>true</code> if the node cannot remove,
     * <code>false</code> otherwise.
     *
     * @param data The element to handle.
     * @return See above.
     */
    private boolean isImmutable(DataObject data)
    {
        Iterator i = immutable.iterator();
        DataObject o;
        while (i.hasNext()) {
            o = (DataObject) i.next();
            if (data.getId() == o.getId()) return true;
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the node is the child of an available node,
     * <code>false</code> otherwise.
     * 
     * @param node The node to handle.
     * @return See above.
     */
    private boolean isChild(TreeImageDisplay node)
    {
        return false;
    }

    /** Removes an item from the selection. */
    private void removeItem()
    {
        TreePath[] paths = selectedItemsListbox.getSelectionPaths();
        if (paths == null || paths.length == 0) return;
        Object c;
        TreeImageDisplay node;
        Object ho;
        DataObject data;
        List<TreeImageDisplay> toRemove = new ArrayList<TreeImageDisplay>();
        System.err.println("children:" +children);
        for (int i = 0; i < paths.length; i++) {
            c = paths[i].getLastPathComponent();
            if (c instanceof TreeImageDisplay) {
                node = (TreeImageDisplay) c;
                ho = node.getUserObject();
                if (ho instanceof DataObject) {
                    data = (DataObject) ho;
                    if (!isImmutable(data)) {
                        if (data.getId() >= 0) {
                            if (children.contains(node)) {
                                children.remove(node);
                            } else {
                                //Check if node is in a tagset.
                                if (!isChild(node)) {
                                    availableItems.add(node);
                                }
                            }
                        }
                        toRemove.add(node);
                    }
                } else {
                    toRemove.add(node);
                }
            }
        }
        selectedItems.removeAll(toRemove);
        sortLists();
        populateTreeItems(availableItemsListbox, availableItems);
        populateTreeItems(selectedItemsListbox, selectedItems);
    }

    /** Removes all items from the selection. */
    private void removeAllItems()
    {
        List<TreeImageDisplay> toKeep = new ArrayList<TreeImageDisplay>();
        Object ho;
        DataObject data;
        for (TreeImageDisplay node: selectedItems) {
            ho = node.getUserObject();
            if (ho instanceof DataObject) {
                data = (DataObject) ho;
                if (isImmutable(data)) {
                    toKeep.add(node);
                } else {
                    if (data.getId() >= 0) {
                        if (children.contains(node)) {
                            children.remove(node);
                        } else {
                            //Check if node is in a tagset.
                            if (!isChild(node)) {
                                availableItems.add(node);
                            }
                        }
                    }
                }
            }
        }
        selectedItems.retainAll(toKeep);
        sortLists();
        populateTreeItems(availableItemsListbox, availableItems);
        populateTreeItems(selectedItemsListbox, selectedItems);
        setSelectionChange();
    }

    /** Adds an item to the list and then sorts the list to maintain order.*/
    private void addItem()
    {
        TreePath[] paths = availableItemsListbox.getSelectionPaths();
        if (paths == null || paths.length == 0) return;
        Object c;
        TreeImageDisplay node, child;
        //List
        List<TreeImageDisplay> toRemove = new ArrayList<TreeImageDisplay>();
        for (int i = 0; i < paths.length; i++) {
            c = paths[i].getLastPathComponent();
            if (c instanceof TreeImageDisplay) {
                node = (TreeImageDisplay) c;
                if (node.hasChildrenDisplay()) { //tagset
                    List l = node.getChildrenDisplay();
                    Iterator j = l.iterator();
                    while (j.hasNext()) {
                        child = (TreeImageDisplay) j.next();
                        if (!isSelected(child)) {
                            selectedItems.add(child);
                            children.add(child);
                        }
                    }
                } else {
                    if (!isSelected(node)) {
                        toRemove.add(node);
                        selectedItems.add(node);
                        TreeImageDisplay parent = node.getParentDisplay();
                        if (parent != null &&
                                parent.getUserObject() instanceof DataObject) {
                            children.add(node);
                        }
                    }
                }
            }
        }
        availableItems.removeAll(toRemove);
        sortLists();
        populateTreeItems(availableItemsListbox, availableItems);
        populateTreeItems(selectedItemsListbox, selectedItems);
    }

    /** Notifies that the selection has changed. */
    private void setSelectionChange()
    {
        /*
        boolean b = false;
        if (originalSelectedItems.size() != selectedItems.size()) {
            b = true;
        } else {
            int n = 0;
            Iterator<Object> i = selectedItems.iterator();
            while (i.hasNext()) {
                if (originalSelectedItems.contains(i.next())) n++;
            }
            b = (n != originalSelectedItems.size());
        }
        firePropertyChange(SELECTION_CHANGE, Boolean.valueOf(!b),
                Boolean.valueOf(b));
                */
    }

    /**
     * Updates the specified tree.
     *
     * @param tree The tree to update.
     * @param nodes The collection of nodes to handle.
     */
    private void populateTreeItems(JTree tree, List<TreeImageDisplay> nodes)
    {
        DefaultTreeModel dtm = (DefaultTreeModel) tree.getModel();
        TreeImageDisplay parent = (TreeImageDisplay) dtm.getRoot();
        parent.removeAllChildrenDisplay();
        parent.removeAllChildren();
        Iterator<TreeImageDisplay> i = nodes.iterator();
        TreeImageDisplay node, child;
        Iterator<TreeImageDisplay> j;
        while (i.hasNext()) {
            node = i.next();
            node.setDisplayItems(false);
            dtm.insertNodeInto(node, parent, parent.getChildCount());
            if (node.hasChildrenDisplay()) {
                node.removeAllChildren();
                tree.expandPath(new TreePath(node.getPath()));
                Collection<TreeImageDisplay> l = node.getChildrenDisplay();
                j = l.iterator();
                while (j.hasNext()) {
                    child = j.next();
                    child.setDisplayItems(false);
                    if (!children.contains(child)) {
                        dtm.insertNodeInto(child, node, node.getChildCount());
                    }
                }
            }
        }
        dtm.reload();
    }

    /** Sorts the lists. */
    private void sortLists()
    {
        if (availableItems != null) 
            availableItems = sorter.sort(availableItems);
        if (selectedItems != null) selectedItems = sorter.sort(selectedItems);
    }

    /** Builds and lays out the UI. */
    private void buildGUI()
    {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        double[][] size = {{TableLayout.FILL, 40, TableLayout.FILL},
                {TableLayout.FILL}};
        setLayout(new TableLayout(size));
        add(createAvailableItemsPane(), "0, 0");
        add(createSelectionPane(), "1, 0, CENTER, CENTER");
        add(createSelectedItemsPane(), "2, 0");
    }

    /**
     * Builds and lays out the available tags.
     *
     * @return See above.
     */
    private JPanel createAvailableItemsPane()
    {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(UIUtilities.setTextFont(createText("Available")));
        panel.add(filterArea);
        panel.add(Box.createVerticalStrut(2));
        p.add(panel, BorderLayout.NORTH);
        p.add(new JScrollPane(availableItemsListbox), BorderLayout.CENTER);
        populateTreeItems(availableItemsListbox, availableItems);
        return p;
    }

    /**
     * Builds and lays out the buttons used to select tags.
     *
     * @return See above.
     */
    private JPanel createSelectionPane()
    {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(Box.createVerticalStrut(30));
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(addAllButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(removeAllButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        return buttonPanel;
    }

    /**
     * Builds and lays out the list of selected tags.
     *
     * @return See above.
     */
    private JPanel createSelectedItemsPane()
    {
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(UIUtilities.setTextFont(createText("Selected")),
                BorderLayout.NORTH);
        p.add(new JScrollPane(selectedItemsListbox), BorderLayout.CENTER);
        populateTreeItems(selectedItemsListbox, selectedItems);
        return p;
    }

    /**
     * Creates the text displayed above the selections.
     *
     * @param txt The text to display.
     * @return See above.
     */
    private String createText(String txt)
    {
        StringBuilder b = new StringBuilder();
        b.append(txt);
        if (TagAnnotationData.class.equals(type)) {
            b.append(" tags");
        } else if (FileAnnotationData.class.equals(type)) {
            b.append(" attachments");
        }
        b.append(":");
        return b.toString();
    }
    /**
     * Creates a new instance. 
     * 
     * @param available The collection of available items.
     * @param type The type of object to handle.
     * @param user The current user.
     */
    public SelectionWizardUI(Collection<Object> available, Class type,
            ExperimenterData user)
    {
        this(available, null, type, user);
    }

    /**
     * Creates a new instance. 
     *
     * @param available The collection of available items.
     * @param selected The collection of selected items.
     * @param type The type of object to handle.
     * @param user The current user.
     */
    public SelectionWizardUI(Collection<Object> available,
            Collection<Object> selected, Class type, ExperimenterData user)
    {
        if (selected == null) selected = new ArrayList<Object>();
        if (available == null) available = new ArrayList<Object>();
        children = new HashSet<TreeImageDisplay>();
        this.availableItems = new ArrayList<TreeImageDisplay>(
                TreeViewerTranslator.transformHierarchy(available));
        this.selectedItems = new ArrayList<TreeImageDisplay>(
                TreeViewerTranslator.transformHierarchy(selected));
        this.type = type;
        createOriginalSelections();
        initComponents(user);
        sortLists();
        buildGUI();
    }

    /** Resets the selection. */
    void reset()
    {
        availableItems.clear();
        selectedItems.clear();
        for (TreeImageDisplay item : originalItems)
            availableItems.add(item);
        for (TreeImageDisplay item : originalSelectedItems)
            selectedItems.add(item);

        populateTreeItems(availableItemsListbox, availableItems);
        populateTreeItems(selectedItemsListbox, selectedItems);
        setSelectionChange();
    }

    /**
     * Adds the passed objects.
     * 
     * @param toAdd The objects to add.
     */
    void addObjects(List<DataObject> toAdd)
    {
        if (CollectionUtils.isEmpty(toAdd)) return;
        Iterator<DataObject> i = toAdd.iterator();
        DataObject data;
        while (i.hasNext()) {
            data = i.next();
            if (!doesObjectExist(data)) {
                selectedItems.add(TreeViewerTranslator.transformDataObject
                        (data));
            }
        }
        sortLists();
        populateTreeItems(selectedItemsListbox, selectedItems);
        setSelectionChange();
    }

    /**
     * Sets the collection of nodes that cannot be removed.
     *
     * @param immutable The collection to set.
     */
    void setImmutableElements(Collection immutable)
    {
        if (immutable == null) immutable = new ArrayList();
        this.immutable = immutable;
    }

    /**
     * Returns the collection of immutable objects.
     * 
     * @return See above.
     */
    Collection getImmutableElements() { return immutable; }

    /**
     * Returns <code>true</code> if the node has been added,
     * <code>false</code> otherwise.
     *
     * @param value The value to handle.
     * @return See above.
     */
    boolean isAddedNode(Object value)
    {
        return !originalSelectedItems.contains(value);
    }

    /**
     * Returns the name of the group corresponding to identifier.
     *
     * @param ctx The context to handle.
     * @return See above
     */
    String getGroupName(long groupId)
    {
        if (groups == null) return null;
        Iterator<GroupData> i = groups.iterator();
        GroupData g;
        while (i.hasNext()) {
            g = i.next();
            if (g.getId() == groupId)
                return g.getName();
        }
        return null;
    }

    /**
     * Sets the groups.
     *
     * @param groups The groups to set.
     */
    void setGroups(Collection<GroupData> groups)
    {
        this.groups = groups;
    }

    /**
     * Returns <code>true</code> to filter by term anywhere in the word,
     * <code>false</code> otherwise.
     * @return
     */
    boolean isFilterAnywhere() { return filterAnywhere; }

    /**
     * Sets to <code>true</code> to filter by term anywhere in the word,
     * <code>false</code> otherwise.
     *
     * @param filterAnywhere The value to set.
     */
    void setFilterAnywhere(boolean filterAnywhere)
    { 
        this.filterAnywhere = filterAnywhere;
    }

    /**
     * Returns the selected items, excluding the immutable node.
     *
     * @return See above.
     */
    public Collection<Object> getSelection()
    { 
        Iterator<TreeImageDisplay> i = selectedItems.iterator();
        List<Object> results = new ArrayList<Object>();
        Object object;
        while (i.hasNext()) {
            object = i.next();
            if (isAddedNode(object))
                results.add(object);
            else {
                //was there but is immutable
                if (object instanceof DataObject) {
                    if (!isImmutable((DataObject) object))
                        results.add(object);
                } else results.add(object);
            }
        }
        return results;
    }

    /**
     * Reacts to event fired by the various controls.
     * @see ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent evt)
    {
        int id = Integer.parseInt(evt.getActionCommand());
        switch (id) {
        case ADD:
            addItem();
            break;
        case ADD_ALL:
            addAllItems();
            break;
        case REMOVE:
            removeItem();
            break;
        case REMOVE_ALL:
            removeAllItems();
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) { filter(false); }

    @Override
    public void insertUpdate(DocumentEvent e) { filter(true); }

    @Override
    public void changedUpdate(DocumentEvent e) {}

}
