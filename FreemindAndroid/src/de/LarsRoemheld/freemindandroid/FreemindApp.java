package de.LarsRoemheld.freemindandroid;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FreemindApp extends Activity implements OnClickListener, OnLongClickListener,
													OnItemClickListener, OnItemLongClickListener {
	private String filePath;
	private MindmapNode myMindmap = new MindmapNode(null);
	private MindmapNode_Adapter bubblesListAdapter;
	private volatile boolean changesMade = false;
	
	private Button titleButton;
	
	private String searchedFor = "";
	private int searchPos = 0;
	private ArrayList<MindmapNode> searchResults = new ArrayList<MindmapNode>();
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        // TODO create temporary files and enable creating new files? Currently all changes are lost onRotate!!!
        // -- cf. Writer app: maintain own folder of mm files (copies saved there) -> "share" from app.
        
        setContentView(R.layout.activity_bubbles);
        
    	titleButton = (Button) findViewById(R.id.titleButton);
		titleButton.setOnLongClickListener(this);

		ListView list = (ListView) findViewById(R.id.bubblesList);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
        
    	myMindmap = new MindmapNode(null);

        Intent intent = getIntent();
        
        if (intent != null && 
        	intent.getType() != null && 
        	intent.getType().indexOf("application/x-freemind") != -1)
        	{
        	// Open a file
        	 
            Uri fileUri = intent.getData();
            try {
            	filePath = fileUri.getPath();
            	FileReader file = new FileReader(filePath);

            	XmlPullParser xmlParser = Xml.newPullParser();
            	xmlParser.setInput(file);
            	
            	xmlParser.nextTag();
                xmlParser.require(XmlPullParser.START_TAG, null, "map");
                
                while (! (xmlParser.getName().equalsIgnoreCase("node") && xmlParser.getEventType() == XmlPullParser.START_TAG)) {xmlParser.nextTag(); }
                
                boolean fullyRead = myMindmap.readFromXml(xmlParser);
                
                if (!fullyRead) {
                	filePath = filePath + "_copy.mm";
                	
                	new AlertDialog.Builder(this) //
                	.setTitle(R.string.readFileIncompleteDialog) //
                	.setMessage(getString(R.string.readFileIncompleteDialogMsg) + "\n\n " + filePath) //
                	.show();
                }
                
                xmlParser.setInput(null);
                xmlParser = null;
                file.close();
            } catch (IOException e) {
            	Log.w("LR1", e.toString());    
            } catch (XmlPullParserException x) {
            	Log.w("LR1", x.toString());    
            };
        } 
        
        if (myMindmap.getText() == null) {
        	myMindmap.setText("Welcome to Lars Roemheld's FreemindApp");
        	
            myMindmap.addChild("Open any *.mm file through this app (using e.g. the downloads app)");
            myMindmap.addChild("Short press to navigate.");
            myMindmap.addChild("Long press to edit nodes.");
            myMindmap.addChild("Any empty node will automatically be deleted.");
            myMindmap.addChild("---");
            myMindmap.addChild("Add new nodes using \"+\" button.");
            myMindmap.addChild("---");
            myMindmap.addChild("<h1> html works </h1> <p> or at least for nodes <i> without children </i> </p>");
            
            MindmapNode x = myMindmap;
            for (int i = 1; i < 20; i++) {
            	x.addChild("son" + i);
            	x = x.addChild("father" + i);
            }
            
        }
        
        MindmapNode dNode = myMindmap;
        MindmapNode cNode;
        changesMade = false;
        
        if (savedInstanceState != null) {
        	try {
        	ArrayList<Integer> relPos = savedInstanceState.getIntegerArrayList("relPos");
        	
        	if (relPos != null) {
        		for (Integer ii : relPos) {
        			cNode = dNode.getChildren().get(ii);
        			dNode = cNode;
        		}
        	}
        	} catch (Exception e) {
        		Log.w("LR", "Exception when trying to restore InstanceState in onCreate!");
        		dNode = myMindmap;
        	}
        	
        	changesMade = savedInstanceState.getBoolean("changesMade", false);
        }
        
        repopulateBubbles(dNode);
        
        
        EditText eT = (EditText) findViewById(R.id.editText_searchText);
        eT.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    action_searchNext(findViewById(R.id.search_Button));
                    return true;
                }
                return false;
            }
        });
    }
	
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) { // onRestoreInstanceState is handled by onCreate!
        super.onSaveInstanceState(savedInstanceState);

        ArrayList<Integer> relPos = new ArrayList<Integer>();

    	MindmapNode dNode = (MindmapNode) titleButton.getTag(R.id.LarsRoemheld_key_node_THIS);
    	
    	while (dNode.getParent() != null) {
    		relPos.add(0, dNode.getParent().getChildren().indexOf(dNode));
    		dNode = dNode.getParent();
    	}
        
        savedInstanceState.putIntegerArrayList("relPos", relPos);
        savedInstanceState.putBoolean("changesMade", changesMade);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_main, menu);
        return true;
    }

    @Override
    protected void onStop() {
    	super.onStop();

    	if (changesMade && filePath != null && !filePath.isEmpty()) {
    		try{
    			BufferedWriter file = new BufferedWriter(new FileWriter(filePath)); 

    			XmlSerializer serializer = Xml.newSerializer();
    			serializer.setOutput(file);

    			// serializer.startDocument(null, null);

    			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

    			serializer.startTag(null, "map"); serializer.attribute(null, "version", "0.7.1");

    			serializer.comment("To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net");
    			serializer.comment(getString(R.string.fileCreatedBy));

    			myMindmap.writeToXml(serializer);

    			serializer.endTag(null, "map");
    			serializer.endDocument();
    			serializer.flush();
    			file.flush();
    		} catch(Exception e)
    		{
    			Log.e("LR", e.toString());
    		}
    	} 
    }

    
	private void repopulateBubbles(MindmapNode node) {
		
		LinearLayout titleCrumbs_lLayout = (LinearLayout) findViewById(R.id.title_crumbs);
    	
		titleButton.setTag(R.id.LarsRoemheld_key_node_NAV, node.getParent());
		titleButton.setTag(R.id.LarsRoemheld_key_node_THIS, node);

		titleCrumbs_lLayout.removeViews(0, titleCrumbs_lLayout.getChildCount() - 1);
		
		titleButton.setText(node.getText());

		// Create Breadcrumbs
		MindmapNode x = node;
		while (x.getParent() != null) {
			x = x.getParent();
			
			Button crumbButton = new Button(titleCrumbs_lLayout.getContext());
			
			
			
			crumbButton.setLayoutParams(
					new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 
							LinearLayout.LayoutParams.MATCH_PARENT));
			crumbButton.setText(x.getText());
			crumbButton.setTag(R.id.LarsRoemheld_key_node_NAV, x);
			crumbButton.setTag(R.id.LarsRoemheld_key_node_THIS, x);

			crumbButton.setOnClickListener(this);
			crumbButton.setOnLongClickListener(this);
			
			crumbButton.setAlpha(0.5f);
			
			titleCrumbs_lLayout.addView(crumbButton, 0);
		}
		x = null;
		
		final HorizontalScrollView scroll = (HorizontalScrollView) findViewById(R.id.scrollViewTitle);
		scroll.post(new Runnable() {            
		    public void run() {
		       scroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT);              
		    }
		});
		
		
		// TODO wahrscheinlich muss einfach ein custom adapter geschrieben werden für ein bubbles_listitem, das auch einen pfeil
		// (icon) beinhaltet, wenn children da sind -- dort dann auch ggf. verlinkung von tags?
		// Dann implementierung von click/longclick!
    	bubblesListAdapter = new MindmapNode_Adapter(this, R.layout.bubbles_listitem, 
    			R.id.bubbles_listitem_text, R.id.bubbles_listitem_iconright, node.getChildren());
    	
        ListView list = (ListView) findViewById(R.id.bubblesList);
        list.setAdapter(bubblesListAdapter);
        
		// Avoid resizing problems
		titleButton.forceLayout();
	}
	
    
    @Override
    public void onBackPressed() {
    	try {
    		MindmapNode m = (MindmapNode) titleButton.getTag(R.id.LarsRoemheld_key_node_THIS);
	    	
	    	if (m.getParent() != null) {
	    		repopulateBubbles(m.getParent());
	    	} else {
		    	super.onBackPressed(); 
	    	}
    	} catch (Exception e) {
        	super.onBackPressed();
    	}
    }
	
	public void onClick(View view) { // Used for title button and breadcrumbs
		if (view.getClass() == Button.class) {
			Object t = ((Button) view).getTag(R.id.LarsRoemheld_key_node_THIS);
			Object n = ((Button) view).getTag(R.id.LarsRoemheld_key_node_NAV);
			if (t != null && 
				n != null)
				if (t.getClass() == MindmapNode.class &&
					n.getClass() == MindmapNode.class) 
				{
					repopulateBubbles((MindmapNode) n);
				}
		}
	}
	
	public boolean onLongClick(View view) { // Used for title button and breadcrumbs
		if (view.getClass() == Button.class) {
			Object b = ((Button) view).getTag(R.id.LarsRoemheld_key_node_THIS);
			if (b != null && b.getClass() == MindmapNode.class) 
			{
		    	editNodeDialog((MindmapNode) b, false);

		    	return true;
			}
		}
		
		return false;
	}
	
	public void onItemClick(AdapterView parent, View view, int position, long id) {
    	MindmapNode t = (MindmapNode) titleButton.getTag(R.id.LarsRoemheld_key_node_THIS);
		
    	if (t != null) {
    		repopulateBubbles(t.getChildren().get(position));
    	}
	}
	
	public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
    	MindmapNode t = (MindmapNode) titleButton.getTag(R.id.LarsRoemheld_key_node_THIS);
		
    	if (t != null) {
    		editNodeDialog(t.getChildren().get(position), false);
    	}
    	
		return true;
	}
	
	public void action_searchNext(View view) {
    	EditText input = (EditText) findViewById(R.id.editText_searchText);
    	
    	if (searchedFor.equals(input.getText().toString()) && !searchResults.isEmpty()) {
    		searchPos++;
    		if (searchPos >= searchResults.size())  { 
        		searchPos = 0;
        		
        		Toast t = Toast.makeText(getApplicationContext(), R.string.searchStartingOver, Toast.LENGTH_SHORT);
        		t.setGravity(Gravity.TOP|Gravity.CENTER, 0, (int) Math.ceil(50 * getApplicationContext().getResources().getDisplayMetrics().density));
        		t.show();
        	}
    		
        	repopulateBubbles(searchResults.get(searchPos));
    	} else {
    		searchResults = myMindmap.searchInChildren(input.getText().toString());
    		searchPos = 0;
    		
        	if (searchResults.isEmpty()) {
        		Toast t = Toast.makeText(getApplicationContext(), R.string.searchUnsuccessful, Toast.LENGTH_SHORT);
        		t.setGravity(Gravity.TOP|Gravity.CENTER, 0, (int) Math.ceil(50 * getApplicationContext().getResources().getDisplayMetrics().density));
        		t.show();
        	} else
        		repopulateBubbles(searchResults.get(0));
    	}
    	
    	searchedFor = input.getText().toString();
	}
    
    public boolean menu_searchNodes(MenuItem it) {
    	ViewGroup a = (ViewGroup) findViewById(R.id.container_search);

		EditText input = (EditText) findViewById(R.id.editText_searchText);

    	if (a.getVisibility() == ViewGroup.VISIBLE) {
    		a.setVisibility(ViewGroup.GONE);

    		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    		if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    	}
    	else {
    		a.setVisibility(ViewGroup.VISIBLE);

        	input.requestFocus();

        	InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        	if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    	}
    	
    	return true;
    }
    
    public boolean menu_addNode(MenuItem it) {
    	MindmapNode t = (MindmapNode) titleButton.getTag(R.id.LarsRoemheld_key_node_THIS);

    	editNodeDialog(t.addChild(""), true);

    	return true;
    }

    
    private void editNodeDialog(MindmapNode node, boolean newNode) {
    	final MindmapNode dialogNode = node;

    	final EditText input = new EditText(this);
    	input.setText(node.getText());
    	input.setSelection(node.getText().length());
    	
    	String okText = "";
    	if (newNode) { okText = getString(R.string.editNodeDialog_new); }
    	else { okText = getString(R.string.editNodeDialog_edit); }
    	
    	new AlertDialog.Builder(this) //
        	.setTitle(R.string.editNodeDialog) //
        	.setView(input) //
        	.setPositiveButton(okText, new DialogInterface.OnClickListener() { //
        		public void onClick(DialogInterface dialog, int whichButton) { //
    				MindmapNode p = dialogNode.getParent();
        			
    				if (input.getText().length() == 0) {
    					if (p != null) {
        					dialogNode.delete();
            				repopulateBubbles(p);
            				changesMade = true;
    					}
    				} else {
    					dialogNode.setText(input.getText().toString());
    					repopulateBubbles((MindmapNode) titleButton.getTag(R.id.LarsRoemheld_key_node_THIS));
        				changesMade = true;
    				}
        		}//
        	})//
        	.setNegativeButton(R.string.editNodeDialog_cancel, new DialogInterface.OnClickListener() {//
        		public void onClick(DialogInterface dialog, int whichButton) {
        			if (dialogNode.getText().length() == 0) {
        				MindmapNode p = dialogNode.getParent();
        				
        				if (p != null) {
            				dialogNode.delete();
                	    	repopulateBubbles(p);
        				}
        			}
        		} //
        	})//
        	.show();
    }
    
    
}
