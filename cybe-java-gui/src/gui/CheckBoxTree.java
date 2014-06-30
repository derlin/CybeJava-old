package gui;

/**
 * soruce: http://stackoverflow.com/questions/21847411/java-swing-need-a-good-quality-developed-jtree-with-checkboxes
 * @author: Lucy Linder
 * @date: 30.06.2014
 */

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;

public class CheckBoxTree extends JTree{

    private static final long serialVersionUID = -4194122328392241790L;

    CheckBoxTree selfPointer = this;


    public static void main( String[] args ){
        JFrame frame = new JFrame();
        frame.setSize( 500, 500 );
        frame.getContentPane().setLayout( new BorderLayout() );
        final CheckBoxTree cbt = new CheckBoxTree();
        frame.getContentPane().add( cbt );
        cbt.addCheckChangeEventListener( ( event ) -> {
            System.out.println( "event" );
            TreePath[] paths = cbt.getCheckedPaths();
            for( TreePath tp : paths ){
                for( Object pathPart : tp.getPath() ){
                    System.out.print( pathPart + "," );
                }
                System.out.println();
            }

        } );
        frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
        frame.setVisible( true );
    }//end main


    /* *****************************************************************
     * CheckedNode class
     * ****************************************************************/

     // Defining data structure that will enable to fast check-indicate the state of each node
    // It totally replaces the "selection" mechanism of the JTree
    private class CheckedNode{
        boolean isSelected;
        boolean hasChildren;
        boolean allChildrenSelected;



        public CheckedNode( boolean isSelected_, boolean hasChildren_, boolean allChildrenSelected_ ){
            isSelected = isSelected_;
            hasChildren = hasChildren_;
            allChildrenSelected = allChildrenSelected_;
        }
    }

    HashMap<TreePath, CheckedNode> nodesCheckingState;
    HashSet<TreePath> checkedPaths = new HashSet<>();


    /* *****************************************************************
     * Listeners handling
     * ****************************************************************/

     // Defining a new event type for the checking mechanism and preparing event-handling mechanism
    protected EventListenerList listenerList = new EventListenerList();

    public class CheckChangeEvent extends EventObject{
        private static final long serialVersionUID = -8100230309044193368L;


        public CheckChangeEvent( Object source ){
            super( source );
        }
    }

    @FunctionalInterface
    public interface CheckChangeEventListener extends EventListener{
        public void checkStateChanged( CheckChangeEvent event );
    }


    public void addCheckChangeEventListener( CheckChangeEventListener listener ){
        listenerList.add( CheckChangeEventListener.class, listener );
    }


    public void removeCheckChangeEventListener( CheckChangeEventListener listener ){
        listenerList.remove( CheckChangeEventListener.class, listener );
    }


    void fireCheckChangeEvent( CheckChangeEvent evt ){
        Object[] listeners = listenerList.getListenerList();
        for( int i = 0; i < listeners.length; i++ ){
            if( listeners[ i ] == CheckChangeEventListener.class ){
                ( ( CheckChangeEventListener ) listeners[ i + 1 ] ).checkStateChanged( evt );
            }
        }
    }


    // Override
    public void setModel( TreeModel newModel ){
        super.setModel( newModel );
        resetCheckingState();
    }


    // New method that returns only the checked paths (totally ignores original "selection" mechanism)
    public TreePath[] getCheckedPaths(){
        return checkedPaths.toArray( new TreePath[ checkedPaths.size() ] );
    }


    // Returns true in case that the node is selected, has children but not all of them are selected
    public boolean isSelectedPartially( TreePath path ){
        CheckedNode cn = nodesCheckingState.get( path );
        return cn.isSelected && cn.hasChildren && !cn.allChildrenSelected;
    }


    private void resetCheckingState(){
        nodesCheckingState = new HashMap<>();
        checkedPaths = new HashSet<>();
        DefaultMutableTreeNode node = ( DefaultMutableTreeNode ) getModel().getRoot();
        if( node == null ){
            return;
        }
        addSubtreeToCheckingStateTracking( node );
    }


    // Creating data structure of the current model for the checking mechanism
    private void addSubtreeToCheckingStateTracking( DefaultMutableTreeNode node ){
        TreeNode[] path = node.getPath();
        TreePath tp = new TreePath( path );
        CheckedNode cn = new CheckedNode( false, node.getChildCount() > 0, false );
        nodesCheckingState.put( tp, cn );
        for( int i = 0; i < node.getChildCount(); i++ ){
            addSubtreeToCheckingStateTracking( ( DefaultMutableTreeNode ) tp.pathByAddingChild( node.getChildAt( i )
            ).getLastPathComponent() );
        }
    }


    /* *****************************************************************
     * renderer class
     * ****************************************************************/

     // Overriding cell renderer by a class that ignores the original "selection" mechanism
    // It decides how to show the nodes due to the checking-mechanism
    private class CheckBoxCellRenderer extends JPanel implements TreeCellRenderer{
        private static final long serialVersionUID = -7341833835878991719L;
        JCheckBox checkBox;
        private TreeCellRenderer delegate;


        public CheckBoxCellRenderer( TreeCellRenderer delegate ){
            super();
            this.setLayout( new BorderLayout() );
            checkBox = new JCheckBox();
            checkBox.addActionListener( ( e ) -> System.out.println( "Checkbox " + checkBox.isSelected() ) );
            this.delegate = delegate;
            add( checkBox, BorderLayout.WEST );
            setOpaque( false );
        }


