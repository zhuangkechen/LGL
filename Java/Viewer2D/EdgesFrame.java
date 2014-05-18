//  
//  Copyright (c) 2003 Alex Adai, All Rights Reserved.
//  
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License as
//  published by the Free Software Foundation; either version 2 of
//  the License, or (at your option) any later version.
//  
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
//  MA 02111-1307 USA
//  
package Viewer2D;

import javax.swing.*; 
import javax.swing.JOptionPane.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import Jama.Matrix.*;
import java.io.*;
import java.util.*;
import Jama.*;
import javax.swing.filechooser.*;

public class EdgesFrame extends JFrame
{
    private Edge[] edges; 
    private Vertex[] vertices;

    private EdgesPanel panel;  // Has the edges drawn
    private JTextField statusBar;  // Shows highlighted/user info

    private String statusMessage;

    private ViewerIO edgesio;
    private File edgesFile;
    private File coordsFile;
    private File edgeColorFile;
    private File vertexColorFile;

    // These are hashes relating edge/vertex id
    // to specific info
    private HashMap vertexIdMap;
    private HashMap edgeIdMap;

    private int threads;

    private Container container;
    private GridBagLayout layout;
    private GridBagConstraints constraints;

    private FormatVertex formatter;    
    private int[] windowSizes;

    private boolean idRegion;

    private double moveStepSize;

    private double zoomStepSize;

    // Related to the showing ids
    private JRadioButtonMenuItem showFonts , showIdsHighlighted, 
	zoomHighlighted , zoomRegion , showVertices;

    private Font font;
    
    private int vertexRadius;

    private JRadioButtonMenuItem[] idStyles;

    private JRadioButton blockers;

    private Color fontColor, vertexColor , 
	edgeColor, backgroundColor;

    private int STATUSBAR_X;
    private int STATUSBAR_Y;

    public EdgesFrame( String title , int x , int y )
    {
	super( title );

	threads = 1;

	STATUSBAR_X = x;
	STATUSBAR_Y = 50;

	moveStepSize = .2;
	zoomStepSize = .2;

      	windowSizes = new int[2];
	windowSizes[0] = x; windowSizes[1] = y;

	font = new Font( "Helvetica" , Font.BOLD , 13 );

	vertexRadius = 1;
	vertexColor = Color.red;
	fontColor = Color.blue;
	edgeColor = Color.black;
	backgroundColor = Color.white;

	idRegion = false;

	container = getContentPane();
	layout = new GridBagLayout();
	container.setLayout( layout );
	constraints = new GridBagConstraints();

	setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	setResizable( false );

	panel = new EdgesPanel( edges , vertices , windowSizes[0] , 
				windowSizes[1] );
	panel.setBackground( backgroundColor );

	setMenuBars();
	setButtons();

 	JScrollPane scrollPane = new JScrollPane( panel );
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	constraints.fill = GridBagConstraints.BOTH;
	layout.setConstraints( scrollPane , constraints );
 	container.add( scrollPane );

	statusBar = new JTextField( statusMessage , STATUSBAR_Y );
	statusBar.setEditable( false );
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	constraints.fill = GridBagConstraints.BOTH;
	layout.setConstraints( statusBar , constraints );
	container.add( statusBar );
	panel.setStatusBar( statusBar );

	setSize( x , y + STATUSBAR_Y );
	//	offsets = getInsets();

	pack();

	show();
    }

