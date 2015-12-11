package com.esoft.archer.user.controller;

import java.security.AllPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.primefaces.model.CheckboxTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.user.model.Permission;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;

import freemarker.template.utility.StringUtil;


@Component
@Scope(ScopeType.VIEW)
public class PermissionList extends EntityQuery<Permission> {
	@Logger
	static Log log;
	@Resource
	private HibernateTemplate ht;
	
	//子结点是否默认展开
	private boolean expanded = false;
	//根结点
	private TreeNode root;
	//单选选中的结点
	private TreeNode selectedNode;
	//多选选中的结点
	private TreeNode[] selectedNodes;
	//某个角色的权限集合(从数据库表role_permission中查出)
	List<Permission> rolePermissionList;
	//选中的权限集合（界面选择）
	List<Permission> selectPermissionList;
	
	//某个菜单的权限集合(从数据库表menu_permission中查出)
	List<Permission> menuPermissionList;


	public PermissionList() {
		final String[] RESTRICTIONS = {"id like #{permissionList.example.id}",
				"name like #{permissionList.example.name}"
				};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}
	
	/**
	 * 根据权限集合permissionList，得到一颗树，并返回树的根结点
	 * */
	public TreeNode getRoot() {
		root = new CheckboxTreeNode(new Permission("id", "用户权限"), null);
		root.setExpanded(true);
		List<Permission> permissionList = new ArrayList<Permission>();
		addOrder("seqNum", DIR_ASC);
		permissionList = ht.find(getRenderedHql(), getParameterValues());
		if(!StringUtils.isEmpty(rolePermissionList)){
			if(rolePermissionList.size() > 0){
				for(int i = 0; i < rolePermissionList.size(); i++){
					for(int j = 0; j < permissionList.size(); j++){
						if(permissionList.get(j).getId().equals(rolePermissionList.get(i).getId())){
							permissionList.get(j).setIsSelected(true);
							break;
						}
					}  
				}
			}
		}
		if (permissionList != null && permissionList.size() > 0) {
			//得到所有的父结点
			List<Permission> pwnp = filtrateMenuWithNullParent(permissionList);
			for (Permission p : pwnp) {
				TreeNode newChild = createNewNode(p, root);
				//初始化子树
				initTreeNode(permissionList, p, newChild);
			}
			//把有父节点，但是不在查询结果中的permission
			for (Permission m : permissionList) {
				TreeNode newChild = createNewNode(m, root);
			}
		}
		return root;
	}

	
	/**
	 * 根据权限集合permissionList，得到一颗树，并返回树的根结点
	 * */
	public TreeNode getMenuRoot() {
		if (root != null) {
			return root;
		}
		root = new CheckboxTreeNode(new Permission("id", "用户权限"), null);
		root.setExpanded(true);
		List<Permission> permissionList = ht.find("from Permission p where p.mustSelected is false order by seqNum desc");
		
		if(!StringUtils.isEmpty(menuPermissionList)){
			if(menuPermissionList.size() > 0){
				//如果数据库中选中，则前端界面显示
				for(int i = 0; i < menuPermissionList.size(); i++){
					for(int j = 0; j < permissionList.size(); j++){
						if(permissionList.get(j).getId().equals(menuPermissionList.get(i).getId())){
							permissionList.get(j).setIsSelected(true);
							break;
						}
					}  
				}
			}
		}
		if (permissionList != null && permissionList.size() > 0) {
			//得到所有的父结点
			List<Permission> pwnp = filtrateMenuWithNullParent(permissionList);
			for (Permission p : pwnp) {
				TreeNode newChild = createNewNode(p, root);
				//初始化子树
				initTreeNode(permissionList, p, newChild);
			}
			//把有父节点，但是不在查询结果中的permission
			for (Permission m : permissionList) {
				TreeNode newChild = createNewNode(m, root);
			}
		}
		return root;
	}
	
	
	/**
	 * 筛选该节点的所有父节点（只要符合条件，在总列表中删除掉，提高性能。）
	 * @param menus
	 * @return
	 */
	private List<Permission> filtrateMenuWithNullParent(List<Permission> permissions) {
		List<Permission> pms = new ArrayList<Permission>();
		for (Iterator iterator = permissions.iterator(); iterator.hasNext();) {
			Permission permission = (Permission) iterator.next();
			if (permission.getParent() == null) {
				pms.add(permission);
				iterator.remove();
			}
		}
		return pms;
	}
	
	  
	/**
	 * 筛选该节点的所有孩子节点（只要符合条件，在总列表中删除掉，提高性能）
	 * @param menus
	 * @param parent
	 * @return
	 */
	private List<Permission> filtrateChildren(List<Permission> permissions, Permission parent) {
		List<Permission> rs = new ArrayList<Permission>();
		for (Iterator iterator = permissions.iterator(); iterator.hasNext();) {
			Permission permission = (Permission) iterator.next();
			if (permission.getParent() != null
					&& permission.getParent().getId().equals(parent.getId())) {
				rs.add(permission);
				iterator.remove();
			}
		}
		return rs;
	}
	
	
	private TreeNode createNewNode(Permission permission, TreeNode parentNode) {
		TreeNode newNode = new CheckboxTreeNode("permissionTree", permission, parentNode);
		if(permission.getIsSelected()){
			newNode.setSelected(true);
		}
		if(!StringUtils.isEmpty(permission.getMustSelected()) && permission.getMustSelected()){
			newNode.setSelected(true);
			newNode.setSelectable(false);
		}
		//如果默认展开，浏览器性能吃不消
		if (expanded) {
			 newNode.setExpanded(true);			
		}
		return newNode;
	}
	
