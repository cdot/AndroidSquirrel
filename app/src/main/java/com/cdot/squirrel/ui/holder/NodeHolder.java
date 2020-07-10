package com.cdot.squirrel.ui.holder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cdot.squirrel.hoard.Leaf;
import com.unnamed.b.atv.model.TreeNode;

import com.cdot.squirrel.hoard.HoardNode;
import com.cdot.squirrel.ui.R;

/**
 * View for the content (name, value) of a hoard node
 */
public class NodeHolder extends TreeNode.BaseNodeViewHolder<HoardNode> {
    private View mView;

    public NodeHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, HoardNode value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.layout_node_holder, null, false);

        TextView tvName = mView.findViewById(R.id.node_name);
        tvName.setText(value.getName());

        TextView tvValue = mView.findViewById(R.id.node_value);
        ImageButton btnOpenClose = mView.findViewById(R.id.open_close_icon);
        if (value instanceof Leaf) {
            tvValue.setText(((Leaf)value).getData());
            btnOpenClose.setVisibility(View.GONE);
        } else {
            tvValue.setVisibility(View.GONE);
            btnOpenClose.setPadding(20,0,10,0);
            btnOpenClose.setOnClickListener(view1 -> tView.toggleNode(node));
        }

        return mView;
    }

    @Override
    public NodeHolder toggle(boolean active) {
        ImageButton arrowView = mView.findViewById(R.id.open_close_icon);
        arrowView.setImageResource(active ? R.drawable.ic_folder_open : R.drawable.ic_folder_closed);
        return this;
    }
}
