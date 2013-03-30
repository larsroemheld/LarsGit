package de.LarsRoemheld.freemindandroid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class FreemindApp extends FragmentActivity implements OnClickListener, OnLongClickListener  {
	private String filePath;
	private MindmapNode myMindmap = new MindmapNode(null);
	private volatile boolean changesMade = false;
	
	//  public volatile MindmapNode dialogNode;
	private void repopulateBubbles(MindmapNode node) {
		
    	Button titleButton = (Button) findViewById(R.id.titleButton);
		LinearLayout lLayout = (LinearLayout) findViewById(R.id.bubbles_Layout);
		LinearLayout titleCrumbs_lLayout = (LinearLayout) findViewById(R.id.title_crumbs);
    	
		titleButton.setTag(R.id.LarsRoemheld_key_node_NAV, node.getParent());
		titleButton.setTag(R.id.LarsRoemheld_key_node_THIS, node);
		titleButton.setOnLongClickListener(this);

		
		lLayout.removeAllViews();
		titleCrumbs_lLayout.removeViews(0, titleCrumbs_lLayout.getChildCount() - 1);

		if (node.hasChildren()) {
			titleButton.setText(node.getText());

			// Create Breadcrumbs
			
			MindmapNode x = node;
			while (x.getParent() != null) {
				x = x.getParent();
				
				Button crumbButton = new Button(titleCrumbs_lLayout.getContext());
				
				crumbButton.setLayoutParams(
						new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 
								titleButton.getHeight()));
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
			
			

			// and list children
			for (MindmapNode b : node.getChildren()) {
				Button btn = new Button(lLayout.getContext());

				btn.setLayoutParams(
						new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT));

				if (!b.hasChildren()) btn.setBackgroundColor(10000);
				btn.setText(b.getText());
				btn.setTag(R.id.LarsRoemheld_key_node_NAV, b);
				btn.setTag(R.id.LarsRoemheld_key_node_THIS, b);

				btn.setOnClickListener(this);
				btn.setOnLongClickListener(this);

				lLayout.addView(btn);
			}
		} else {
			titleButton.setText(getString(R.string.bubble_back));
			
			WebView web = new WebView(lLayout.getContext());
			
			web.setLayoutParams(
					new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
							LinearLayout.LayoutParams.MATCH_PARENT, 1f));
			web.loadData(node.getText(), "text/html", null);

			lLayout.addView(web);
		}

		// Avoid resizing problems
		titleButton.forceLayout();
	}
	
	
	public void onClick(View view) {
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

	public boolean onLongClick(View view) {
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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_bubbles);

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
            	if (Math.random() < 0.5f) {
            		x = x.addChild("father" + i);
            	} else { x.addChild("son" + i); }
            }
            
        }

        repopulateBubbles(myMindmap);
        changesMade = false;
    }
    
    @Override
    public void onBackPressed() {
    	try {
    		Button titleButton = (Button) findViewById(R.id.titleButton);
	    	Object t = titleButton.getTag(R.id.LarsRoemheld_key_node_THIS);
	    	
	    	MindmapNode m = (MindmapNode) t;
	    		
	    	if (m.getParent() != null) {
	    		repopulateBubbles(m.getParent());
	    	} else {
		    	super.onBackPressed(); 
	    	}
    	} catch (Exception e) {
        	super.onBackPressed();
    	}
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
    
    private String editNodeDialog(MindmapNode node, boolean newNode) {
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
    					repopulateBubbles((MindmapNode) findViewById(R.id.titleButton).getTag(R.id.LarsRoemheld_key_node_THIS));
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

    	return "";
    }
    
    public boolean menu_addNode(MenuItem it) {
    	Button titleButton = (Button) findViewById(R.id.titleButton);
    	MindmapNode t = (MindmapNode) titleButton.getTag(R.id.LarsRoemheld_key_node_THIS);

    	editNodeDialog(t.addChild(""), true);

    	return true;
    }
    
}