	private void initTreeNode(List<Permission> permissionList, Permission parentPermission,
			TreeNode parentNode) {
		List<Permission> pcs = filtrateChildren(permissionList, parentPermission);
		for (Permission permission : pcs) {
			TreeNode newNode = createNewNode(permission, parentNode);
			initTreeNode(permissionList, permission, newNode);
		}

	}
	
	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	
	  
    public TreeNode[] getSelectedNodes() {  
        return selectedNodes;  
    }  
  
    public void setSelectedNodes(TreeNode[] selectedNodes) {  
        this.selectedNodes = selectedNodes;  
    }
    
    public List<Permission> getRolePermissionList() {
		return rolePermissionList;
	}
    
    public void setRolePermissionList(List<Permission> rolePermissionList) {
		this.rolePermissionList = rolePermissionList;
	}
    
	
	public List<Permission> getMenuPermissionList() {
		return menuPermissionList;
	}

	public void setMenuPermissionList(List<Permission> menuPermissionList) {
		this.menuPermissionList = menuPermissionList;
	}
	

	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}
    
    /**
     * 得到前台选中的permission
     * */
    public List<Permission> getSelectPermissionList(TreeNode[] nodes) {
    	if(nodes != null && nodes.length > 0) {
    		selectPermissionList = new ArrayList<Permission>();
            for(TreeNode node : nodes) {
            	if(!((Permission)node.getData()).getId().equals(((Permission)root.getData()).getId())){
            		selectPermissionList.add((Permission)node.getData());
            	}
            }
        }
		return selectPermissionList;
	}
    
    /**
     * 得到前端单选选中的permission
     * */
    public List<Permission> getSelectSinglePermission() {
    	selectPermissionList = new ArrayList<Permission>();
    	if(!StringUtils.isEmpty(selectedNode)){
    		selectPermissionList.add((Permission)selectedNode.getData());
    	}
		return selectPermissionList;
	}
    
    /**
     * 得到前台某个角色选中的permission。（子权限被选中，不管其它子权限选不选中，父权限也会被选中）
     * */
    public List<Permission> getRoleSelectPermissionList(TreeNode[] nodes) {
    	if(nodes != null && nodes.length > 0) {
    		selectPermissionList = new ArrayList<Permission>();
            for(TreeNode node : nodes) {
            	if(!((Permission)node.getData()).getId().equals(((Permission)root.getData()).getId())){
            		Permission permission = (Permission)node.getData();
            		//去掉自己的重复
        			boolean flag1 = true;
            		for(int i = 0; i < selectPermissionList.size(); i++){
            			if(selectPermissionList.get(i).getId().equals(permission.getId())){
            				flag1 = false;
            				break;
            			}
            		}
            		if(flag1){
            			selectPermissionList.add(permission);
            		}
            		//查找父结点，并把父结点加入选中的权限列表
            		while(permission.getParent() != null){
            			permission = permission.getParent();
            			//父权限是否已经添加到选中的权限列表，如果没有则添加，则添加这个父权限
            			boolean flag2 = true;
            			for(int i = 0; i < selectPermissionList.size(); i++){
            				if(selectPermissionList.get(i).getId().equals(permission.getId())){
            					flag2 = false;
            					break;
            				}
            			}
            			if(flag2){
            				selectPermissionList.add(permission);
            			}
            		}
            		
            	}
            }
        }
		return selectPermissionList;
	}
    
}
