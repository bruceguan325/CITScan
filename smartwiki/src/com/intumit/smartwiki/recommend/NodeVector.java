package com.intumit.smartwiki.recommend;

import java.util.Vector;


public class NodeVector extends Vector{

	public NodeVector()
	{
		super();
	}

	//重複的不加入
	@Override
	public void addElement(Object element)
	{
		Node node = (Node)element;
		boolean exist = false;

		for(int i = 0 ; i < this.size(); i++)
		{
			Node n = (Node)this.get(i);
			if(node.id == n.id)
			{
				exist = true;
				break;
			}
		}
		if(!exist)
			super.addElement(element);
	}

	@Override
	public void add(int index, Object element)
	{
		Node node = (Node)element;
		boolean exist = false;

		if(this.size() > index)
		{
			Node n = (Node)this.get(index);
			if(node.id == n.id)
				exist = true;
		}
		if(!exist)
			super.add(index, element);
	}

}
