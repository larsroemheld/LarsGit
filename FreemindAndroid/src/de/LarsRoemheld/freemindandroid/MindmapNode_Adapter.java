/**
 * 
 */
package de.LarsRoemheld.freemindandroid;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Lars
 *
 */
public class MindmapNode_Adapter extends ArrayAdapter<MindmapNode> {
	private final Context context;
	private final int rowLayoutId;
	private final int textViewId;
	private final int imViewId;

	private ArrayList<MindmapNode> nodes;


	public MindmapNode_Adapter(Activity context, int rowLayoutId, int textViewId, int imViewId, ArrayList<MindmapNode> nodes)
	{
		super(context, rowLayoutId, nodes);

		this.context = context;
		this.rowLayoutId = rowLayoutId;
		this.textViewId = textViewId;
		this.imViewId = imViewId;

		this.nodes = nodes;
	}

	@Override
	public View getView(int position, View concertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) this.context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = inflater.inflate(this.rowLayoutId, parent, false);

		TextView tView = (TextView) rowView.findViewById(this.textViewId);
		TextView cView = (TextView) rowView.findViewById(R.id.bubbles_listitem_count);
		ImageView iView = (ImageView) rowView.findViewById(this.imViewId);

		MindmapNode node = this.nodes.get(position);

		if (node.hasChildren()) {
			tView.setText(node.getText());
			cView.setText("(" + node.getChildren().size() + ")");
			
			iView.setImageResource(R.drawable.ic_arrow_right);
		} else {
			tView.setText(node.getText());
			// iView.setImageResource(0);
		}

		return rowView;
	}

}