        @Override
        public Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, boolean expanded,
                                                       boolean leaf, int row, boolean hasFocus ){
            hasFocus = false;
            selected = false;
            Component renderer = delegate.getTreeCellRendererComponent( tree, value, selected, expanded, leaf, row,
                    hasFocus );
            DefaultMutableTreeNode node = ( DefaultMutableTreeNode ) value;
            Object obj = node.getUserObject();
            TreePath tp = new TreePath( node.getPath() );
            CheckedNode cn = nodesCheckingState.get( tp );
            if( cn == null ){
                return this;
            }
            checkBox.setSelected( cn.isSelected );

            checkBox.setOpaque( cn.isSelected && cn.hasChildren && !cn.allChildrenSelected );
            removeAll();
            add( checkBox, BorderLayout.WEST );
            //checkBox.setVisible( cnt != 0 );
            add( renderer, BorderLayout.CENTER );
            if( obj.toString().equals( currentSelection ) ){
                renderer.setForeground( Color.red );
            }else{
                renderer.setBackground( Color.BLACK );
            }
            return this;
        }
    }

    private String currentSelection = "";
    private int cnt = 0;


    public CheckBoxTree(){
        super();
        // Disabling toggling by double-click
        this.setToggleClickCount( 0 );
        // Overriding cell renderer by new one defined above
        CheckBoxCellRenderer cellRenderer = new CheckBoxCellRenderer( super.getCellRenderer() );
        this.setCellRenderer( cellRenderer );

        // Overriding selection model by an empty one
        DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel(){
            private static final long serialVersionUID = -8190634240451667286L;


            // Totally disabling the selection mechanism
            public void setSelectionPath( TreePath path ){
            }


            public void addSelectionPath( TreePath path ){
            }


            public void removeSelectionPath( TreePath path ){
            }


            public void setSelectionPaths( TreePath[] pPaths ){
            }
        };
        final int hotspot = new JCheckBox().getPreferredSize().width;
        // Calling checking mechanism on mouse click
        this.addMouseListener( new MouseAdapter(){
            public void mouseClicked( MouseEvent evt ){
                TreePath treePath = selfPointer.getPathForLocation( evt.getX(), evt.getY() );
                if( treePath == null ) return;

                if( evt.getX() > getPathBounds( treePath ).x + hotspot ){
                    String value = treePath.getLastPathComponent().toString();
                    currentSelection = currentSelection.equals( value ) ? "" : value;
                    cnt = ( cnt + 1 ) % 3;
                    selfPointer.repaint();
                    System.out.println( "*** " + treePath.getLastPathComponent() + " is selected" );

                }else{
                    boolean checkMode = !nodesCheckingState.get( treePath ).isSelected;
                    checkSubTree( treePath, checkMode );
                    updatePredecessorsWithCheckMode( treePath, checkMode );
                    // Firing the check change event
                    fireCheckChangeEvent( new CheckChangeEvent( new Object() ) );
                }

                // Repainting tree after the data structures were updated
                selfPointer.repaint();
            }

        } );
        //this.setSelectionModel( dtsm );
    }


    // When a node is checked/unchecked, updating the states of the predecessors
    protected void updatePredecessorsWithCheckMode( TreePath tp, boolean check ){
        TreePath parentPath = tp.getParentPath();
        // If it is the root, stop the recursive calls and return
        if( parentPath == null ){
            return;
        }
        CheckedNode parentCheckedNode = nodesCheckingState.get( parentPath );
        DefaultMutableTreeNode parentNode = ( DefaultMutableTreeNode ) parentPath.getLastPathComponent();
        parentCheckedNode.allChildrenSelected = true;
        parentCheckedNode.isSelected = false;
        for( int i = 0; i < parentNode.getChildCount(); i++ ){
            TreePath childPath = parentPath.pathByAddingChild( parentNode.getChildAt( i ) );
            CheckedNode childCheckedNode = nodesCheckingState.get( childPath );
            // It is enough that even one subtree is not fully selected
            // to determine that the parent is not fully selected
            if( !childCheckedNode.allChildrenSelected ){
                parentCheckedNode.allChildrenSelected = false;
            }
            // If at least one child is selected, selecting also the parent
            if( childCheckedNode.isSelected ){
                parentCheckedNode.isSelected = true;
            }
        }
        if( parentCheckedNode.isSelected ){
            checkedPaths.add( parentPath );
        }else{
            checkedPaths.remove( parentPath );
        }
        // Go to upper predecessor
        updatePredecessorsWithCheckMode( parentPath, check );
    }


    // Recursively checks/unchecks a subtree
    protected void checkSubTree( TreePath tp, boolean check ){
        CheckedNode cn = nodesCheckingState.get( tp );
        cn.isSelected = check;
        DefaultMutableTreeNode node = ( DefaultMutableTreeNode ) tp.getLastPathComponent();
        for( int i = 0; i < node.getChildCount(); i++ ){
            checkSubTree( tp.pathByAddingChild( node.getChildAt( i ) ), check );
        }
        cn.allChildrenSelected = check;
        if( check ){
            checkedPaths.add( tp );
        }else{
            checkedPaths.remove( tp );
        }
    }

}