    private void setMenuBars()
    {
	JMenuBar bar = new JMenuBar();
	setJMenuBar( bar );

	//-----------------------------------------------------
	// FILE ISSUES
	//-----------------------------------------------------

	JMenu fileMenu = new JMenu("File");
	fileMenu.setMnemonic( 'F' );

	// LOAD THE EDGES FILE ( SOMETHING.lgl )
 	JMenuItem openFile = new JMenuItem("Open .lgl file");
	openFile.setMnemonic( 'O' );
 	openFile.addActionListener( new ActionListener() {
 		public void actionPerformed( ActionEvent e ) {
 		    JFileChooser chooser = new JFileChooser();
		    chooser.setFileFilter( new FileSuffixFilter( "lgl" ) );
 		    int returnVal = chooser.showOpenDialog( EdgesFrame.this );
 		    if ( returnVal == JFileChooser.APPROVE_OPTION ) {
 			edgesFile = chooser.getSelectedFile();
 			loadSHORTFile( edgesFile );
 		    } else {
 			// Cancelled operation
 		    }
 		}
 	    } );
 	fileMenu.add( openFile );

	// LOAD THE 2D COORDS
	JMenuItem cFile = new JMenuItem("Open 2D Coords file");
	cFile.setMnemonic( 'C' );
	cFile.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( edgesCheck() ) {
			JFileChooser chooser = new JFileChooser();
			int returnVal = chooser.showOpenDialog( EdgesFrame.this );
			if ( returnVal == JFileChooser.APPROVE_OPTION ) {
			    coordsFile = chooser.getSelectedFile();
			    loadCoordsFile( coordsFile );
			} else {
			    // Cancelled operation
			}
		    }
		}
	    } );
	fileMenu.add( cFile );
	
	// LOAD VERTEX DESCRIPTIONS
	JMenuItem openVertexDescripFile = new JMenuItem("Load Vertex Information");
	openVertexDescripFile.setMnemonic( 'D' );
	openVertexDescripFile.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{		    
		    if ( vertexCheck() )
			{
			    JFileChooser chooser = new JFileChooser();
			    int returnVal = chooser.showOpenDialog( EdgesFrame.this );
			    if ( returnVal == JFileChooser.APPROVE_OPTION ) {
				loadVertexDescriptions( chooser.getSelectedFile() );
			    } else {
				// Operation Cancelled
			    }
			}
		}
	    }
						 ); 
	fileMenu.add( openVertexDescripFile );
	
	// OPEN EDGE COLOR FILE
	JMenuItem edgeColorMenu = new JMenuItem("Open Edge Color File");
	edgeColorMenu.setMnemonic( 'O' );
	edgeColorMenu.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( vertexCheck() ) 
			{
			    JFileChooser chooser = new JFileChooser();
			    int returnVal = chooser.showOpenDialog( EdgesFrame.this );
			    if ( returnVal == JFileChooser.APPROVE_OPTION ) {
				loadEdgeColorFile( chooser.getSelectedFile() );
			    } else {
				// Operation Cancelled
			    }
			}
		}
	    } );
	fileMenu.add( edgeColorMenu );
	
	// SAVE THE CURRENT EDGE COLORS
	JMenuItem saveEdgeColorMenu = new JMenuItem("Save Edge Colors");
	saveEdgeColorMenu.setMnemonic( 'S' );
	saveEdgeColorMenu.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( vertexCheck() ) 
			{
			    JFileChooser chooser = new JFileChooser();
			    int returnVal = chooser.showSaveDialog( EdgesFrame.this );
			    if ( returnVal == JFileChooser.APPROVE_OPTION ) {
				panel.saveEdgeColorMap( chooser.getSelectedFile() );
			    } else {
				// Operation Cancelled
			    }
			}
		}
	    } );
	fileMenu.add( saveEdgeColorMenu );

	// OPEN VERTEX COLOR FILE
	JMenuItem vertexColorMenu = new JMenuItem("Open Vertex Color File");
	vertexColorMenu.setMnemonic( 'O' );
	vertexColorMenu.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( vertexCheck() ) 
			{
			    JFileChooser chooser = new JFileChooser();
			    int returnVal = chooser.showOpenDialog( EdgesFrame.this );
			    if ( returnVal == JFileChooser.APPROVE_OPTION ) {
				loadVertexColorFile( chooser.getSelectedFile() );
			    } else {
				// Operation Cancelled
			    }
			}
		}
	    } );
	fileMenu.add( vertexColorMenu );
	
	// SAVE THE CURRENT VERTEX COLORS
	JMenuItem saveVertexColorMenu = new JMenuItem("Save Vertex Colors");
	saveVertexColorMenu.setMnemonic( 'S' );
	saveVertexColorMenu.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( vertexCheck() ) 
			{
			    JFileChooser chooser = new JFileChooser();
			    int returnVal = chooser.showSaveDialog( EdgesFrame.this );
			    if ( returnVal == JFileChooser.APPROVE_OPTION ) {
				panel.saveVertexColorMap( chooser.getSelectedFile() );
			    } else {
				// Operation Cancelled
			    }
			}
		}
	    } );
	fileMenu.add( saveVertexColorMenu );

	
	// RELOAD THE ORIGINAL FILE
	JMenuItem reload = new JMenuItem("Reload 2D Coords File");
	reload.setMnemonic( 'R' );
	reload.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( edgesCheck() && coordsFile != null ) {
			loadCoordsFile( coordsFile );
		    }
		}
	    } );
	fileMenu.add( reload );
	
	// APPLICATION EXIT
	JMenuItem exit = new JMenuItem("Exit");
	exit.setMnemonic( 'x' );
	exit.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    System.exit( 0 );
		}
	    } );
	fileMenu.add( exit );
	
	bar.add( fileMenu );

	//-----------------------------------------------------
	// EDITING PREFERENCES
	//-----------------------------------------------------

	JMenu edit = new JMenu("Edit");
	edit.setMnemonic('E');

	// CHANGE THE STEP SIZE OF THE MOVE
	JMenuItem movesize = new JMenuItem("Change Move Step Size");
	movesize.setMnemonic( 'M' );
	movesize.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    String newSize = 
			JOptionPane.showInputDialog( EdgesFrame.this , 
						     "Enter the new Move step size (0,1)" , 
						     Double.toString(moveStepSize) );
		    if ( newSize != null ) {
			double newStepSize = Double.parseDouble(newSize);
			if ( newStepSize <= 0 || newStepSize >= 1 ) {
			    JOptionPane.showMessageDialog( null , "Illegal Value" ,
							   "Error" , JOptionPane.ERROR_MESSAGE );
			} else {
			    moveStepSize = newStepSize;
			    panel.setMoveStepSize( newStepSize );
			}
		    }
		}
	    } );
	edit.add( movesize );

	// CHANGE THE STEP SIZE OF ZOOMING
	JMenuItem zoomsize = new JMenuItem("Change Zoom Step Size");
	zoomsize.setMnemonic( 'Z' );
	zoomsize.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    String newSize = 
			JOptionPane.showInputDialog( EdgesFrame.this , 
						     "Enter the new Zoom step size (0,1)" , 
						     Double.toString(zoomStepSize) );
		    if ( newSize != null ) {
			double newZoomStepSize = Double.parseDouble(newSize);
			if ( newZoomStepSize <= 0 || newZoomStepSize >= 1 ) {
			    JOptionPane.showMessageDialog( null , "Illegal Value" ,
							   "Error" , JOptionPane.ERROR_MESSAGE );
			} else {
			    zoomStepSize = newZoomStepSize;
			    panel.setZoomStepSize( zoomStepSize );
			}
		    }
		}
	    } );
	edit.add( zoomsize );

	// BUTTON TO REMOVE TRANSIENT EDGES
	blockers = new JRadioButton("Remove Transient Edges");
	blockers.setMnemonic( 'R' );
	blockers.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( blockers.isSelected() ) {
			panel.setVisibilityTest( true );
			statusBar.setText("Removing Transient Edges");
		    } else {
			panel.setVisibilityTest( false );
			statusBar.setText("Showing Transient Edges");
		    }
		}
	    } );
	blockers.setSelected( true );
	panel.setVisibilityTest( true ); 
	edit.add( blockers );

	bar.add( edit );

	//-----------------------------------------------------
	// HIGHLIGHT EVENTS
	//-----------------------------------------------------
	
	JMenu highlight = new JMenu( "Highlight" );
	highlight.setMnemonic( 'H' );

	// CLICK TO ZOOM
	zoomHighlighted = new JRadioButtonMenuItem( "Zoom Point" );
	zoomHighlighted.setMnemonic( 'Z' );
	zoomHighlighted.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( zoomHighlighted.isSelected() ) {
			panel.prepZoomPoint( true );
			statusMessage = "Enabled Zoom Point";
		    } else {
			panel.prepZoomPoint( false );
			statusMessage = "Disabled Zoom Point";
		    }
		    statusBar.setText( statusMessage );
		}
	    } );	
	zoomHighlighted.setSelected( false );
	highlight.add( zoomHighlighted );

	// HIGHLIGHTING A REGION TO ZOOM INTO
	zoomRegion = new JRadioButtonMenuItem( "Zoom Region" );
	zoomRegion.setMnemonic( 'R' );
	zoomRegion.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( zoomRegion.isSelected() ) {
			panel.prepZoomRegion( true );
			statusMessage = "Enabled Zoom Region";
		    } else {
			panel.prepZoomRegion( false );
			statusMessage = "Disabled Zoom Region";
		    }
		    statusBar.setText( statusMessage );
		}
	    } );	
	zoomRegion.setSelected( false );
	highlight.add( zoomRegion );

	// HIGHLIGHT TO SHOW IDS
	showIdsHighlighted = new JRadioButtonMenuItem( "ID Region" );
	showIdsHighlighted.setMnemonic( 'I' );
	showIdsHighlighted.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( panel.getVertices() != null ) {
			if ( showIdsHighlighted.isSelected() ) {
			    panel.prepIdRegion( true );
			    statusMessage = "Enabled ID Region";
			} else {
			    panel.prepIdRegion( false );
			    statusMessage = "Disabled ID Region";
			}
			statusBar.setText( statusMessage );
		    }
		}
	    }
					   );	
	showIdsHighlighted.setSelected( false );
	highlight.add( showIdsHighlighted );	

	bar.add( highlight );

	//-----------------------------------------------------
	// FORMAT ISSUES ( SHOWING IDS , COLORS ETC. )
	//-----------------------------------------------------

	JMenu formatMenu = new JMenu( "Format" );
	formatMenu.setMnemonic( 'F' );

	// FONT ISSUES
	JMenu fontIssues = new JMenu("Font");
	fontIssues.setMnemonic( 'F' );

	// CHANGING FONT COLOR
	JMenuItem idColor = new JMenuItem("Color");
	idColor.setMnemonic( 'C' );
	idColor.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    Color c = JColorChooser.showDialog( EdgesFrame.this,
							"Choose The Font Color", 
							fontColor );
		    if ( c != null ) {
			fontColor = c;
			panel.setFontColor( fontColor );
			repaint();
		    }
		}
	    } );
	fontIssues.add( idColor );

	// CHANGING ID SIZE
	JMenuItem idSize = new JMenuItem("Size");
	idSize.setMnemonic( 'S' );
	idSize.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {		    
 		    String newSize = 
			JOptionPane.showInputDialog( EdgesFrame.this , 
						     "Enter the new Font Size" , 
						     Float.toString( font.getSize2D() ) );
		    if ( newSize != null ) {
			float newFontSize = Float.parseFloat( newSize );
			if ( newFontSize <= 0.0 ) {
			    JOptionPane.showMessageDialog( null , "Illegal Value" ,
							   "Error" , JOptionPane.ERROR_MESSAGE );
			} else {
			    font = font.deriveFont( newFontSize );
			    panel.setFont( font );
			    repaint();
			}
		    }
		}
	    } );
	fontIssues.add( idSize );

	fontIssues.addSeparator();

	// CHANGING ID STYLE
	String styles[] = { "Bold" , "Italic" };
	idStyles = new JRadioButtonMenuItem[styles.length];
	StyleHandler stylehandler = new StyleHandler();
	for ( int ii=0; ii<styles.length; ++ii ) 
	    {
		idStyles[ii] = new JRadioButtonMenuItem( styles[ii] );
		fontIssues.add( idStyles[ii] );
		idStyles[ii].addItemListener( stylehandler );
	    }
	// Bold is on by default
	idStyles[0].setSelected( true );

	formatMenu.add( fontIssues );

	// VERTEX ISSUES
	JMenu vertexIssues = new JMenu( "Vertices" );
	vertexIssues.setMnemonic( 'V' );

	// CLEAR ALL VERTEX COLORS
	JMenuItem vertexClear = new JMenuItem("Clear Vertex Colors");
	vertexClear.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {	
		    panel.clearAllVertexColors();
		    edgesio.clearAllVertexColors();
		    repaint();
		}
	    } );
	vertexIssues.add( vertexClear );

	// CHANGING THE VERTEX COLOR
	JMenuItem vertexC = new JMenuItem("Color");
	vertexC.setMnemonic( 'C' );
	vertexC.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {		    
		    Color c = JColorChooser.showDialog( EdgesFrame.this,
							"Choose The Vertex Color", 
							vertexColor );
		    if ( c != null ) {
			vertexColor = c;
			panel.setVertexColor( vertexColor );		    
			repaint();
		    }  
		}
	    } );
	vertexIssues.add( vertexC );
	formatMenu.add( vertexIssues );

	// CHANGING THE VERTEX SIZE
	JMenuItem vertexS = new JMenuItem("Size");
	vertexS.setMnemonic( 'S' );
	vertexS.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {		    
		    String newSize = 	
			JOptionPane.showInputDialog( EdgesFrame.this , 
						     "Enter the new Vertex Radius in Pixels" , 
						     "" + vertexRadius );
		    if ( newSize != null ) {
			int newVSize = Integer.parseInt( newSize );
			if ( newVSize <= 0 ) {
			    JOptionPane.showMessageDialog( null , "Illegal Value" ,
							   "Error" , JOptionPane.ERROR_MESSAGE );
			} else {
			    vertexRadius = newVSize;
			    panel.setVertexRadius( vertexRadius );
			    repaint();
			}
		    }
		}
	    } );
	vertexIssues.add( vertexS );
	formatMenu.add( vertexIssues );

	// EDGE ISSUES
	JMenu edgeIssues = new JMenu( "Edges" );
	edgeIssues.setMnemonic( 'E' );

	// CLEAR ALL EDGE COLORS
	JMenuItem edgeClear = new JMenuItem("Clear Edge Colors");
	edgeClear.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {	
		    panel.clearAllEdgeColors();
		    edgesio.clearAllEdgeColors();
		    repaint();
		}
	    } );
	edgeIssues.add( edgeClear );

	// CHANGING EDGE COLOR
	JMenuItem edgeC = new JMenuItem("Color");
	edgeC.setMnemonic( 'C' );
	edgeC.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {		    
		    Color c = JColorChooser.showDialog( EdgesFrame.this,
							"Choose The Edge Color", 
							edgeColor );
		    if ( c != null ) {
			edgeColor = c;
			panel.setEdgeColor( edgeColor );		    
			repaint();
		    }  
		}
	    } );
	edgeIssues.add( edgeC );
	formatMenu.add( edgeIssues );

	// CHANGING BACKGROUND COLOR
	JMenuItem backgroundC = new JMenuItem("Background Color");
	backgroundC.setMnemonic( 'B' );
	backgroundC.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {		    
		    Color c  = JColorChooser.showDialog( EdgesFrame.this,
							 "Choose The Background Color", 
							 backgroundColor );
		    if ( c != null ) {
			backgroundColor = c;
			panel.setBackgroundColor( backgroundColor );
			repaint();
		    }
		}
	    } );
	formatMenu.add( backgroundC );

	bar.add( formatMenu );

	//-----------------------------------------------------
	// FINDING SPECIFIC VERTICES
	//-----------------------------------------------------

	JMenu find = new JMenu("Find");
	find.setMnemonic( 'F' );

	// FIND VERTICES
	JMenuItem findVertex = new JMenuItem("Find Vertices");
	findVertex.setMnemonic( 'V' );
	findVertex.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    FindVertexFrame f = new FindVertexFrame( "Find Vertices" , 
							     panel );
		}
	    } );
	find.add( findVertex );

	// FIND EDGES
	JMenuItem findEdges = new JMenuItem("Find Edges");
	findEdges.setMnemonic( 'E' );
	findEdges.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    FindEdgesFrame f = new FindEdgesFrame( "Find Edges" , 
							   panel );
		}
	    } );
	find.add( findEdges );

	bar.add( find );

    }

    private void loadVertexDescriptions( File f )
    {
	boolean saidAlready = false;
	System.out.print("Loading Vertex Descriptions...");
	HashMap vertexDescriptions = new HashMap();

	try {
	    FileInputHandler fileio = new FileInputHandler( f.getAbsolutePath() );
	    
	    while( fileio.readNextLine() )
		{
		    // The first entry is the vertex id, and the
		    // remaining is part of the description
		    String id = fileio.getToken(0);
		    Vertex v = (Vertex) vertexIdMap.get( id );
		    if ( v != null ) {
			String descrip = new String();
			for ( int ii=1; ii<fileio.getTokenCount(); ++ii )
			    {
				descrip += " " + fileio.getToken(ii);
			    }
			vertexDescriptions.put( v , descrip );
		    } else {
			if ( !saidAlready ) {
			    System.out.print("At least one id given has no matching vertex...");
			    saidAlready = true;
			}
		    }
		}

	    VertexDescripTable table = new VertexDescripTable( panel , vertexDescriptions );

	    System.out.println("Done.");

	} catch ( FileNotFoundException ee ) {
	    JOptionPane.showMessageDialog( null , "File Not Found" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	} catch ( IOException ee ) {
	    JOptionPane.showMessageDialog( null , "IO Error: Check File Format" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	}
    }

    public void paint( Graphics g )
    {
	super.paint( g );
    }

    public void update(Graphics g) {
	super.update( g );
	this.paint( g );
    }
    
    public void loadSHORTFile( File f )
    {
	edgesFile = f;
	try { 
	    edgesio = new ViewerIO( edgesFile );
	    edgesio.loadSHORTFile( f );
	    edges = edgesio.getEdges();
	} catch ( FileNotFoundException ee ) {
	    JOptionPane.showMessageDialog( null , "File Not Found" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	} catch ( IOException ee ) {
	    JOptionPane.showMessageDialog( null , "IO Error: Check File Format" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	}
    }

    public void loadEdgeColorFile( File f )
    {
	edgeColorFile = f;
	System.out.println("Loading Edge Color File " + f.getAbsolutePath() );
	try {
	    edgesio.loadEdgeColorFile( f );
	    // Update the panel with the new colors
	    panel.addEdgeColors( edgesio.getEdgeColorMap() );
	} catch ( FileNotFoundException ee ) {
	    JOptionPane.showMessageDialog( null , "File Not Found" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	} catch ( IOException ee ) {
	    JOptionPane.showMessageDialog( null , "IO Error: Check File Format" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	}
    }

    public void loadVertexColorFile( File f )
    {
	vertexColorFile = f;
	System.out.println("Loading Vertex Color File " + f.getAbsolutePath() );
	try {
	    edgesio.loadVertexColorFile( f );
	    // Update the panel with the new colors	    
	    panel.addVertexColors( edgesio.getVertexColorMap() );
	} catch ( FileNotFoundException ee ) {
	    JOptionPane.showMessageDialog( null , "File Not Found" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	} catch ( IOException ee ) {
	    JOptionPane.showMessageDialog( null , "IO Error: Check File Format" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	}
    }

    public void loadCoordsFile( File f )
    {
	coordsFile = f;
	try { 
	    edgesio.loadVertexCoords( f );
	    edges = edgesio.getEdges();
	    panel.setEdges( edges );
	    vertices = edgesio.getVertices();
	    panel.setVertices( vertices );
	    panel.setVertexRadius( vertexRadius );
	    panel.addEdgeColors( edgesio.getEdgeColorMap()  );
	    vertexIdMap = edgesio.getVertexIdMap();
	    panel.setVertexIdMap( vertexIdMap );
	    edgeIdMap = edgesio.getEdgeIdMap();
	    panel.setEdgeIdMap( edgeIdMap );
	    panel.setFontColor( fontColor );
	    panel.setEdgeColor( edgeColor );
	    panel.setVertexColor( vertexColor );
	    panel.setBackgroundColor( backgroundColor );
	    panel.setFont( font );
	    panel.setMoveStepSize( moveStepSize );
	    panel.setZoomStepSize( zoomStepSize );
	    formatter = new FormatVertex( vertices , edgesio.getStats() ,
					  windowSizes , threads ); 
	    panel.setFormatter( formatter );
	    panel.fitData();
	    repaint();
	} catch ( FileNotFoundException ee ) {
	    JOptionPane.showMessageDialog( null , "File Not Found" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	} catch ( IOException ee ) {
	    JOptionPane.showMessageDialog( null , "IO Error: Check File Format" ,
					   "Error" , JOptionPane.ERROR_MESSAGE );
	}
    }

    //-----------------------------------------------------
    // VIEW MENU. HANDLES ZOOMING MOVING ID'ING ETC
    //-----------------------------------------------------
    private void setButtons()
    {
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.weightx = 1.0;

	// UNDO
	JButton undo = new JButton("Undo");
	//undo.setMnemonic( 'U' );
	undo.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    panel.undo();
		    repaint();
		}
	    }
				    );
	buttonAdd( undo );

	// ZOOM IN
	JButton zoomIn = new JButton("In");
	//zoomIn.setMnemonic( 'I' );
	zoomIn.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    VertexFitter f = new VertexFitter();
		    panel.zoomIn( f );
		    panel.applyFit( f );
		    repaint();
		}
	    } );
	buttonAdd( zoomIn );

	// ZOOM OUT
	JButton zoomOut = new JButton("Out");
	//zoomOut.setMnemonic( 'O' );
	zoomOut.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    VertexFitter f = new VertexFitter();
		    panel.zoomOut( f );
		    panel.applyFit( f );
		    repaint();
		}
	    } );
	buttonAdd( zoomOut );

	// MOVE UP
	JButton moveup = new JButton("Up");
	//moveup.setMnemonic('U');
	moveup.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    panel.moveUp();
		    repaint();
		}
	    } );
	buttonAdd( moveup );

	// MOVE DOWN
	JButton movedown = new JButton("Down");
	//movedown.setMnemonic('D');
	movedown.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    panel.moveDown();
		    repaint();
		}
	    } );
	buttonAdd( movedown );

	// MOVE LEFT
	JButton moveleft = new JButton("Left");
	//moveleft.setMnemonic('L');
	moveleft.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    panel.moveLeft();
		    repaint();
		}
	    } );
	buttonAdd( moveleft );

	// MOVE RIGHT
	JButton moveright = new JButton("Right");
	//moveright.setMnemonic('R');
	moveright.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    panel.moveRight();
		    repaint();
		}
	    } );
	buttonAdd( moveright );

	JButton fit = new JButton("Fit");
	//fit.setMnemonic('F');
	fit.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    panel.fitData();
		    panel.fitData();
		    System.out.println("Fit data still buggy.");
		    repaint();
		}
	    } );
	buttonAdd( fit );

	constraints.gridwidth = GridBagConstraints.REMAINDER;

	JButton snap = new JButton("SnapShot");
	//snap.setMnemonic('S');
	snap.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    statusBar.setText("Preparing to write Image");
		    JFileChooser chooser = new JFileChooser();
		    chooser.setFileFilter( new FileSuffixFilter( "png" ) );
		    int returnVal = chooser.showSaveDialog( EdgesFrame.this );
		    if ( returnVal == JFileChooser.APPROVE_OPTION ) {
			panel.writeImage( chooser.getSelectedFile().getAbsolutePath() ,
					  null );
		    } else {
			// Cancelled operation
		    }
		}
	    } );
	buttonAdd( snap );

	constraints.gridwidth = 2;

	// SHOWING IDS
	showFonts = new JRadioButtonMenuItem( "Show All IDs" );
	//showFonts.setMnemonic( 'I' );
	showFonts.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( showFonts.isSelected() ) {
			panel.setFontColor( fontColor );
			panel.setFont( font );
			panel.showIds( true );
			repaint();
		    } else {
			panel.showIds( false );
			repaint();
		    }
		}
	    }
				     );	
	showFonts.setSelected( false );
	layout.setConstraints( showFonts , constraints );
	container.add( showFonts );

	constraints.gridwidth = GridBagConstraints.REMAINDER;

	showVertices = new JRadioButtonMenuItem( "Show All Vertices" );
	showVertices.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( showVertices.isSelected() ) {
			panel.showVertices( true );
			repaint();
		    } else {
			panel.showVertices( false );
			repaint();
		    }
		}
	    } );	
	panel.showVertices( false );
	showVertices.setSelected( false );
	layout.setConstraints( showVertices , constraints );
	container.add( showVertices );
	
    }

    private void buttonAdd( JButton b )
    {
	layout.setConstraints(b,constraints);
	container.add( b );
    }

    class StyleHandler implements ItemListener {
	public void itemStateChanged( ItemEvent e )
	{
	    int style = 0;
	    if ( idStyles[0].isSelected() ) {
		style += Font.BOLD;
	    }
	    if ( idStyles[1].isSelected() ) {
		style += Font.ITALIC;
	    }
	    font = font.deriveFont(style);
	    panel.setFont( font );
	    repaint();
	}
    }
    

    //////////////////////////////////////////////////////////////////
    // ERROR HANDLING
    //////////////////////////////////////////////////////////////////

    private boolean edgesCheck()
    {
	if ( edges == null )
	    {
		JOptionPane.showMessageDialog( null , 
					       "You must load the edges first." ,
					       "Error" , JOptionPane.ERROR_MESSAGE );		
		return false;
	    }
	return true;
    }
	
    private boolean vertexCheck()
    {
	if ( vertices == null )
	    {
		JOptionPane.showMessageDialog( null , 
					       "You must load the vertices first." ,
					       "Error" , JOptionPane.ERROR_MESSAGE );		
		return false;
	    }
	return true;
    }

}

//////////////////////////////////////////////////////////////////

class FileSuffixFilter extends javax.swing.filechooser.FileFilter
{
    // The suffix should have a '.' at the beggining since
    // that is implicit
    private String suffix;

    public FileSuffixFilter( String s )
    {
	suffix = s;
    }

    // Accept all directories and all files that match the provided
    // file suffix
    public boolean accept( File f )
    {

        if ( f.isDirectory() )
	    {
		return true;
	    }

        String extension = getExtension( f );
        if ( extension != null )
	    {
		if ( extension.equals( suffix ) )
		    {
			return true;
		    }
		else
		    {
			return false;
		    }
	    }
	else
	    {
		return false;
	    }
    }

    public void setSuffix( String s ) { suffix = s; }
    public String getSuffix() { return suffix; }

    //The description of this filter
    public String getDescription()
    {
        return "Only *" + suffix + " files.";
    }

    public String getExtension( File f )
    {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

	if (i > 0 &&  i < s.length() - 1)
	    {
		ext = s.substring(i+1).toLowerCase();
	    }

        return ext;
    }

}

//////////////////////////////////////////////////////////////////
