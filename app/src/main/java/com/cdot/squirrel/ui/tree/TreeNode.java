package com.cdot.squirrel.ui.tree;

import com.cdot.squirrel.hoard.HoardNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A TreeNode is the link between a HoarfdNode and the view used to render that node.
 * There is one TreeNode for each HoardNode.
 */
public class TreeNode {
    public static final String NODES_ID_SEPARATOR = ":";

    private int mId;
    private int mLastId = 0;
    public TreeNode mParent;
    private final List<TreeNode> mChildren;
    public TreeNodeView mTreeNodeView;
    public HoardNode mHoardNode;
    public boolean mExpanded;

    public TreeNode(HoardNode hnode) {
        mChildren = new ArrayList<>();
        mHoardNode = hnode;
    }

    public void addChild(TreeNode childNode) {
        childNode.mParent = this;
        childNode.mId = ++mLastId;
        mChildren.add(childNode);
    }

    public int deleteChild(TreeNode child) {
        for (int i = 0; i < mChildren.size(); i++) {
            if (child.mId == mChildren.get(i).mId) {
                mChildren.remove(i);
                return i;
            }
        }
        return -1;
    }

    public List<TreeNode> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /**
     * Find the TreeNode that contains the given HoardNode
     * @param n node to search for
     * @return the tree node found
     */
    public TreeNode findTreeNode(HoardNode n) {
        if (mHoardNode == n)
            return this;
        for (TreeNode tkid : getChildren()) {
            TreeNode found = tkid.findTreeNode(n);
            if (found != null)
                return found;
        }
        return null;
    }

    /**
     * Get a path built from a string of node id's. This is used for preserving tree state.
     * @return a string of node ids
     */
    public String getPath() {
        final StringBuilder path = new StringBuilder();
        TreeNode node = this;
        while (node.mParent != null) {
            path.append(node.mId);
            node = node.mParent;
            if (node.mParent != null) {
                path.append(NODES_ID_SEPARATOR);
            }
        }
        return path.toString();
    }

    public TreeNode getRoot() {
        TreeNode root = this;
        while (root.mParent != null) {
            root = root.mParent;
        }
        return root;
    }
}
