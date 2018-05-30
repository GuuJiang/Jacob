package com.guujiang.jacob.samples;

import com.guujiang.jacob.annotation.Generator;
import com.guujiang.jacob.annotation.GeneratorMethod;
import com.guujiang.jacob.annotation.OverRangePolicy;

import static com.guujiang.jacob.Stub.yield;

class Node {
	int val;
	Node left;
	Node right;

	public Node(int val) {
		this.val = val;
	}

	public Node setVal(int val) {
		this.val = val;
		return this;
	}

	public Node setLeft(Node left) {
		this.left = left;
		return this;
	}

	public Node setRight(Node right) {
		this.right = right;
		return this;
	}

	@Override
	public String toString() {
		return String.valueOf(val);
	}
}

@Generator
public class BinaryTree {

	public Node root;
	
	/**
	 * build the following tree
	 * <pre>
	 *            1
	 *          /   \
	 *         2     6
	 *        / \      \
	 *       3   4      7
	 *            \    /
	 *             5  8
	 * </pre>
	 */
	public void buildTestTree() {
		root = new Node(1)
				.setLeft(new Node(2)
						.setLeft(new Node(3))
						.setRight(new Node(4)
								.setRight(new Node(5))))
				.setRight(new Node(6)
						.setRight(new Node(7)
								.setLeft(new Node(8))));
	}

	@GeneratorMethod(overIterate = OverRangePolicy.PreFetch)
	public Iterable<Node> inOrderTraversal(Node root) {
		if (root.left != null) {
			for (Node n : inOrderTraversal(root.left)) {
				yield(n);
			}
		}
		yield(root);
		if (root.right != null) {
			for (Node n : inOrderTraversal(root.right)) {
				yield(n);
			}
		}
		return null;
	}

	@GeneratorMethod(overIterate = OverRangePolicy.PreFetch)
	public Iterable<Node> preOrderTraversal(Node root) {
		yield(root);
		if (root.left != null) {
			for (Node n : inOrderTraversal(root.left)) {
				yield(n);
			}
		}
		if (root.right != null) {
			for (Node n : inOrderTraversal(root.right)) {
				yield(n);
			}
		}
		return null;
	}
	
	@GeneratorMethod(overIterate = OverRangePolicy.PreFetch)
	public Iterable<Node> postOrderTraversal(Node root) {
		if (root.left != null) {
			for (Node n : inOrderTraversal(root.left)) {
				yield(n);
			}
		}
		if (root.right != null) {
			for (Node n : inOrderTraversal(root.right)) {
				yield(n);
			}
		}
		yield(root);
		return null;
	}
	
	public static void main(String[] args) {
		BinaryTree tree = new BinaryTree();
		tree.buildTestTree();
		
		tree.inOrderTraversal(tree.root).forEach(System.out::print);
		System.out.println();
		
		tree.preOrderTraversal(tree.root).forEach(System.out::print);
		System.out.println();
		
		tree.postOrderTraversal(tree.root).forEach(System.out::print);
		System.out.println();
	}
}